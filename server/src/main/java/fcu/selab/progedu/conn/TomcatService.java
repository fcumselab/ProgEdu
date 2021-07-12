package fcu.selab.progedu.conn;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fcu.selab.progedu.utils.ExceptionUtil;

public class TomcatService {

  private static TomcatService instance = new TomcatService();

  private static final Logger LOGGER = LoggerFactory.getLogger(TomcatService.class);

  public static TomcatService getInstance() {
    return instance;
  }

  /**
   * if file exist, then will overwrite that file.
   * if not success store, return ""
   *
   * @param file        (to do)
   * @param projectName (to do)
   * @return target
   */
  public String storeFileToUploadsFolder(InputStream file, String projectName) {
    String uploadsDir = System.getProperty("java.io.tmpdir") + "/" +  "uploads";
    File uploadsFolder = new File(uploadsDir);
    if (!uploadsFolder.exists()) {
      uploadsFolder.mkdirs();
    }

    String target = uploadsDir + "/" + projectName;
    try {
      storeFile(file, target);
    } catch (SecurityException | IOException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
      return "";
    }

    return target;
  }

  /**
   * (to do)
   *
   * @param file     (to do)
   * @param fileName (to do)
   * @return target
   */
  public String storeDescriptionImage(String uploadsDir, String fileName, InputStream file) {
    File uploadsFolder = new File(uploadsDir);
    if (!uploadsFolder.exists()) {
      uploadsFolder.mkdirs();
    }
    String target = uploadsDir + fileName;
    try {
      storeFile(file, target);
    } catch (SecurityException | IOException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
    return target;
  }

  /**
   * Utility method to save InputStream data to target location/file
   *
   * @param input  - InputStream to be saved
   * @param target - full path to destination file
   */
  private void storeFile(InputStream input, String target) throws IOException {
    OutputStream output = new FileOutputStream(target);
    IOUtils.copy(input, output);
  }

  /**
   * (to do)
   *
   * @return target (to do)
   */
  public String storeWebFileToServer() {

    String fileName = "WebQuickStart.zip";
    String filePath = this.getClass().getResource("/sample/" + fileName).getFile();
    File sample = new File(filePath);
    InputStream file;
    try {
      file = new FileInputStream(sample);
      return storeFileToUploadsFolder(file, fileName);
    } catch (FileNotFoundException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

    return "";
  }

  /**
   * get server current time
   *
   * @return current time
   */
  public Date getCurrentTime() {
    Date date = new Date();
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    format.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));
    try {
      date = format.parse(format.format(date));
    } catch (Exception e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }
    return date;

  }

  /**
   * (to do)
   *
   * @return target (to do)
   */
  public String storeMvnFileToServer() {

    String fileName = "MvnQuickStart.zip";
    String filePath = this.getClass().getResource("/sample/" + fileName).getFile();
    File sample = new File(filePath);
    InputStream file;
    try {
      file = new FileInputStream(sample);
      return storeFileToUploadsFolder(file, fileName);
    } catch (FileNotFoundException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

    return "";
  }
}
