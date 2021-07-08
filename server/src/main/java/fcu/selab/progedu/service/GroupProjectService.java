package fcu.selab.progedu.service;

import java.io.File;
import java.io.IOException;

import fcu.selab.progedu.jenkinsconfig.JenkinsProjectConfig;
import fcu.selab.progedu.jenkinsconfig.WebGroupConfig;
import fcu.selab.progedu.utils.JavaIoUtile;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fcu.selab.progedu.config.CourseConfig;
import fcu.selab.progedu.config.GitlabConfig;
import fcu.selab.progedu.config.JenkinsConfig;
import fcu.selab.progedu.conn.GitlabService;
import fcu.selab.progedu.conn.JenkinsService;
import fcu.selab.progedu.conn.TomcatService;
import fcu.selab.progedu.data.GroupProject;
import fcu.selab.progedu.db.service.ProjectDbService;
import fcu.selab.progedu.exception.LoadConfigFailureException;
import fcu.selab.progedu.data.ProjectTypeEnum;
import fcu.selab.progedu.utils.ZipHandler;
import fcu.selab.progedu.utils.ExceptionUtil;

public class GroupProjectService {
  private static GroupProjectService instance = new GroupProjectService();
  private GitlabService gitlabService = GitlabService.getInstance();
  private ZipHandler zipHandler;
  private TomcatService tomcatService = TomcatService.getInstance();
  private final String tempDir = System.getProperty("java.io.tmpdir");
  private final String uploadDir = tempDir + "/uploads/";
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupProjectService.class);


  public static GroupProjectService getInstance() {
    return instance;
  }

  /**
   * Constuctor
   */
  public GroupProjectService() {
    try {
      zipHandler = new ZipHandler();
    } catch (LoadConfigFailureException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
  }
  

  /**
   *
   * @param groupName   group name
   * @param projectName project name
   * @param projectType projectType
   */
  public void createGroupProjectV2(String groupName, String projectName, String projectType) {

    if (!projectType.equals("web")) {
      LOGGER.error("The createGroupProjectV2 not support" + projectType);
      return;
    }

    String readMe = "Initialization";
    final ProjectTypeEnum projectTypeEnum = ProjectTypeEnum.getProjectTypeEnum(projectType);
    // 1. Create root project and get project id and url
    int projectId = 0;
    try {
      projectId = gitlabService.createGroupProject(groupName, projectName);
    } catch (IOException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

    // 2. Clone the project to C:\\Users\\users\\AppData\\Temp\\uploads
    String cloneDirectoryPath = gitlabService.cloneProject(groupName, projectName);
    // 3. if README is not null
    JavaIoUtile.createUtf8FileFromString(readMe, new File(cloneDirectoryPath, "README.md"));

    // 4 create template
    String filePath = tomcatService.storeWebFileToServer();
    zipHandler.unzipFile(filePath, cloneDirectoryPath);

    // 5. Add .gitkeep if folder is empty.
    JavaIoUtile.addFile2EmptyFolder(new File(cloneDirectoryPath), ".gitkeep");
    // 6. git push
    gitlabService.pushProject(cloneDirectoryPath);

    // 7. remove project directory
    JavaIoUtile.deleteDirectory(new File(uploadDir));

    // 8. import project infomation to database
    addProject(groupName, projectName, readMe, projectTypeEnum);

    String jobName = groupName + "_" +  projectName;
    try {
      // 9. set Gitlab webhook
      GitlabProject project = gitlabService.getProject(projectId);
      gitlabService.setGitlabWebhook(project, jobName);

      // 10. Create Jenkins Job
      GitlabConfig gitlabConfig = GitlabConfig.getInstance();

      CourseConfig courseConfig = CourseConfig.getInstance();
      String progEduApiUrl = courseConfig.getTomcatServerIp() + courseConfig.getBaseuri()
              + "/publicApi/groups";
      String updateDbUrl = progEduApiUrl + "/commits/update";
      String projectUrl = gitlabConfig.getGitlabHostUrl() + "/" + groupName + "/" + projectName
              + ".git";

      JenkinsProjectConfig jenkinsProjectConfig = new WebGroupConfig(projectUrl, updateDbUrl,
                                                                     groupName, projectName);

      JenkinsService jenkinsService = JenkinsService.getInstance();

      jenkinsService.createJobV2(jobName, jenkinsProjectConfig.getXmlConfig());
      jenkinsService.buildJob(jobName);
    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

  }

  /**
   * Add a project to database
   * 
   * @param groupName   group name
   * @param projectName project name
   * @param readMe      readMe
   * @param projectType projectType
   */
  public void addProject(String groupName, String projectName, String readMe,
      ProjectTypeEnum projectType) {
    GroupProject groupProject = new GroupProject();
    groupProject.setName(projectName);
    groupProject.setCreateTime(tomcatService.getCurrentTime());
    groupProject.setDeadline(tomcatService.getCurrentTime());
    groupProject.setDescription(readMe);
    groupProject.setType(projectType);
//    ProjectDbManager projectDb = ProjectDbManager.getInstance();
//    projectDb.addProject(groupProject);

    ProjectDbService gpdb = ProjectDbService.getInstance();
    gpdb.addProject(groupProject, groupName);
  }

}
