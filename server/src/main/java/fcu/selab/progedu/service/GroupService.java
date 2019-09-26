package fcu.selab.progedu.service;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;

import fcu.selab.progedu.conn.GitlabService;
import fcu.selab.progedu.conn.JenkinsService;
import fcu.selab.progedu.db.service.GroupDbService;
import fcu.selab.progedu.db.service.ProjectDbService;
import fcu.selab.progedu.db.service.UserDbService;

@Path("groups")
public class GroupService {

  private GitlabService gitlabService = GitlabService.getInstance();
  private UserService userService = new UserService();
  private GroupDbService gdb = GroupDbService.getInstance();
  private ProjectDbService pdb = ProjectDbService.getInstance();
  private UserDbService udb = UserDbService.getInstance();
  private AssignmentService projectService = new AssignmentService();

  /**
   * create gitlab group
   *
   * @param name        group name
   * @param leader      the username of team leader
   * @param members     the members of group
   * @param projectType project type
   * @param projectName project name
   * @return response
   */
  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createGroup(@FormParam("name") String name, @FormParam("leader") String leader,
      @FormParam("member") List<String> members, @FormParam("projectType") String projectType,
      @FormParam("projectName") String projectName) {
    GitlabGroup gitlabGroup = gitlabService.createGroup(name);
    int groupGitLabId = gitlabGroup.getId();
    members.remove(leader);
    int leaderGitlabId = udb.getGitLabId(leader);
    gitlabService.addMember(groupGitLabId, leaderGitlabId, GitlabAccessLevel.Owner);
    gdb.addGroup(groupGitLabId, name, leader);
    gdb.addMember(leader, name);

    addMembers(name, members);

    // create project
    GroupProjectService gps = new GroupProjectService();
    gps.createGroupProject(name, projectName, projectType);

    return Response.ok().build();
  }

  /**
   * add group members
   * 
   * @param name    group name
   * @param members members
   * @return response
   */
  @POST
  @Path("/{name}/members")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addMembers(@PathParam("name") String name,
      @FormParam("member") List<String> members) {
    int groupGitLabId = gdb.getGitlabId(name);
    for (String member : members) {
      int gitlabId = udb.getGitLabId(member);
      gitlabService.addMember(groupGitLabId, gitlabId, GitlabAccessLevel.Master);
      gdb.addMember(member, name);
    }

    return Response.ok().build();
  }

  /**
   * update team leader
   * 
   * @param name   group name
   * @param leader leader username
   * @return response
   */
  @PUT
  @Path("/{name}/members/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateLeader(@PathParam("name") String name,
      @PathParam("username") String leader) {
    int groupGitLabId = gdb.getGitlabId(name);
    // update newLeader's AccessLevel to owner
    int leaderGitlabId = udb.getGitLabId(leader);
    gitlabService.updateMemberAccessLevel(groupGitLabId, leaderGitlabId, GitlabAccessLevel.Owner);

    String currentLeader = gdb.getLeader(name);
    int currentLeaderGitlabId = udb.getGitLabId(currentLeader);
    // update currentLeader's AccessLevel to master
    gitlabService.updateMemberAccessLevel(groupGitLabId, currentLeaderGitlabId,
        GitlabAccessLevel.Master);
    // update db.group leader
    gdb.updateLeader(name, leader);

    return Response.ok().build();
  }

  /**
   * remove members
   * 
   * @param name   group name
   * @param member member
   * @return response
   */
  @DELETE
  @Path("/{name}/members/{username}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response removeMembers(@PathParam("name") String name,
      @PathParam("username") String member) {
    int groupGitLabId = gdb.getGitlabId(name);
    int gitlabId = udb.getGitLabId(member);
    gitlabService.removeGroupMember(groupGitLabId, gitlabId);
    gdb.removeMember(name, member);

    return Response.ok().build();
  }

  /**
   * remove group
   * 
   * @param name group name
   */
  @DELETE
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public void removeGroup(@PathParam("name") String name) {

    // remove gitlab
//    gitlabService.deleteProjects(name);
    int gitlabId = gdb.getGitlabId(name);
    gitlabService.removeGroup(gitlabId);

    // remove Jenkins
    JenkinsService js = JenkinsService.getInstance();

    List<String> projectNames = pdb.getProjectNames(name);
    for (String projectName : projectNames) {
      String jobName = js.getJobName(name, projectName);
      js.deleteJob(jobName);
    }

    // remove db
    gdb.removeGroup(name);

  }
//
//  private void addMembers(String name, int groupGitLabId, List<String> members) {
//    for (String member : members) {
//      int gitlabId = udb.getGitLabId(member);
//      gitlabService.addMember(groupGitLabId, gitlabId, GitlabAccessLevel.Master);
//      gdb.addMember(member, name);
//    }
//  }

}
