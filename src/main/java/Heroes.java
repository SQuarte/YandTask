import annotations.Key;
import annotations.Table;

/**
 * Created by SQuartes on 29.04.2014.
 */
@Table(name = "heroes")
public class Heroes {
    @Key
    public String name;
    @Key
    public String clazz;

    public Integer level;

    public Integer weaponPower;

    public Integer lastTimeDeath;

}
