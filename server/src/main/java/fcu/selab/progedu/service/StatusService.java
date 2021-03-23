package fcu.selab.progedu.service;

public class StatusService {
  private static StatusService instance = new StatusService();
  private static final String NPM_ERR = "npm ERR! Failed at";

  public static StatusService getInstance() {
    return instance;
  }

  /**
   * Check is Initialization
   *
   * @param num num
   * @return boolean
   */
  public boolean isInitialization(int num) {
    boolean isInitialization = false;
    if (num == 1) {
      isInitialization = true;
    }
    return isInitialization;
  }

  /**
   * Check is build success
   *
   * @param console console
   * @return boolean
   */
  public boolean isBuildSuccess(String console) {
    boolean isBuildSuccess = false;
    if (console.contains("BUILD SUCCESS")) {
      isBuildSuccess = true;
    }
    return isBuildSuccess;
  }

  /**
   * Check is checkstyle error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isMavenCheckstyleFailure(String console) {
    boolean isCheckstyleError = false;
    String checkstyleErrorMessage = "Failed during checkstyle execution";
    isCheckstyleError = console.contains(checkstyleErrorMessage);
    return isCheckstyleError;
  }

  /**
   * Check is JUnit error
   *
   * @param console jenkins job build console
   * @return boolean
   */
  public boolean isMavenUnitTestFailure(String console) {
    boolean isUnitTestError = false;
    if (console.contains("T E S T S")
        && (console.contains("Failed tests") || console.contains("Tests in error"))) {
      isUnitTestError = true;
    }
    return isUnitTestError;
  }

  /**
   * Check is JUnit error
   *
   * @param console jenkins job build console
   * @return boolean
   */
  public boolean isWebUnitTestFailure(String console) {
    boolean isUnitTestError = false;
    if (console.contains(NPM_ERR) && console.contains("test script.")) {
      isUnitTestError = true;
    }
    return isUnitTestError;
  }

  /**
   * Check is html error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isWebHtmlhintFailure(String console) {
    if (console.contains(NPM_ERR) && console.contains("htmlhint script.")) {
      return true;
    }
    return false;
  }

  /**
   * Check is css error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isWebStylelintFailure(String console) {
    if (console.contains(NPM_ERR) && console.contains("stylelint script.")) {
      return true;
    }
    return false;
  }

  /**
   * Check is es error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isWebEslintFailure(String console) {
    if (console.contains(NPM_ERR) && console.contains("eslint script.")) {
      return true;
    }
    return false;
  }

  /**
   * Android is compile error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isAndroidCompileFailure(String console) {
    return console.contains("compileDebugJavaWithJavac FAILED");
  }

  /**
   * Android is unit test error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isAndroidUnitTestFailure(String console) {
    return console.contains("testDebugUnitTest FAILED");
  }

  /**
   * Android is Ui test error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isAndroidUiTestFailure(String console) {
    return console.contains("connectedDebugAndroidTest FAILED");
  }

  /**
   * Android is checkstyle error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isAndroidCheckstyleFailure(String console) {
    return console.contains("checkStyle FAILED");
  }

  /**
   * Android is lint error
   *
   * @param console jenkins job console text
   * @return boolean
   */
  public boolean isAndroidLintFailure(String console) {
    return console.contains("lint FAILED");
  }

  /**
  * Check is JUnit error
  *
  * @param console jenkins job build console
  * @return boolean
  */
  public boolean isMavenCompileFailureOfUnitTest(String console) {
    boolean isCompileFailureOfUnitTestError = false;
    if (console.contains("testCompile") && console.contains("[ERROR] COMPILATION ERROR : ")) {
      isCompileFailureOfUnitTestError = true;
    }
    return isCompileFailureOfUnitTestError;
  }
}
