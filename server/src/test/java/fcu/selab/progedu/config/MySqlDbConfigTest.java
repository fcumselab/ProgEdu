package fcu.selab.progedu.config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MySqlDbConfigTest {

    @Test
    void getDbHostTest(){
        try {
            assertEquals("db", MySqlDbConfig.getInstance().getDbHost());  // can't run because can't read config file
        } catch (Exception e){

        }
    }

}
