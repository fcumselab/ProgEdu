package fcu.selab.progedu.jenkinsconfig;

import org.junit.Test;

import static org.junit.Assert.*;

public class WebPipelineConfigTest {

  @Test
  public void createPipeline() {
    WebPipelineConfig webPipelineConfig = new WebPipelineConfig("franky", "franky"
            , "franky", "franky", "franky",
        "WEB_HTMLHINT_FAILURE, UNIT_TEST_FAILURE, WEB_ESLINT_FAILURE, WEB_STYLELINT_FAILURE");
    System.out.println(webPipelineConfig.getXmlConfig());
  }
}