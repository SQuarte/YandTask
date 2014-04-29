

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static org.junit.Assert.*;


public class TestHero {
    private static Logger log = Logger.getLogger(TestEntityWithoutTable.class.getName());
    private Implement<Hero> heroesDAO;
    private String URL = "jdbc:postgresql://localhost:5432/postgres";
    private String userName = "postgres";
    private String password = "12345678";

    public TestHero(){
        heroesDAO = new Implement<>(Hero.class,URL,userName,password);
    }

    @Test(expected = RuntimeException.class)
    public void testUniqueInsert(){
        Random random = new Random();
        Hero hero = heroesFactory();
        heroesDAO.insert(hero);
        heroesDAO.insert(hero);
    }

    @Test
    public  void testInsert(){
        Hero hero = heroesFactory();
        heroesDAO.insert(hero);
        Hero dbHero = heroesDAO.selectByKey(hero);
        assertEquals(hero, dbHero);
    }

    @Test
    public void testDelete(){
        Hero hero = heroesFactory();
        heroesDAO.insert(hero);
        heroesDAO.deleteByKey(hero);
        Hero hero2 = new Hero();
        try {
            hero2 = heroesDAO.selectByKey(hero);
        }catch (RuntimeException e){
            hero2 = null;
        }
        assertNull(hero2);
    }

    @Test
    public void testUpdate(){
        Hero hero = heroesFactory();
        heroesDAO.insert(hero);
        Random random = new Random();
        hero.weaponPower = random.nextInt();
        heroesDAO.update(hero);
        Hero dbHero = heroesDAO.selectByKey(hero);
        assertEquals(hero, dbHero);
    }

    @Test
    public void testNotFullHero(){
        Hero hero = heroesFactory();
        hero.weaponPower = null;
        heroesDAO.insert(hero);
        Hero dbHero = heroesDAO.selectByKey(hero);
        assertEquals(hero, dbHero);
    }

    @Test
    public void testSelectAll(){
        Hero hero = heroesFactory();
        heroesDAO.insert(hero);
        List<Hero> heroesList = heroesDAO.selectAll();
        if (!heroesList.contains(hero)) throw new RuntimeException("Нету объекта в выборке из базы");
    }


    private Hero heroesFactory(){
        Hero newHero = new Hero();
        Random random = new Random();
        newHero.name = "Finn" + random.nextLong();
        newHero.clazz = "mage" + random.nextLong();
        newHero.level = random.nextInt();
        newHero.lastTimeDeath = random.nextInt();
        newHero.weaponPower = random.nextInt();
        return newHero;
    }
}
