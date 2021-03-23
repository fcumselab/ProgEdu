package fcu.selab.progedu.status;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fcu.selab.progedu.data.FeedBack;

import fcu.selab.progedu.utils.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenUnitTestFailure implements Status {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenUnitTestFailure.class);

  @Override
  public String extractFailureMsg(String consoleText) {
    try {
      String unitTest = "";
      String startString = " T E S T S";
      String endString = "BUILD FAILURE";

      unitTest = consoleText.substring(consoleText.indexOf(startString),
          consoleText.indexOf(endString, consoleText.indexOf(startString)));

      return unitTest;
    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
      return "ExtractFailureMsg Method Error";
    }
  }

  @Override
  public ArrayList<FeedBack> formatExamineMsg(String consoleText) {
    String suggest = "https://www.learnjavaonline.org/";
    String consoleContext = consoleText.substring(0, consoleText.indexOf("Results :"));
    String consoleResult = consoleText.substring(consoleText.indexOf("Results :"));
    ArrayList<FeedBack> feedbackList = new ArrayList<>();
    try {
      Pattern pattern = Pattern.compile("(.*?)((\\()(.*?)(\\)))(.*?)(\n)");
      Matcher matcher = pattern.matcher(consoleResult);
      while (matcher.find()) {
        String target = matcher.group(1).replace("Failed tests:", "").trim();
        int targetIndex = consoleContext.indexOf(target);
        int targetEndNextRow = consoleContext.indexOf("\n", consoleContext.indexOf(target));
        int findAtWord = consoleContext.indexOf("at", targetEndNextRow);
        String targetContext = consoleContext.substring(targetIndex, findAtWord).trim();
        String errorMethod = targetContext.substring(0, targetContext.indexOf("("));
        String fileName = targetContext.substring(
            targetContext.indexOf("(") + 1, targetContext.indexOf(")"));
        String errorContext = targetContext.substring(
            targetContext.indexOf("\n"), targetContext.length()).trim();
        String symptom;
        String message = errorMethod;
        if (errorContext.contains(":")) {
          symptom = errorContext.substring(0, errorContext.indexOf(":"));
          message = message + ": "
              + errorContext.substring(errorContext.indexOf(":") + 1, errorContext.length()).trim();
        } else {
          symptom = errorContext;
        }
        fileName = fileName.replace(".", "/");
        feedbackList.add(new FeedBack(
              StatusEnum.UNIT_TEST_FAILURE, fileName, "", message, symptom, suggest
          ));
      }
      if (feedbackList.isEmpty()) {
        feedbackList.add(
            new FeedBack(StatusEnum.UNIT_TEST_FAILURE,
                "Please notify teacher or assistant this situation, thank you!", ""));
      }
    } catch (Exception e) {
      feedbackList.add(
          new FeedBack(StatusEnum.UNIT_TEST_FAILURE,
              "UnitTest ArrayList error", e.getMessage()));
    }
    return feedbackList;
  }
}
