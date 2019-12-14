package fcu.selab.progedu.status;

import fcu.selab.progedu.data.FeedBack;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidCompileFailure implements Status {

  @Override
  public String extractFailureMsg(String consoleText) {
    String feedback;
    String feedbackStart = "compileDebugJavaWithJavac FAILED";
    String feedbackEnd = "FAILURE: Build failed with an exception.";
    feedback = consoleText.substring(consoleText.indexOf(feedbackStart) + feedbackStart.length(),
            consoleText.indexOf(feedbackEnd));
    /**
     * Remove /var/jenkins_home/workspace/
     */
    feedback = feedback.replaceAll("/var/jenkins_home/workspace/", "");
    return feedback.trim();
  }

  @Override
  public ArrayList<FeedBack> formatExamineMsg(String consoleText) {
    try {
      ArrayList<FeedBack> feedbackList = new ArrayList<>();
      Pattern pattern = Pattern.compile("[0-9]+ error");
      Matcher matcher = pattern.matcher(consoleText);
      String endStr = "";
      if (matcher.find()) {
        endStr = matcher.group(0);
      }
      int endIndex = consoleText.indexOf(endStr);

      while (consoleText.contains(".java")) {
        int error = consoleText.indexOf(": error:");
        int symbol = consoleText.indexOf("symbol:");
        int nextRow = consoleText.indexOf("\n", symbol);
        String fileNameAndLine = consoleText.substring(0, error).trim();

        feedbackList.add(new FeedBack(
                StatusEnum.COMPILE_FAILURE,
                fileNameAndLine.substring(0, fileNameAndLine.indexOf(":")).trim(),
                fileNameAndLine.substring(fileNameAndLine.indexOf(":") + 1, error).trim(),
                consoleText.substring(error + ": error:".length(), symbol).trim().replace("^",""),
                consoleText.substring(symbol + "symbol:".length(), nextRow).trim(),
                "https://developer.android.com/studio/build"
        ));
        consoleText = consoleText.substring(nextRow + 1, endIndex);
        endIndex = endIndex - nextRow - 1;
      }

      return feedbackList;
    } catch (Exception e) {
      ArrayList<FeedBack> feedbackList = new ArrayList<>();
      feedbackList.add(new FeedBack(StatusEnum.COMPILE_FAILURE,"", "", consoleText, "", ""));
      return feedbackList;
    }
  }
}
