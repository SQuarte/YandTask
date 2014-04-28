import annotations.Key;
import annotations.Table;

/**
 * Created by SQuartes on 27.04.2014.
 */
@Table(name = "person")
public class ExampleEntity {
    @Key
    public String name;
}
