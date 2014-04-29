import annotations.Key;
import annotations.Table;

/**
 * Created by SQuartes on 29.04.2014.
 */
@Table(name = "heroes")
public class Hero {
    @Key
    public String name;
    @Key
    public String clazz;

    public Integer level;

    public Integer weaponPower;

    public Integer lastTimeDeath;

    @Override
    public boolean equals(Object heroO){
        Hero hero = (Hero)heroO;
        if (this.name.equals(hero.name) &&
            this.clazz.equals(hero.clazz) &&
            this.level.equals(hero.level) &&(
                (this.weaponPower == null && hero.weaponPower==null )|| this.weaponPower.equals(hero.weaponPower) )&&
            this.lastTimeDeath.equals(hero.lastTimeDeath)) return  true;
        else return false;
    }

}
