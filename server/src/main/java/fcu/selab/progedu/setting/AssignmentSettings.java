package fcu.selab.progedu.setting;

import java.util.List;

public interface AssignmentSettings {

  public void createAssignmentSetting(
      List<String> order, String name, String targetSavePath);
}