package fcu.selab.progedu.db;
import fcu.selab.progedu.config.MySqlDbConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MySqlDatabaseTest {
    @Test
    void getConnectionTest() {
        MySqlDatabase mySqlDatabase = new MySqlDatabase();
        Connection conn = mySqlDatabase.getConnection();
        try{
            assertEquals("ProgEdu", conn.getCatalog());
        } catch(Exception e){

        }
    }
}
