package fcu.selab.progedu.conn;
import org.gitlab.api.models.GitlabProject;
import org.gitlab.api.models.GitlabUser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.gitlab.api.GitlabAPI;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GitlabServiceTest {

    @Test
    void createUserTest() {
        GitlabService gitlabService = GitlabService.getInstance();
        try{
            GitlabUser user = gitlabService.createUser("TEST@TEST", "password", "testUser", "testUser");
            assertEquals("testUser", user.getUsername());
            gitlabService.deleteUser(user.getId());
        } catch(Exception e){
            System.out.println(e);
        }
    }

}
