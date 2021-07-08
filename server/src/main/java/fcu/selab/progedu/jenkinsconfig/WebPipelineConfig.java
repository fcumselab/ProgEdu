package fcu.selab.progedu.jenkinsconfig;

import fcu.selab.progedu.utils.ExceptionUtil;
import fcu.selab.progedu.utils.JavaIoUtile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class WebPipelineConfig extends JenkinsProjectConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebPipelineConfig.class);

  URL baseUrl = this.getClass().getResource("/jenkins/pipelineConfig.xml");
  Document xmlDocument;

  /**
   * WebPipelineConfig
   *
   * @param projectUrl   projectUrl
   */
  public WebPipelineConfig(String projectUrl, String updateDbUrl,
                             String username, String projectName, String updateScreenShotDb) {

    try {
      Path basePath = Paths.get(this.baseUrl.toURI());
      File baseFile = basePath.toFile();

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true); // Todo 我不知道這個要不要刪掉, 先註解起來保留
      DocumentBuilder builder = factory.newDocumentBuilder();
      this.xmlDocument = builder.parse(baseFile);

      setJenkinsPipeline(projectUrl, updateDbUrl, username, projectName, updateScreenShotDb);

    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

  }

  /**
   * WebPipelineConfig
   *
   * @param projectUrl   projectUrl
   */
  public WebPipelineConfig(String projectUrl, String updateDbUrl, String username,
                           String projectName, String updateScreenShotDb, String order) {

    try {
      Path basePath = Paths.get(this.baseUrl.toURI());
      File baseFile = basePath.toFile();

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true); // Todo 我不知道這個要不要刪掉, 先註解起來保留
      DocumentBuilder builder = factory.newDocumentBuilder();
      this.xmlDocument = builder.parse(baseFile);

      setJenkinsPipelineWithOrder(projectUrl, updateDbUrl, username,
          projectName, updateScreenShotDb, order);

    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

  }


  @Override
  public Document getXmlDocument() {
    return this.xmlDocument;
  }


  private void setJenkinsPipeline(String projectUrl, String updateDbUrl,
                                  String username, String projectName, String updateScreenShotDb) {

    String pipeline = createPipeline(projectUrl, updateDbUrl, username,
                                     projectName, updateScreenShotDb);

    this.xmlDocument.getElementsByTagName("script").item(0).setTextContent(pipeline);
  }

  private void setJenkinsPipelineWithOrder(String projectUrl, String updateDbUrl, String username,
                                           String projectName, String updateScreenShotDb,
                                           String order) {

    String pipeline = createPipelineWithOrder(projectUrl, updateDbUrl, username,
        projectName, updateScreenShotDb, order);

    this.xmlDocument.getElementsByTagName("script").item(0).setTextContent(pipeline);
  }

  /**
   * createPipeline
   *
   * @param projectUrl   projectUrl
   * @param updateDbUrl   updateDbUrl
   * @param username   username
   * @param projectName   projectName
   * @param updateScreenShotDb   updateScreenShotDb
   */
  public String createPipeline(String projectUrl, String updateDbUrl,
                               String username, String projectName, String updateScreenShotDb) {
    String newPipeLine = "";
    try {
      URL webPipelineUrl = this.getClass().getResource("/jenkins/web-pipeline");
      Path webPipelinePath = Paths.get(webPipelineUrl.toURI());
      File webPipelineFile = webPipelinePath.toFile();


      String pipeLine = JavaIoUtile.readFileToString(webPipelineFile);
      pipeLine = pipeLine.replaceAll("\\{GitLab-url\\}", projectUrl);
      pipeLine = pipeLine.replaceAll("\\{ProgEdu-server-updateDbUrl\\}", updateDbUrl);
      pipeLine = pipeLine.replaceAll("\\{ProgEdu-user-name\\}", username);
      pipeLine = pipeLine.replaceAll("\\{ProgEdu-project-name\\}", projectName);

      pipeLine = pipeLine.replaceAll("\\{ProgEdu-server-screenshot-updateDbUrl\\}",
              updateScreenShotDb);

      newPipeLine = pipeLine;

    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
    return newPipeLine;
  }

  /**
   * createPipeline
   *
   * @param projectUrl   projectUrl
   * @param updateDbUrl   updateDbUrl
   * @param username   username
   * @param projectName   projectName
   * @param updateScreenShotDb   updateScreenShotDb
   */
  public String createPipelineWithOrder(String projectUrl, String updateDbUrl, String username,
                                        String projectName, String updateScreenShotDb,
                                        String order) {
    String newPipeLine = "";
    String dockerCommand = "docker run -i --rm -v \"$(pwd)\":/usr/src/mynode -w /usr/src/mynode -e "
        + "WEB_PORT=$web_container_port -e WEB_SELENIUM_URL=$WEB_SELENIUM_URL node:12.16.1 ";
    String[] ordersList = {"None", "None", "None", "None"};
    String[] orderTokens = order.split(", ");
    for (int i = 0; i < orderTokens.length; i++) {
      ordersList[i] = orderTokens[i];
    }
    try {
      URL webPipelineUrl = this.getClass().getResource("/jenkins/web-pipeline");
      Path webPipelinePath = Paths.get(webPipelineUrl.toURI());
      File webPipelineFile = webPipelinePath.toFile();


      String pipeLine = JavaIoUtile.readFileToString(webPipelineFile);
      pipeLine = pipeLine.replaceAll("\\{GitLab-url\\}", projectUrl);
      pipeLine = pipeLine.replaceAll("\\{ProgEdu-server-updateDbUrl\\}", updateDbUrl);
      pipeLine = pipeLine.replaceAll("\\{ProgEdu-user-name\\}", username);
      pipeLine = pipeLine.replaceAll("\\{ProgEdu-project-name\\}", projectName);

      pipeLine = pipeLine.replaceAll("\\{ProgEdu-server-screenshot-updateDbUrl\\}",
          updateScreenShotDb);

      //
      pipeLine = pipeLine.replace(dockerCommand + "npm run test", ordersList[0]);
      pipeLine = pipeLine.replace(dockerCommand + "npm run htmlhint", ordersList[1]);
      pipeLine = pipeLine.replace(dockerCommand + "npm run stylelint", ordersList[2]);
      pipeLine = pipeLine.replace(dockerCommand + "npm run eslint", ordersList[3]);

      for (String temp: ordersList) {
        if (temp.equals("UNIT_TEST_FAILURE")) {
          pipeLine = pipeLine.replace("UNIT_TEST_FAILURE", dockerCommand + "npm run test");
        } else if (temp.equals("WEB_HTMLHINT_FAILURE")) {
          pipeLine = pipeLine.replace("WEB_HTMLHINT_FAILURE", dockerCommand + "npm run htmlhint");
        } else if (temp.equals("WEB_STYLELINT_FAILURE")) {
          pipeLine = pipeLine.replace("WEB_STYLELINT_FAILURE", dockerCommand + "npm run stylelint");
        } else if (temp.equals("WEB_ESLINT_FAILURE")) {
          pipeLine = pipeLine.replace("WEB_ESLINT_FAILURE", dockerCommand + "npm run eslint");
        } else {
          pipeLine = pipeLine.replace("None", "");
        }
      }
      //

      newPipeLine = pipeLine;

    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
    return newPipeLine;
  }
}


