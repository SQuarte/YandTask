import annotations.Key;
import annotations.Table;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by SQuartes on 23.04.2014.
 */
public class Implement<T> implements ReflectionJdvcDao<T> {

    private static Logger log = Logger.getLogger(Implement.class.getName());

    private static final String userName = "postgres";
    private static final String password = "12345678";
    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private Class targetClass;
    private String tableName;
    Properties connectionProps = new Properties();

    public Implement(Class targetClass) {
        this.targetClass = targetClass;
        connectionProps.put("user", userName);
        connectionProps.put("password", password);
        tableName = getTableName(targetClass);
    }


    @Override
    public void insert(T object) {
        Class c = object.getClass();
        Connection connection = null;
        PreparedStatement p = null;
        ResultSet resultSet = null;
        InsertMerged insertMerged = mergeInsertParams(getFieldSet(object, Arrays.asList(c.getFields())));
        try {
            connection = DriverManager.getConnection(URL, connectionProps);
            String s = "insert into " + tableName + insertMerged.fields + " values " + insertMerged.params;
            log.info("SQL " + s);
            p = connection.prepareStatement(s);
            List<Object> list = insertMerged.paramsValues;
            for (int i = 0; i < list.size(); i++) {
                p.setObject(i + 1, list.get(i));
            }
            p.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(T object) {
        deleteByKey(object);
        selectByKey(object);
    }

    @Override
    public void deleteByKey(T key) {
        {
            Class c = key.getClass();
            HashMap<String, Object> fields = getKeySet(key, Arrays.asList(c.getDeclaredFields()));
            Connection connection = null;
            PreparedStatement p = null;
            ResultSet resultSet = null;
            try {
                connection = DriverManager.getConnection(URL, connectionProps);
                Map.Entry<String, List<Object>> merged = mergeParams(fields);
                p = connection.prepareStatement("delete from " + tableName + " where " + merged.getKey());
                for (int i = 0; i < merged.getValue().size(); i++) {
                    p.setObject(i + 1, merged.getValue().get(i));
                }
                p.execute();
                return;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (p != null) p.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    public T selectByKey(T key) {
        Class c = key.getClass();
        HashMap<String, Object> fields = getKeySet(key, Arrays.asList(c.getDeclaredFields()));
        Connection connection = null;
        PreparedStatement p = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", connectionProps);
            Map.Entry<String, List<Object>> merged = mergeParams(fields);
            p = connection.prepareStatement("select * from " + tableName + " where " + merged.getKey());
            for (int i = 0; i < merged.getValue().size(); i++) {
                p.setObject(i + 1, merged.getValue().get(i));
            }
            resultSet = p.executeQuery();
            return transformSQLtoObject(key.getClass(), resultSet);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (p != null) p.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<T> selectAll() {
        String tableName = getTableName(targetClass);
        Connection connection = null;
        PreparedStatement p = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", connectionProps);
            p = connection.prepareStatement("select * from " + tableName);
            resultSet = p.executeQuery();
            return transformSQLtoObjectList(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException("SQl error", e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (p != null) p.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private String getTableName(Class c) {
        Annotation a = c.getAnnotation(Table.class);
        if (a == null) throw new RuntimeException("Сущность не содержит имени таблицы");
        Table table = (Table) a;
        return table.name();
    }

    private static String camelToUnder(String s) {
        String regex = "([a-z])([A-Z]+)";
        String replacement = "$1_$2";
        return s.replaceAll(regex, replacement).toLowerCase();
    }

    private HashMap<String, Object> getFieldSet(T key, List<Field> fields) {
        HashMap<String, Object> result = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object o = null;
            try {
                o = field.get(key);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("WTF");
            }
            if (o != null) {
                result.put(camelToUnder(field.getName()), o);
            }
        }
        return result;
    }

    private HashMap<String, Object> getKeySet(T key, List<Field> fields) {
        HashMap<String, Object> result = new HashMap<>();
        for (Field field : fields) {
            Annotation a = field.getAnnotation(Key.class);
            if (a != null) {
                Object o = null;
                field.setAccessible(true);
                try {
                    o = field.get(key);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (o == null) {
                    throw new RuntimeException("Не задано значения ключевого поля " + field.getName());
                } else {
                    result.put(camelToUnder(field.getName()), o);
                }

            }
        }
        return result;
    }

    private InsertMerged mergeInsertParams(HashMap<String, Object> params) {
        StringBuilder f = new StringBuilder(" ( ");
        StringBuilder p = new StringBuilder(" ( ");
        List<Object> values = new ArrayList<>();
        boolean isFirst = true;
        for (Map.Entry<String, Object> param : params.entrySet()) {
            f.append(param.getKey());
            p.append("?");
            values.add(param.getValue());
            if (!isFirst) {
                f.append(" , ");
                p.append(" , ");
            }
            isFirst = false;
        }
        f.append(" ) ");
        p.append(" ) ");
        return new InsertMerged(f.toString(),
                p.toString(),
                values);
    }

    private Map.Entry<String, List<Object>> mergeParams(HashMap<String, Object> params) {
        StringBuilder result = new StringBuilder();
        List<Object> paramsValue = new ArrayList<>();
        boolean isFirst = true;
        for (Map.Entry<String, Object> p : params.entrySet()) {
            if (!isFirst) {
                result.append(" and ");
            } else {
                isFirst = false;
            }
            result.append(p.getKey() + " = ? ");
            paramsValue.add(p.getValue());
        }
        log.info("PARAMS =  " + result.toString());
        return new HashMap.SimpleEntry<String, List<Object>>(result.toString(), paramsValue);
    }

    private T transformSQLtoObject(Class c, ResultSet resultSet) {
        log.info("HERE");
        T t;
        try {
            t = (T) targetClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Error");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error");
        }

        try {
            if (resultSet.next()) {
                List<Field> resultFields = Arrays.asList(t.getClass().getFields());
                for (Field f : resultFields) {
                    f.setAccessible(true);
                    String name = camelToUnder(f.getName());
                    Object object = resultSet.getObject(name);
                    try {
                        f.set(t, object);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("WTF");
                    }
                }
            } else {
                throw new RuntimeException("Пустой результсет");
            }
            if (resultSet.next()) throw new RuntimeException("Не уникальный результат");
            return t;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<T> transformSQLtoObjectList(ResultSet resultSet) {
        List<T> result = new ArrayList<>();
        try {
            while (resultSet.next()) {
                T t = (T) new Object();
                List<Field> resultFields = Arrays.asList(t.getClass().getFields());
                for (Field f : resultFields) {
                    f.setAccessible(true);
                    String name = camelToUnder(f.getName());
                    Object object = resultSet.getObject(name);
                    try {
                        f.set(t, object);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("WTF");
                    }
                }
                result.add(t);
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    class InsertMerged {
        public String fields;
        public String params;
        List<Object> paramsValues;

        public InsertMerged(String fields, String params, List<Object> paramsValues) {
            this.fields = fields;
            this.params = params;
            this.paramsValues = paramsValues;
        }
    }
}
