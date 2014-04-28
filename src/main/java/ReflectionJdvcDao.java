import java.util.List;

/**
 * Created by SQuartes on 23.04.2014.
 */
public interface ReflectionJdvcDao<T> {
    public void insert(T object);
    public void update(T object);
    public void deleteByKey(T key);
    public T selectByKey(T key);
    public List<T> selectAll();
}
