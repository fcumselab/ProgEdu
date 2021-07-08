package fcu.selab.progedu.updatecommit;


import fcu.selab.progedu.conn.JenkinsService;
import fcu.selab.progedu.data.CommitRecord;
import fcu.selab.progedu.data.ProjectTypeEnum;
import fcu.selab.progedu.db.UserDbManager;
import fcu.selab.progedu.db.service.ProjectDbService;
import fcu.selab.progedu.db.service.ProjectGroupDbService;
import fcu.selab.progedu.jenkinsjob2status.JenkinsJob2StatusFactory;
import fcu.selab.progedu.jenkinsjob2status.JenkinsJobStatus;
import fcu.selab.progedu.status.StatusEnum;
import fcu.selab.progedu.utils.ExceptionUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Path("groups/commits")
public class GroupCommitService {
  private JenkinsService js = JenkinsService.getInstance();
  private UserDbManager dbManager = UserDbManager.getInstance();
  private ProjectDbService gpdb = ProjectDbService.getInstance();
  private ProjectGroupDbService pgdb = ProjectGroupDbService.getInstance();
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupCommitService.class);

  /**
   * update user assignment commit record to DB.
   *
   * @param groupName username
   * @param projectName assignment name
   * @throws ParseException (to do)
   */
  @POST
  @Path("/update")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateCommitRecord(
      @FormParam("user") String groupName, @FormParam("proName") String projectName) {
    ProjectTypeEnum projectTypeEnum = gpdb.getProjectType(projectName);
    int pgid = pgdb.getId(groupName, projectName);
    CommitRecord cr = gpdb.getCommitResult(pgid);
    int commitNumber = 1;
    if (cr != null) {
      commitNumber = cr.getNumber() + 1;
    }
    Date date = new Date();
    DateFormat time = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
    try {
      date = time.parse(time.format(Calendar.getInstance().getTime()));
    } catch (ParseException e) {
      LOGGER.debug(ExceptionUtil.getErrorInfoFromException(e));
      LOGGER.error(e.getMessage());
    }

    JenkinsJobStatus jobStatus = JenkinsJob2StatusFactory.createJenkinsJobStatus(projectTypeEnum);
    String jobName = groupName + "_" + projectName;
    StatusEnum statusEnum = jobStatus.getStatus(jobName, commitNumber);

    String committer = js.getCommitter(jobName, commitNumber);
    gpdb.insertProjectCommitRecord(pgid, commitNumber, statusEnum, date, committer);
    JSONObject ob = new JSONObject();
    ob.put("pgid", pgid);
    ob.put("commitNumber", commitNumber);
    ob.put("time", time);
    ob.put("status", statusEnum.getType());
    ob.put("committer", committer);

    return Response.ok().entity(ob.toString()).build();
  }
}