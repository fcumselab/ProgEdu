package fcu.selab.progedu.service;

import fcu.selab.progedu.config.CourseConfig;
import fcu.selab.progedu.config.GitlabConfig;
import fcu.selab.progedu.config.JenkinsConfig;
import fcu.selab.progedu.conn.GitlabService;
import fcu.selab.progedu.conn.JenkinsService;
import fcu.selab.progedu.conn.TomcatService;
import fcu.selab.progedu.data.Assignment;
import fcu.selab.progedu.data.ProjectTypeEnum;
import fcu.selab.progedu.data.User;
import fcu.selab.progedu.db.AssignmentAssessmentDbManager;
import fcu.selab.progedu.db.AssignmentDbManager;
import fcu.selab.progedu.db.AssignmentUserDbManager;
import fcu.selab.progedu.db.UserDbManager;
import fcu.selab.progedu.exception.LoadConfigFailureException;
import fcu.selab.progedu.jenkinsconfig.JenkinsProjectConfig;
import fcu.selab.progedu.jenkinsconfig.JenkinsProjectConfigFactory;
import fcu.selab.progedu.jenkinsconfig.WebPipelineConfig;
import fcu.selab.progedu.utils.ExceptionUtil;
import fcu.selab.progedu.utils.JavaIoUtile;
import fcu.selab.progedu.utils.ZipHandler;
import org.gitlab.api.models.GitlabProject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AssignmentWithoutOrderCreator {
  private ZipHandler zipHandler;
  private GitlabService gitlabService = GitlabService.getInstance();
  private TomcatService tomcatService = TomcatService.getInstance();
  private UserService userService = UserService.getInstance();
  private String gitlabRootUsername;
  private CourseConfig courseConfig = CourseConfig.getInstance();
  private GitlabConfig gitlabData = GitlabConfig.getInstance();
  private JenkinsConfig jenkinsData = JenkinsConfig.getInstance();

  private AssignmentDbManager adbManager = AssignmentDbManager.getInstance();
  private UserDbManager userDbManager = UserDbManager.getInstance();
  private AssignmentUserDbManager auDbManager = AssignmentUserDbManager.getInstance();
  private AssignmentAssessmentDbManager aaDbManager = AssignmentAssessmentDbManager.getInstance();
  private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentWithoutOrderCreator.class);

  private final String projectDir = System.getProperty("catalina.base");
  private final String tempDir = System.getProperty("java.io.tmpdir");
  private final String uploadDir = tempDir + "/uploads/";
  private final String imageTempName = "/temp_images/";
  private final String imageTempDir = projectDir + imageTempName;

  /**
   * Init assignment with order creator
   *
   * 
   */
  public AssignmentWithoutOrderCreator() {
    try {
      zipHandler = new ZipHandler();
      gitlabRootUsername = gitlabData.getGitlabRootUsername();
    } catch (LoadConfigFailureException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
  }

  /**
   * Create assignment
   * 
   * @param assignmentName assignment name
   * @param releaseTime    release time
   * @param readMe         read me
   * @param assignmentType assignment type
   * @param file           file
   * @param fileDetail     file detail
   * @throws Exception abc
   */
  public void createAssignment(String assignmentName, Date releaseTime,
      Date deadline, String readMe,
      String assignmentType, InputStream file,
      FormDataContentDisposition fileDetail) {

    // 1. Create root project and get project id and url
    gitlabService.createRootProject(assignmentName);

    // 2. Clone the project
    final String cloneDirectoryPath = gitlabService.cloneProject(gitlabRootUsername,
        assignmentName);
    // 3. Store Zip File to uploads folder if file is not empty
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
      ProjectTypeEnum assignmentTypeEnum = adbManager.getAssignmentType(assignmentName);
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
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
  }

  /**
   * Add auid to database
   *
   * @param username       username
   * @param assignmentName assignment name
   */
  public void addAuid(String username, String assignmentName) {
    int aid = adbManager.getAssignmentIdByName(assignmentName);
    int uid = userDbManager.getUserIdByUsername(username);

    auDbManager.addAssignmentUser(aid, uid);
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

    adbManager.addAssignment(assignment);
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
}