package fcu.selab.progedu.service;

import java.io.File;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import fcu.selab.progedu.data.AssignmentUser;
import fcu.selab.progedu.data.PairMatching;
import fcu.selab.progedu.data.ReviewSetting;
import fcu.selab.progedu.db.PairMatchingDbManager;
import fcu.selab.progedu.db.ReviewRecordDbManager;
import fcu.selab.progedu.db.ReviewSettingDbManager;
import fcu.selab.progedu.db.ReviewSettingMetricsDbManager;
import fcu.selab.progedu.db.ReviewStatusDbManager;
import fcu.selab.progedu.jenkinsconfig.JenkinsProjectConfig;
import fcu.selab.progedu.jenkinsconfig.JenkinsProjectConfigFactory;
import fcu.selab.progedu.jenkinsconfig.MavenPipelineConfig;
import fcu.selab.progedu.jenkinsconfig.WebPipelineConfig;
import fcu.selab.progedu.setting.WebAssignmentSetting;
import fcu.selab.progedu.utils.JavaIoUtile;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fcu.selab.progedu.config.CourseConfig;
import fcu.selab.progedu.config.GitlabConfig;
import fcu.selab.progedu.config.JenkinsConfig;
import fcu.selab.progedu.conn.GitlabService;
import fcu.selab.progedu.conn.JenkinsService;
import fcu.selab.progedu.conn.TomcatService;
import fcu.selab.progedu.data.Assignment;
import fcu.selab.progedu.data.User;
import fcu.selab.progedu.db.AssignmentDbManager;
import fcu.selab.progedu.db.AssignmentUserDbManager;
import fcu.selab.progedu.db.AssignmentAssessmentDbManager;
import fcu.selab.progedu.db.CommitRecordDbManager;
import fcu.selab.progedu.db.CommitStatusDbManager;
import fcu.selab.progedu.db.ScreenshotRecordDbManager;
import fcu.selab.progedu.db.UserDbManager;
import fcu.selab.progedu.exception.LoadConfigFailureException;
import fcu.selab.progedu.data.ProjectTypeEnum;
import fcu.selab.progedu.utils.ExceptionUtil;
import fcu.selab.progedu.utils.Linux;
import fcu.selab.progedu.utils.ZipHandler;
import fcu.selab.progedu.setting.MavenAssignmentSetting;
import fcu.selab.progedu.service.AssignmentWithOrderCreator;

@Path("assignment/")
public class AssignmentService {
  private static AssignmentService instance = new AssignmentService();

  public static AssignmentService getInstance() {
    return instance;
  }

  private GitlabService gitlabService = GitlabService.getInstance();
  private GitlabUser root = gitlabService.getRoot();
  private ZipHandler zipHandler;
  private JenkinsService jenkins = JenkinsService.getInstance();
  private TomcatService tomcatService = TomcatService.getInstance();
  private GitlabConfig gitlabData = GitlabConfig.getInstance();
  private JenkinsConfig jenkinsData = JenkinsConfig.getInstance();
  private CourseConfig courseConfig = CourseConfig.getInstance();
  private UserService userService = UserService.getInstance();
  private String mailUsername;
  private String mailPassword;
  private String gitlabRootUsername;
  private AssignmentDbManager dbManager = AssignmentDbManager.getInstance();
  private AssignmentAssessmentDbManager aaDbManager = AssignmentAssessmentDbManager.getInstance();
  private AssignmentUserDbManager auDbManager = AssignmentUserDbManager.getInstance();
  private UserDbManager userDbManager = UserDbManager.getInstance();
  private CommitStatusDbManager csDbManager = CommitStatusDbManager.getInstance();
  private CommitRecordDbManager crDbManager = CommitRecordDbManager.getInstance();
  private ScreenshotRecordDbManager srDbManager = ScreenshotRecordDbManager.getInstance();
  private ReviewSettingDbManager rsDbManager = ReviewSettingDbManager.getInstance();
  private PairMatchingDbManager pmDbManager = PairMatchingDbManager.getInstance();
  private ReviewSettingMetricsDbManager rsmDbManager = ReviewSettingMetricsDbManager.getInstance();
  private ReviewRecordDbManager rrDbManager = ReviewRecordDbManager.getInstance();
  private ReviewStatusDbManager reviewStatusDbManager = ReviewStatusDbManager.getInstance();
  private final String tempDir = System.getProperty("java.io.tmpdir");
  private final String uploadDir = tempDir + "/uploads/";
  private final String testDir = tempDir + "/tests/";
  private final String assignmentSettingDir = tempDir + "/assignmentSetting/";
  private final String mavenPomXmlSettingDir = tempDir + "/mavenPomXmlSetting/";
  private final String webConfigXmlSettingDir = tempDir + "/webConfigXmlSetting/";

  // System.getProperty("catalina.base") is /usr/local/tomcat, in tomcat container
  private final String projectDir = System.getProperty("catalina.base");

  private final String imageTempName = "/temp_images/";
  private final String imageTempDir = projectDir + imageTempName;
  private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentService.class);


  /**
   * Constuctor
   */
  public AssignmentService() {
    try {
      zipHandler = new ZipHandler();
      mailUsername = jenkinsData.getMailUser();
      mailPassword = jenkinsData.getMailPassword();
      gitlabRootUsername = gitlabData.getGitlabRootUsername();
    } catch (LoadConfigFailureException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
  }

  /**
   * @param assignmentName assignment name
   * @param releaseTime    release time
   * @param readMe         read me
   * @param assignmentType assignment type
   * @param file           file
   * @param fileDetail     file detail
   * @return abc
   * @throws Exception abc
   */
  @POST
  @Path("create")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createAssignment(
      @FormDataParam("assignmentName") String assignmentName,
      @FormDataParam("releaseTime") Date releaseTime, @FormDataParam("deadline") Date deadline,
      @FormDataParam("readMe") String readMe, @FormDataParam("fileRadio") String assignmentType,
      @FormDataParam("file") InputStream file,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {


    // 1. Create root project and get project id and url
    gitlabService.createRootProject(assignmentName);

    // 2. Clone the project
    final String cloneDirectoryPath = gitlabService.cloneProject(gitlabRootUsername,
        assignmentName);
//    // 3. Store Zip File to uploads folder if file is not empty
    String filePath = tomcatService.storeFileToUploadsFolder(file, fileDetail.getFileName());

    // 4. Unzip the uploaded file to cloneDirectoryPath
    zipHandler.unzipFile(filePath, cloneDirectoryPath);

    // 5. Add .gitkeep if folder is empty.
    JavaIoUtile.addFile2EmptyFolder(new File(cloneDirectoryPath), ".gitkeep");

    // 6. Copy all description image to temp/images
    LOGGER.debug("Copy all description image to temp/images");
    LOGGER.debug("System.getProperty('catalina.base') is ");
    LOGGER.debug(System.getProperty("catalina.base"));

    ArrayList<String> paths = findAllDescriptionImagePaths(readMe);
    for (String path : paths) {
      String targetPath = path.replace("temp_images", "images");

      File originalFile = new File(projectDir + path);
      File targetFile = new File(projectDir + targetPath);
      LOGGER.debug("start copy :" + originalFile.getPath());
      LOGGER.debug("to :" + targetFile.getPath());

      try {
        JavaIoUtile.copyDirectoryCompatibilityMode(originalFile, targetFile);
      } catch (Exception e) {
        LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
        LOGGER.error(e.getMessage());
      }
    }

    // Delete all images of temp_images folder, but not temp_images folder
    JavaIoUtile.deleteFileInDirectory(new File(imageTempDir));

    // 7. If README is not null
    // First, we need to modify images path
    try {
      readMe = readMe.replaceAll(imageTempName, courseConfig.getTomcatServerIp() + "/images/");
      if (!readMe.equals("<br>") || !"".equals(readMe) || !readMe.isEmpty()) {
        // Add readme to folder
        JavaIoUtile.createUtf8FileFromString(readMe, new File(cloneDirectoryPath, "README.md"));
      }
    } catch (LoadConfigFailureException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
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
    return Response.ok().build();
  }

  /**
   * @param assignmentName                     assignment name
   * @param releaseTime                        release time
   * @param readMe                             read me
   * @param assignmentType                     assignment type
   * @param file                               file
   * @param fileDetail                         file detail
   * @param assignmentCompileOrdersAndScore    assignment compile order & score
   * @return abc
   * @throws Exception abc
   */
  @POST
  @Path("autoAssessment/create")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createAutoAssessment(
      @FormDataParam("assignmentName") String assignmentName,
      @FormDataParam("releaseTime") Date releaseTime, @FormDataParam("deadline") Date deadline,
      @FormDataParam("readMe") String readMe, @FormDataParam("fileRadio") String assignmentType,
      @FormDataParam("file") InputStream file,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("order") String assignmentCompileOrdersAndScore) {
    
    Response response = null;
    
    try {
      AssignmentWithOrderCreator awoc = new AssignmentWithOrderCreator();
      awoc.createAssignment(assignmentName, releaseTime, deadline, readMe,
          assignmentType, file, fileDetail, assignmentCompileOrdersAndScore);
      addOrder(assignmentCompileOrdersAndScore, assignmentName);    

      response = Response.ok().build();
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
      response = Response.serverError().entity(e.getMessage()).build();
    }
    return response;
  }

  /**
   * @param assignmentName    assignment name
   * @param releaseTime       release time
   * @param readMe            read me
   * @param assignmentType    assignment type
   * @param file              file
   * @param fileDetail        file detail
   * @param amount            amount
   * @param reviewStartTime   review release time
   * @param reviewEndTime     review deadline
   * @param metrics           metrics
   * @return response
   * @throws Exception abc
   */
  @POST
  @Path("peerReview/create")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createPeerReview(
      @FormDataParam("assignmentName") String assignmentName,
      @FormDataParam("releaseTime") Date releaseTime,
      @FormDataParam("deadline") Date deadline,
      @FormDataParam("readMe") String readMe,
      @FormDataParam("fileRadio") String assignmentType,
      @FormDataParam("file") InputStream file,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("amount") int amount,
      @FormDataParam("reviewStartTime") Date reviewStartTime,
      @FormDataParam("reviewEndTime") Date reviewEndTime,
      @FormDataParam("metrics") String metrics
  ) {
    Response response = null;

    try {

      // 1. create assignment
      AssignmentWithoutOrderCreator awooc = new AssignmentWithoutOrderCreator();
      awooc.createAssignment(assignmentName,
          releaseTime, deadline, readMe, assignmentType, file, fileDetail);

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

      response = Response.ok().build();
    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
      response = Response.serverError().entity(e.getMessage()).build();
    }
    return response;
  }

  /**
   * Get all assignment which is assign as pair review
   */
  @GET
  @Path("peerReview/allAssignment")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllReviewAssignment() {
    Response response = null;

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
      JSONArray array = new JSONArray();
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
        array.put(ob);
      }
      result.put("allReviewAssignments", array);

      response = Response.ok().entity(result.toString()).build();
    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
      response = Response.serverError().entity(e.getMessage()).build();
    }
    return response;
  }

  /**
   * Get all assignment which is assign as auto assessment
   */
  @GET
  @Path("autoAssessment/allAssignment")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllAutoAssignment() {
    Response response = null;

    try {
      List<Assignment> assignmentList = dbManager.getAutoAssessment();
      JSONObject ob = new JSONObject();
      ob.put("allAutoAssessment", assignmentList);

      response = Response.ok().entity(ob.toString()).build();
    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
      response = Response.serverError().entity(e.getMessage()).build();
    }
    return response;
  }

  public List<String> getAllAssignmentNames() {
    return dbManager.getAllAssignmentNames();
  }

  private String getRootProjectUrl(String name) {
    String url = null;
    String gitlabUrl = null;
    try {
      gitlabUrl = gitlabData.getGitlabRootUrl();
    } catch (LoadConfigFailureException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
    List<GitlabProject> rootProjects = gitlabService.getProject(root);
    for (GitlabProject project : rootProjects) {
      String proName = project.getName();
      if (proName.equals(name)) {
        url = gitlabUrl + "/root/" + name;
      }
    }
    return url;
  }

  /**
   * @param readme Description details
   * @return All images paths
   */
  public ArrayList<String> findAllDescriptionImagePaths(String readme) {
    ArrayList<String> paths = new ArrayList<>();
    Document doc = Jsoup.parse(readme);
    Elements srcAttributes = doc.select("img[src]");
    for (Element src : srcAttributes) {
      paths.add(src.attr("src"));
    }
    return paths;
  }

  /**
   * Send the notification email to student
   */
  public void sendEmail(String email, String name) {
    final String username = mailUsername;
    // final String password = "csclbyqwjhgogypt";// your password
    final String password = mailPassword;

    Properties props = new Properties();
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.port", "587");
    Session session = Session.getInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });

    try {
      String content = "You have a new assignment \"" + name + "\" !";
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(username));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
      message.setSubject("New assignment notification", "utf-8");
      message.setContent(content, "text/html;charset=utf-8");

      Transport.send(message);

    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Add a project to database
   *
   * @param name        Project name
   * @param deadline    Project deadline
   * @param readMe      Project readme
   * @param projectType File type
   */
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

  /**
   * Add the assignment's assessment order to database
   *
   * @param assignmentCompileOrdersAndScore     Assignment order and score
   * @param assignmentName                      Assignment name
   */
  private void addOrder(String assignmentCompileOrdersAndScore, String assignmentName) {
    List<String> ordersList = new ArrayList<>();
    List<String> scoresList = new ArrayList<>();

    String[] ordersAndScores = assignmentCompileOrdersAndScore.split(", ");
    for (String orderAndScore : ordersAndScores) {
      String[] token = orderAndScore.split(":");

      if (token[0].equals("Compile Failure")) {
        ordersList.add("cpf");
      } else if (token[0].equals("Unit Test Failure")) {
        ordersList.add("utf");
      } else if (token[0].equals("Coding Style Failure")) {
        ordersList.add("csf");
      } else if (token[0].equals("HTML Failure")) {
        ordersList.add("whf");
      } else if (token[0].equals("CSS Failure")) {
        ordersList.add("wsf");
      } else if (token[0].equals("JavaScript Failure")) {
        ordersList.add("wef");
      }
      scoresList.add(token[1]);
    }
    //write assignment assessment in to data base
    //addAssignmentAssessment( aid, int sid, int order, int score)
    for (int i = 0; i < ordersAndScores.length; i++) {
      aaDbManager.addAssignmentAssessment(dbManager.getAssignmentIdByName(assignmentName),
          csDbManager.getStatusIdByName(ordersList.get(i)),
          i + 1, Integer.valueOf(scoresList.get(i)));
    }
  }

  /**
   * Add auid to database
   *
   * @param username       username
   * @param assignmentName assignment name
   */
  public void addAuid(String username, String assignmentName) {
    int aid = dbManager.getAssignmentIdByName(assignmentName);
    int uid = userDbManager.getUserIdByUsername(username);

    auDbManager.addAssignmentUser(aid, uid);
  }

  /**
   * delete projects
   *
   * @param name project name
   * @return response
   */
  @POST
  @Path("delete")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteProject(@FormDataParam("assignmentName") String name) {
    Response response = null;

    try {
      Linux linuxApi = new Linux();

      // if this assignment was assigned as pair review, delete review db
      if (rsDbManager.checkAssignmentByAid(name)) {
        deleteReviewDatabase(name);
      }

      // delete db
      deleteAssignmentDatabase(name);

      // delete gitlab
      gitlabService.deleteProjects(name);

      // delete Jenkins

      List<User> users = userService.getStudents();

      for (User user : users) {
        String jobName = jenkins.getJobName(user.getUsername(), name);
        jenkins.deleteJob(jobName);
      }
      response = Response.ok().build();
    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
      response = Response.serverError().entity(e.getMessage()).build();
    }
    return response;
  }

  /**
   * ToDo
   * The unitTest file options is not working now, so need to change the front-end web
   *
   * @param assignmentName project name
   * @return response
   */
  @POST
  @Path("edit")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response editProject(
      @FormDataParam("assignmentName") String assignmentName,
      @FormDataParam("releaseTime") Date releaseTime, @FormDataParam("deadline") Date deadline,
      @FormDataParam("readMe") String readMe, @FormDataParam("file") InputStream file,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("order") String assignmentCompileOrdersAndScore) {
    int aid = dbManager.getAssignmentIdByName(assignmentName);
    dbManager.editAssignment(deadline, releaseTime, readMe, aid);
    if (!assignmentCompileOrdersAndScore.isEmpty()) {
      List<Integer> aaIds = aaDbManager.getAssignmentAssessmentIdByaId(aid);
      List<Integer> scoresList = new ArrayList<>();
      //order: Compile Failure:10, Coding Style Failure:80, Unit Test Failure:10
      String[] ordersAndScores = assignmentCompileOrdersAndScore.split(", ");
      for (String orderAndScore : ordersAndScores) {
        String[] token = orderAndScore.split(":");
        scoresList.add(Integer.valueOf(token[1]));
      }
      for (int i = 0; i < scoresList.size(); i++) {
        aaDbManager.updateScore(aid,
            aaDbManager.getAssessmentOrder(aaIds.get(i)),
            scoresList.get(i));
      }
    }
    return Response.ok().build();
  }

  /**
   * get project checksum
   *
   * @param assignmentName assignment name
   * @return checksum
   */
  @GET
  @Path("getAssignment")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAssignment(@QueryParam("assignmentName") String assignmentName) {
    Assignment assignment = dbManager.getAssignmentByName(assignmentName);
    JSONObject ob = new JSONObject();
    ob.put("description", assignment.getDescription());
    ob.put("deadline", assignment.getDeadline());
    ob.put("type", assignment.getType());
    return Response.ok().entity(ob.toString()).build();
  }

  /**
   * @return AllAssignments
   */
  @GET
  @Path("getAllAssignments")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllAssignments() {
    List<Assignment> assignments = dbManager.getAllAssignment();
    JSONObject ob = new JSONObject();
    ob.put("allAssignments", assignments);
    return Response.ok().entity(ob.toString()).build();
  }

  /**
   * get course name
   *
   * @return course name
   */
  public String getCourseName() {
    String name = "";
    try {
      name = courseConfig.getCourseName();
    } catch (LoadConfigFailureException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

    return name;
  }

  /**
   * get test folder
   *
   * @param filePath folder directory
   * @return zip file
   */
  @GET
  @Path("getTestFile")
  public Response getTestFile(@QueryParam("filePath") String filePath) {
    File file = new File(filePath);

    ResponseBuilder response = Response.ok((Object) file);
    response.header("Content-Disposition", "attachment;filename=");
    return response.build();
  }

  /**
   * delete Assignment from Database by name
   */
  public void deleteAssignmentDatabase(String name) {

    int aid = dbManager.getAssignmentIdByName(name);
    List<Integer> auidList = auDbManager.getAuids(aid);
    List<Integer> aaidList = aaDbManager.getAssignmentAssessmentIdByaId(aid);
    
    for (int aaid : aaidList) {  //Assignment_Assessment
      aaDbManager.deleteAssignmentAssessment(aaid); 
    }

    for (int auid : auidList) { // CommitRecord
      List<Integer> cridList = crDbManager.getCommitRecordId(auid);
      for (int crid : cridList) { // ScreenShot
        srDbManager.deleteScreenshotByCrid(crid);
      }
      crDbManager.deleteRecord(auid);
    }
    auDbManager.deleteAssignmentUserByAid(aid);// Assignment_User
    dbManager.deleteAssignment(name);// Assignment

  }

  /**
   *  delete assignment form pair review db
   */
  public void deleteReviewDatabase(String assignmentName) throws SQLException {
    int aid = dbManager.getAssignmentIdByName(assignmentName);
    int reviewSettingId = rsDbManager.getReviewSettingIdByAid(aid);
    List<AssignmentUser> auList = auDbManager.getAssignmentUserListByAid(aid);

    for (AssignmentUser au : auList) {
      List<PairMatching> pmList = pmDbManager.getPairMatchingByAuId(au.getId());
      for (PairMatching pm : pmList) {
        rrDbManager.deleteReviewRecordByPmId(pm.getId());
        pmDbManager.deletePairMatchingById(pm.getId());
      }
    }

    rsmDbManager.deleteReviewSettingMetricsByAssignmentId(reviewSettingId);
    rsDbManager.deleteReviewSettingByAId(aid);
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

      //
      String orderString = "";
      if (dbManager.getAssignmentType(assignmentName).equals(ProjectTypeEnum.ANDROID) != true) {
        List<String> ordersList = new ArrayList<>();
        String[] ordersAndScores = aaDbManager.getAssignmentOrderAndScore(
            dbManager.getAssignmentIdByName(assignmentName)).split(", ");
        while (ordersList.size() == 0) {
          ordersAndScores = aaDbManager.getAssignmentOrderAndScore(
              dbManager.getAssignmentIdByName(assignmentName)).split(", ");
        }
        for (String orderAndScore : ordersAndScores) {
          String[] token = orderAndScore.split(":");
          ordersList.add(token[0]);
        }
        if (ordersList.isEmpty() != true) {
          for (int i = 0; i < ordersList.size(); i++) {
            orderString += ordersList.get(i);
            if (i < ordersList.size() - 1) {
              orderString += ", ";
            }
          }
        }
      }
      //
      ProjectTypeEnum assignmentTypeEnum = dbManager.getAssignmentType(assignmentName);
      JenkinsProjectConfig jenkinsProjectConfig;
      if ( assignmentTypeEnum.equals(ProjectTypeEnum.WEB) ) {
        jenkinsProjectConfig = new WebPipelineConfig(projectUrl, updateDbUrl,
                username, assignmentName,
                courseConfig.getTomcatServerIp() + "/publicApi/commits/screenshot/updateURL",
            orderString);
      } else {
        jenkinsProjectConfig = JenkinsProjectConfigFactory
                .getJenkinsProjectConfig(assignmentTypeEnum.getTypeName(), projectUrl, updateDbUrl,
                        username, assignmentName);
      }

      JenkinsService jenkinsService = JenkinsService.getInstance();


      jenkinsService.createJobV2(jobName, jenkinsProjectConfig.getXmlConfig());

      jenkinsService.buildJob(jobName);

    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

  }

  /**
   * create previous Assignment
   *
   * @param username username
   */
  public void createPreviousAssignment(String username) {
    List<String> assignmentNames = dbManager.getAllAssignmentNames();

    for (String assignmentName : assignmentNames) {
      createAssignmentSettingsV2(username, assignmentName);
    }

  }

  /**
   * improvise random student to review different student's hw
   *
   * @param amount number of reviewer
   * @param assignmentName assignment name
   */
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

  /**
   * split string to array
   *
   * @param metrics metrics
   */
  public int[] arrayStringToIntArray(String metrics) {
    String[] items = metrics.split(",");
    int[] array = new int[items.length];

    for (int i = 0; i < items.length; i++) {
      array[i] = Integer.parseInt(items[i].trim());
    }

    return array;
  }

  /**
   * update status in pair matching
   *
   * @param aid assignment id
   */
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

  /**
   * change assignment compile order
   *
   * @param fileType fileType
   * @param assignmentCompileOrdersAndScore assignmentCompileOrdersAndScore
   * @param assignmentName assignmentName
   */
  @POST
  @Path("order")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response modifyAssignmentOrderFile(
      @FormDataParam("fileRadio") String fileType, 
      @FormDataParam("order") String assignmentCompileOrdersAndScore,
      @FormDataParam("assignmentName") String assignmentName) {
    Response response = null;
    //-----order: Compile Failure:10, Coding Style Failure:80, Unit Test Failure:10
    List<String> ordersList = new ArrayList<>();
    String[] tokens = assignmentCompileOrdersAndScore.split(", ");
    for (String token:tokens) {
      ordersList.add(token);
    }
    //---------make pom.xml
    try {
      if (fileType.equals("maven")) {
        String mavenResourcesZipPath =
            this.getClass().getResource("/sample/MvnQuickStart.zip").getPath();
        MavenAssignmentSetting mas = new MavenAssignmentSetting();
        ZipHandler zipHandler = new ZipHandler();
        zipHandler.unzipFile(mavenResourcesZipPath,
            assignmentSettingDir + assignmentName);
        mas.createAssignmentSetting(ordersList, assignmentName,
            mavenPomXmlSettingDir);

        File mavenPomXmlSettingFile = new File(mavenPomXmlSettingDir
            + assignmentName + "_pom.xml");
        File assignmentSettingFile = new File(assignmentSettingDir
            + assignmentName + "/pom.xml");
        JavaIoUtile.copyDirectoryCompatibilityMode(mavenPomXmlSettingFile, assignmentSettingFile);

        zipHandler.zipTestFolder(assignmentSettingDir + assignmentName);
      } else if (fileType.equals("web")) {
        String configWebXmlPath =
            this.getClass().getResource("/jenkins/config_web.xml").getPath();

        WebAssignmentSetting was = new WebAssignmentSetting();
        was.createAssignmentSetting(ordersList, assignmentName,
            webConfigXmlSettingDir);

        File webConfigXmlSettingFile = new File(webConfigXmlSettingDir
            + assignmentName + "_config_web.xml");
        File assignmentSettingFile = new File(configWebXmlPath);
        JavaIoUtile.copyDirectoryCompatibilityMode(
            webConfigXmlSettingFile, assignmentSettingFile);
      }
      response = Response.ok().build();
    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
      response = Response.serverError().entity(e.getMessage()).build();
    }
    return response;
  }

  /**
   * create get assignment file
   *
   * @param fileName fileName
   * @return zip file
   * 
   */
  @GET
  @Path("getAssignmentFile")
  public Response getAssignmentFile(@QueryParam("fileName") String fileName) {
    String filePath = assignmentSettingDir + fileName + ".zip";

    File file = new File(filePath);

    ResponseBuilder response = Response.ok((Object) file);
    response.header("Content-Disposition", "attachment;filename=" + fileName + ".zip");
    
    return response.build();
  }

  /**
  * get assignment order
  *
  * @param fileName fileName
  * @return assignment order
  * 
  */
  @GET
  @Path("getAssignmentOrder")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAssignmentOrder(@QueryParam("fileName") String fileName) {
    int aid = dbManager.getAssignmentIdByName(fileName);
    String orders = aaDbManager.getAssignmentOrderAndScore(aid);
    if (orders.isEmpty()) {
      orders = "None";
    }
    JSONObject ob = new JSONObject();
    ob.put("orders", orders);
    
    return Response.ok().entity(ob.toString()).build();
  }
}
