package fcu.selab.progedu.status;

import fcu.selab.progedu.data.FeedBack;
import fcu.selab.progedu.utils.ExceptionUtil;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class MavenUnitTestFailureTest {

  private static final String TEST_FILE = "/MavenUnitTestFailureText.properties";

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenUnitTestFailureTest.class);

  private Properties props;

  public MavenUnitTestFailureTest() {
    InputStream is = this.getClass().getResourceAsStream(TEST_FILE);
    try {
      props = new Properties();
      props.load(is);
    } catch (IOException e) {
      System.out.println(e.getMessage());
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
  }

  @Test
  public void testFormatExamineMsg() {
    String consoleText = System.getenv("TEST");
    MavenUnitTestFailure mavenUnitTestFailure = new MavenUnitTestFailure();
    String consoleTextOne = "";
    String consoleTextTwo = "";
    if (consoleText != null && !consoleText.equals("")) {
      System.out.println("Unable to get properties of TEST case from file;" + TEST_FILE);
    }
    if (props != null) {
      consoleTextOne = props.getProperty("TEST_ONE").trim();
      consoleTextTwo = props.getProperty("TEST_TWO").trim();
    }
    consoleTextOne = mavenUnitTestFailure.extractFailureMsg(consoleTextOne);
    ArrayList arrayListOne = mavenUnitTestFailure.formatExamineMsg(consoleTextOne);
    ArrayList<FeedBack> testArrayOne = new ArrayList<>();
    testArrayOne.add(new FeedBack(StatusEnum.UNIT_TEST_FAILURE, "selab/cofitost/java/AppTest", "", "shouldAnswerWithTrue", "java.lang.AssertionError", "https://www.learnjavaonline.org/"));
    testArrayOne.add(new FeedBack(StatusEnum.UNIT_TEST_FAILURE, "selab/cofitost/java/AppTest", "", "shouldAnswerWithTrue2", "java.lang.AssertionError", "https://www.learnjavaonline.org/"));
    testArrayOne.add(new FeedBack(StatusEnum.UNIT_TEST_FAILURE, "selab/cofitost/java/ThermometerTest", "", "testFahrenheit: expected:<10.0> but was:<100.0>", "java.lang.AssertionError", "https://www.learnjavaonline.org/"));
    testArrayOne.add(new FeedBack(StatusEnum.UNIT_TEST_FAILURE, "selab/cofitost/java/ThermometerTest", "", "testCelsius: expected:<1832.0> but was:<212.0>", "java.lang.AssertionError", "https://www.learnjavaonline.org/"));
    String toJsonOne = mavenUnitTestFailure.tojsonArray(arrayListOne);
    String testJsonOne = mavenUnitTestFailure.tojsonArray(testArrayOne);
    Assert.assertEquals(toJsonOne, testJsonOne);

    consoleTextTwo = mavenUnitTestFailure.extractFailureMsg(consoleTextTwo);
    ArrayList arrayListTwo = mavenUnitTestFailure.formatExamineMsg(consoleTextTwo);
    ArrayList<FeedBack> testArrayTwo = new ArrayList<>();
    testArrayTwo.add(new FeedBack(StatusEnum.UNIT_TEST_FAILURE, "fcu/iecs/q2/Q2Test", "", "testAccountCreate: the length of account array is wrong.", "java.lang.AssertionError", "https://www.learnjavaonline.org/"));
    testArrayTwo.add(new FeedBack(StatusEnum.UNIT_TEST_FAILURE, "selab/myapp/AppTest", "", "testAppTest: expected:<Hello every[]one,\nI am studying a...> but was:<Hello every[ ]one,\nI am studying a...>", "org.junit.ComparisonFailure", "https://www.learnjavaonline.org/"));
    testArrayTwo.add(new FeedBack(StatusEnum.UNIT_TEST_FAILURE, "fcu/iecs/q2/Q2Test", "", "testWithdrawAndDeposite", "java.lang.NullPointerException", "https://www.learnjavaonline.org/"));
    String toJsonTwo = mavenUnitTestFailure.tojsonArray(arrayListTwo);
    String testJsonTwo = mavenUnitTestFailure.tojsonArray(testArrayTwo);
    Assert.assertEquals(toJsonTwo, testJsonTwo);
  }
}
