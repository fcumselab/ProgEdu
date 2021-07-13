package fcu.selab.progedu.service;

import fcu.selab.progedu.config.CourseConfig;
import fcu.selab.progedu.config.GitlabConfig;
import fcu.selab.progedu.config.JenkinsConfig;
import fcu.selab.progedu.conn.GitlabService;
import fcu.selab.progedu.conn.JenkinsService;
import fcu.selab.progedu.conn.TomcatService;
import fcu.selab.progedu.data.*;
import fcu.selab.progedu.db.*;
import fcu.selab.progedu.exception.LoadConfigFailureException;
import fcu.selab.progedu.jenkinsconfig.JenkinsProjectConfig;
import fcu.selab.progedu.jenkinsconfig.JenkinsProjectConfigFactory;
import fcu.selab.progedu.jenkinsconfig.WebPipelineConfig;
import fcu.selab.progedu.utils.JavaIoUtile;
import fcu.selab.progedu.utils.ZipHandler;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.gitlab.api.models.GitlabProject;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.jsoup.nodes.Element;
import org.jsoup.nodes.Document;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.io.InputStream;
import java.util.List;
import java.util.TimeZone;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value ="/assignment")
public class AssignmentService {

  private GitlabService gitlabService = GitlabService.getInstance();
  private TomcatService tomcatService = TomcatService.getInstance(); // Todo 命名有問題 應該是server 環境服務
  private JenkinsConfig jenkinsData = JenkinsConfig.getInstance();
  private GitlabConfig gitlabData = GitlabConfig.getInstance();
  private CourseConfig courseConfig = CourseConfig.getInstance();
  private AssignmentDbManager dbManager = AssignmentDbManager.getInstance();
  private UserService userService = UserService.getInstance();
  private UserDbManager userDbManager = UserDbManager.getInstance();
  private AssignmentUserDbManager auDbManager = AssignmentUserDbManager.getInstance();
  private ReviewSettingDbManager rsDbManager = ReviewSettingDbManager.getInstance();
  private ReviewSettingMetricsDbManager rsmDbManager = ReviewSettingMetricsDbManager.getInstance();
  private PairMatchingDbManager pmDbManager = PairMatchingDbManager.getInstance();
  private ReviewStatusDbManager reviewStatusDbManager = ReviewStatusDbManager.getInstance();


  private final String tempDir = System.getProperty("java.io.tmpdir");
  private final String uploadDir = tempDir + "/uploads/";
  private final String testDir = tempDir + "/tests/";


  private String gitlabRootUsername;
  private ZipHandler zipHandler;
  private String mailUsername;
  private String mailPassword;

  public AssignmentService() {
    try {
      zipHandler = new ZipHandler();
      mailUsername = jenkinsData.getMailUser();
      mailPassword = jenkinsData.getMailPassword();
      gitlabRootUsername = gitlabData.getGitlabRootUsername();
    } catch (LoadConfigFailureException e) {
      e.printStackTrace();
    }
  }


  @PostMapping("/create")
  public ResponseEntity<Object> createAssignment( // 把readme 的圖片處理拿掉 因為太複雜了
          @RequestParam("assignmentName") String assignmentName,
          @RequestParam("releaseTime") Date releaseTime, @RequestParam("deadline") Date deadline,
          @RequestParam("readMe") String readMe, @RequestParam("fileRadio") String assignmentType,
          @RequestParam("file") MultipartFile file) {


    // 1. Create root project and get project id and url
    gitlabService.createRootProject(assignmentName);

    // 2. Clone the project
    final String cloneDirectoryPath = gitlabService.cloneProject(gitlabRootUsername,
            assignmentName);

    // 3. Store Zip File to uploads folder if file is not empty
    String filePath;
    try {
      filePath = tomcatService.storeFileToUploadsFolder(file.getInputStream(), file.getName());
    } catch (Exception e) {
      e.printStackTrace();
      return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    // 4. Unzip the uploaded file to cloneDirectoryPath
    zipHandler.unzipFile(filePath, cloneDirectoryPath);

    // 5. Add .gitkeep if folder is empty.
    JavaIoUtile.addFile2EmptyFolder(new File(cloneDirectoryPath), ".gitkeep");

    if (!readMe.equals("<br>") || !"".equals(readMe) || !readMe.isEmpty()) {
      // Add readme to folder
      JavaIoUtile.createUtf8FileFromString(readMe, new File(cloneDirectoryPath, "README.md"));
    }

    // 8. git push
    gitlabService.pushProject(cloneDirectoryPath);

    // 9. import project information to database
    ProjectTypeEnum projectTypeEnum = ProjectTypeEnum.getProjectTypeEnum(assignmentType);
    addProject(assignmentName, releaseTime, deadline, readMe, projectTypeEnum);


    List<User> users = userService.getStudents();
    for (User user : users) {
      createAssignmentSettingsV2(user.getUsername(), assignmentName);
    }

    // 10. remove project file
    JavaIoUtile.deleteDirectory(new File(uploadDir));
    return new ResponseEntity<Object>(HttpStatus.OK);
  }

  @GetMapping("getAllAssignments")
  public ResponseEntity<Object> getAllAssignments() {
    List<Assignment> assignments = dbManager.getAllAssignment();
    JSONObject ob = new JSONObject();
    ob.put("allAssignments", assignments);
    return new ResponseEntity<Object>(ob, HttpStatus.OK);
  }

  @PostMapping("peerReview/create")
  public ResponseEntity<Object> createPeerReview(
          @RequestParam("assignmentName") String assignmentName,
          @RequestParam("releaseTime") Date releaseTime,
          @RequestParam("deadline") Date deadline,
          @RequestParam("readMe") String readMe,
          @RequestParam("fileRadio") String assignmentType,
          @RequestParam("file") MultipartFile file,
          @RequestParam("amount") int amount,
          @RequestParam("reviewStartTime") Date reviewStartTime,
          @RequestParam("reviewEndTime") Date reviewEndTime,
          @RequestParam("metrics") String metrics) {

    try {

      // 1. create assignment
      createAssignment(assignmentName,
              releaseTime, deadline, readMe, assignmentType, file);

      // 2. create peer review setting
      int assignmentId = dbManager.getAssignmentIdByName(assignmentName);
      rsDbManager.insertReviewSetting(assignmentId, amount, reviewStartTime, reviewEndTime);

      // 3. set review metrics for specific peer review
      int reviewSettingId = rsDbManager.getReviewSettingIdByAid(assignmentId);
      int[] array = arrayStringToIntArray(metrics);
      for (int item: array) {
        rsmDbManager.insertReviewSettingMetrics(reviewSettingId, item);
      }

      // 4. set random reviewer and review status for each assignment_user
      randomPairMatching(amount, assignmentName);

      return new ResponseEntity<Object>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<Object>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  @GetMapping("peerReview/allAssignment")
  public ResponseEntity<Object> getAllReviewAssignment() {

    try {
      List<Assignment> assignmentList = dbManager.getAllReviewAssignment();
      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
      Date current = new Date();
      for (Assignment assignment : assignmentList) {
        if (current.compareTo(assignment.getReleaseTime()) >= 0) {
          updatePairMatchingStatusByAid(assignment.getId());
        }
      }
      JSONObject result = new JSONObject();
      List<JSONObject> array = new ArrayList<>();
      for (Assignment assignment : assignmentList) {
        JSONObject ob = new JSONObject();
        ReviewSetting reviewSetting = rsDbManager.getReviewSetting(assignment.getId());
        ob.put("id", assignment.getId());
        ob.put("name", assignment.getName());
        ob.put("createTime", assignment.getCreateTime());
        ob.put("deadline", assignment.getDeadline());
        ob.put("releaseTime", assignment.getReleaseTime());
        ob.put("display", assignment.isDisplay());
        ob.put("description", assignment.getDescription());
        ob.put("reviewReleaseTime", reviewSetting.getReleaseTime());
        ob.put("reviewDeadline", reviewSetting.getDeadline());
        array.add(ob);
      }
      result.put("allReviewAssignments", array);

      return new ResponseEntity<Object>(result, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<Object>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  public void updatePairMatchingStatusByAid(int aid) throws SQLException {
    List<AssignmentUser> assignmentUserList = auDbManager.getAssignmentUserListByAid(aid);

    for (AssignmentUser assignmentUser : assignmentUserList) {

      if (!pmDbManager.checkStatusUpdated(assignmentUser.getId())) {
        List<PairMatching> pmList = pmDbManager.getPairMatchingByAuId(assignmentUser.getId());

        for (PairMatching pairMatching: pmList) {

          if (pairMatching.getReviewStatusEnum().equals(ReviewStatusEnum.INIT)) {
            int status = reviewStatusDbManager
                    .getReviewStatusIdByStatus(ReviewStatusEnum.UNCOMPLETED.getTypeName());
            pmDbManager.updatePairMatchingById(status, pairMatching.getId());
          }
        }
      }
    }
  }


  @GetMapping("autoAssessment/allAssignment")
  public ResponseEntity<Object> getAllAutoAssignment() {

    try {
      List<Assignment> assignmentList = dbManager.getAutoAssessment();
      JSONObject ob = new JSONObject();
      ob.put("allAutoAssessment", assignmentList);

      return new ResponseEntity<Object>(ob, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<Object>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("getAssignment")
  public ResponseEntity<Object> getAssignment(@RequestParam ("assignmentName") String assignmentName) {
    Assignment assignment = dbManager.getAssignmentByName(assignmentName);
    JSONObject ob = new JSONObject();
    ob.put("description", assignment.getDescription());
    ob.put("deadline", assignment.getDeadline());
    ob.put("type", assignment.getType());
    return new ResponseEntity<Object>(ob, HttpStatus.OK);
  }


  public ArrayList<String> findAllDescriptionImagePaths(String readme) {
    ArrayList<String> paths = new ArrayList<>();
    Document doc = Jsoup.parse(readme);
    Elements srcAttributes = doc.select("img[src]");
    for (Element src : srcAttributes) {
      paths.add(src.attr("src"));
    }
    return paths;
  }

  public void addProject(String name, Date releaseTime, Date deadline, String readMe,
                         ProjectTypeEnum projectType) {
    Assignment assignment = new Assignment();
    Date date = tomcatService.getCurrentTime();
    assignment.setName(name);
    assignment.setCreateTime(date);
    assignment.setReleaseTime(releaseTime);
    assignment.setDeadline(deadline);
    assignment.setDescription(readMe);
    assignment.setType(projectType);

    dbManager.addAssignment(assignment);
  }

  private void createAssignmentSettingsV2(String username, String assignmentName) {

    addAuid(username, assignmentName);
    //Todo 以上 addAuid 要改, 因為之後沒有 assignment

    GitlabProject gitlabProject = gitlabService.createPrivateProject(username,
            assignmentName, "root"); // Todo assignment 要改

    try {
      String jobName = username + "_" + assignmentName;

      gitlabService.setGitlabWebhook(gitlabProject, jobName);

      GitlabConfig gitlabConfig = GitlabConfig.getInstance();
      String projectUrl = gitlabConfig.getGitlabHostUrl() + "/" + username + "/" + assignmentName
              + ".git";

      CourseConfig courseConfig = CourseConfig.getInstance();
      String progEduApiUrl = courseConfig.getTomcatServerIp() + courseConfig.getBaseuri()
              + "/webapi";
      String updateDbUrl = courseConfig.getTomcatServerIp() + "/publicApi/update/commits";

      ProjectTypeEnum assignmentTypeEnum = dbManager.getAssignmentType(assignmentName);

      JenkinsProjectConfig jenkinsProjectConfig;
      if ( assignmentTypeEnum.equals(ProjectTypeEnum.WEB) ) {
        jenkinsProjectConfig = new WebPipelineConfig(projectUrl, updateDbUrl,
                username, assignmentName,
                courseConfig.getTomcatServerIp() + "/publicApi/commits/screenshot/updateURL");
      } else {
        jenkinsProjectConfig = JenkinsProjectConfigFactory
                .getJenkinsProjectConfig(assignmentTypeEnum.getTypeName(), projectUrl, updateDbUrl,
                        username, assignmentName);
      }

      JenkinsService jenkinsService = JenkinsService.getInstance();

      jenkinsService.createJobV2(jobName, jenkinsProjectConfig.getXmlConfig());
      jenkinsService.buildJob(jobName);

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public int[] arrayStringToIntArray(String metrics) {
    String[] items = metrics.split(",");
    int[] array = new int[items.length];

    for (int i = 0; i < items.length; i++) {
      array[i] = Integer.parseInt(items[i].trim());
    }

    return array;
  }

  public void randomPairMatching(int amount, String assignmentName) throws SQLException {
    int aid = dbManager.getAssignmentIdByName(assignmentName);
    List<User> userList = userService.getStudents();
    List<AssignmentUser> assignmentUserList = auDbManager.getAssignmentUserListByAid(aid);
    int randomNumber;

    while (true) {
      randomNumber = (int) (Math.random() * userList.size());
      if (assignmentUserList.get(randomNumber).getUid() != userList.get(0).getId()) {
        break;
      }
    }

    for (int count = 0; count < amount; count++) {
      int userSize = userList.size();
      List<PairMatching> insertPairMatchingList = new ArrayList<>();
      for (int order = 0; order < userSize; order++) {
        int totalCount = randomNumber + order + count;
        int mod = totalCount % userSize;

        if (assignmentUserList.get(mod).getUid() == userList.get(order).getId()) {
          randomNumber++;
          totalCount = randomNumber + order + count;
          mod = totalCount % userSize;
        }

        PairMatching pairMatching = new PairMatching();
        pairMatching.setAuId(assignmentUserList.get(mod).getId());
        pairMatching.setReviewId(userList.get(order).getId());
        pairMatching.setReviewStatusEnum(ReviewStatusEnum.INIT);

        insertPairMatchingList.add(pairMatching);
      }

      pmDbManager.insertPairMatchingList(insertPairMatchingList);
    }
  }

  public void addAuid(String username, String assignmentName) {
    int aid = dbManager.getAssignmentIdByName(assignmentName);
    int uid = userDbManager.getUserIdByUsername(username);

    auDbManager.addAssignmentUser(aid, uid);
  }



}