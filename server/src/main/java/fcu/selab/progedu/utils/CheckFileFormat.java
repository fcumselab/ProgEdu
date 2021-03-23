package fcu.selab.progedu.utils;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckFileFormat {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaIoUtile.class);

  /**
   * Check file
   * @param filePath file path
   * @param assignmentType type
   */
  public static void checkFile(String filePath, String assignmentType) {
    File file = new File(filePath);
    if (file.exists()) {
      switch (assignmentType) {
        case "web":
          checkWebFile(file);
          break;
        default:
      }
    }
  }

  /**
   * check web file
   * @param dir file path
   */
  public static void checkWebFile(File dir) {
    File[] files = dir.listFiles();
    if (dir.getPath().contains("src/test")) {
      addFile(dir, "Test.js");
    } else if (dir.getPath().contains("src/web/html")) {
      addFile(dir,"index.html");
    }
    for (File file:files) {
      if (file.isDirectory()) {
        checkWebFile(file);
      }
    }

  }

  /**
   * add file
   * @param dir file path
   * @param newFileName new file name
   */
  public static void addFile(File dir,String newFileName) {
    if (dir.length() == 0) {
      File newFile = new File(dir.getPath() + "/" + newFileName);
      if (!newFile.exists()) {
        try {
          newFile.createNewFile();
        } catch (IOException e) {
          LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
          LOGGER.error(e.getMessage());
        }
      }
    }

  }

}
