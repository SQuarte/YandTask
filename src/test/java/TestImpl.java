import org.junit.Test;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by SQuartes on 27.04.2014.
 */

public class TestImpl {

    private static Logger log = Logger.getLogger(TestImpl.class.getName());

    @Test
    public  void test(){
        ExampleEntity a = new ExampleEntity();
        a.name = "egor";
        Implement<ExampleEntity> implement= new Implement<>(ExampleEntity.class);
        List<ExampleEntity> entityList = implement.selectAll();
        log.info(String.valueOf(entityList.size()));
    }
}
