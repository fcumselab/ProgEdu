package fcu.selab.progedu.jenkinsconfig;


import fcu.selab.progedu.data.ProjectTypeEnum;

public class JenkinsProjectConfigFactory {

  /**
   * get projectType
   *
   * @param projectType assignmentType
   */
  public static JenkinsProjectConfig getJenkinsProjectConfig(String projectType, String projectUrl,
                                                      String updateDbUrl, String username,
                                                      String assignmentName) {

    ProjectTypeEnum projectTypeEnum = ProjectTypeEnum.getProjectTypeEnum(projectType);

    switch (projectTypeEnum) {
      case JAVAC:
        return new JavacConfig(projectUrl, updateDbUrl,
              username, assignmentName);
      case MAVEN:
        return new MavenPipelineConfig(projectUrl, updateDbUrl,
                  username, assignmentName);
//      case WEB:
//        return new WebConfig(projectUrl, updateDbUrl,
//                username, assignmentName);
      case ANDROID:
        return new AndroidPipelineConfig(projectUrl, updateDbUrl,
                username, assignmentName);
      default:
        return null;
    }
  }

}
