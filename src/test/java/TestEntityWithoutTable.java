import org.junit.Test;

import java.util.logging.Logger;

/**
 * Created by SQuartes on 27.04.2014.
 */

public class TestEntityWithoutTable {

    private static Logger log = Logger.getLogger(TestEntityWithoutTable.class.getName());
    private String URL = "jdbc:postgresql://localhost:5432/postgres";
    private String userName = "postgres";
    private String password = "12345678";

    @Test(expected = RuntimeException.class)
    public  void testEmptyTable(){
        Implement<EntityWithoutTable> withouTableDAO = new Implement<>(EntityWithoutTable.class,URL,userName,password);
    }

    @Test(expected = RuntimeException.class)
    public void  testEmptyKey(){
        Implement<EntityWithoutKeys> withoutKeysDAO = new Implement<>(EntityWithoutKeys.class,URL,userName,password);
    }
}
