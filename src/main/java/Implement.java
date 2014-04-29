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


    private static  String URL ;
    private Class targetClass;
    private List<Field> classFields;
    private String tableName;
    Properties connectionProps = new Properties();

    public Implement(Class targetClass,String URL, String userName, String password) {
        this.targetClass = targetClass;
        this.URL = URL;
        connectionProps.put("user", userName);
        connectionProps.put("password", password);
        tableName = getTableName(targetClass);
        classFields = Arrays.asList(targetClass.getFields());
        if (!checkKeyExist(classFields)) throw new RuntimeException("У класса нет ключевых полей");
    }


    @Override
    public void insert(T object) {
        Connection connection = null;
        PreparedStatement p = null;
        InsertMerged insertMerged = mergeInsertParams(getFieldSet(object, classFields));
        boolean isExist = true;
        try{
            selectByKey(object);
        }catch (Exception e){
            isExist = false;
        }
        if (isExist) throw new RuntimeException("Запись с таким ключом уже существует");
        try {
            connection = DriverManager.getConnection(URL, connectionProps);
            String s = "insert into " + tableName + insertMerged.fields + " values " + insertMerged.params;
            log.info("SQl " + s);
            p = connection.prepareStatement(s);
            List<Object> list = insertMerged.paramsValues;
            for (int i = 0; i < list.size(); i++) {
                p.setObject(i + 1, list.get(i));
            }
            p.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeConnections(connection, p);
        }
    }

    @Override
    public void update(T object) {
        Connection connection = null;
        PreparedStatement p = null;
        HashMap<String,Object> objectFields = getFieldSet(object, classFields);
        HashMap<String, Object> keyFields = getKeySet(object, classFields);
        Map.Entry<String, List<Object>> mergedKey = mergeParams(keyFields," and ");
        Map.Entry<String,List<Object>> mergedFields = mergeParams(objectFields," , ");
        try {
            connection = DriverManager.getConnection(URL, connectionProps);
            String sql = " update " + tableName + " set " + mergedFields.getKey() + " where " + mergedKey.getKey();
            p = connection.prepareStatement(sql);
            for (int i = 0; i <mergedFields.getValue().size(); i++){
                p.setObject(i+1, mergedFields.getValue().get(i));
            }
            for (int i = 0; i <mergedKey.getValue().size(); i++){
                p.setObject(i+mergedFields.getValue().size() + 1, mergedKey.getValue().get(i));
            }
            p.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }finally {
            closeConnections(connection,p);
        }
    }

    @Override
    public void deleteByKey(T key) {
        HashMap<String, Object> fields = getKeySet(key, classFields);
        Connection connection = null;
        PreparedStatement p = null;
        try {
            connection = DriverManager.getConnection(URL, connectionProps);
            Map.Entry<String, List<Object>> merged = mergeParams(fields," and ");
            p = connection.prepareStatement("delete from " + tableName + " where " + merged.getKey());
            for (int i = 0; i < merged.getValue().size(); i++) {
                p.setObject(i + 1, merged.getValue().get(i));
            }
            p.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeConnections(connection, p);
        }
    }

    @Override
    public T selectByKey(T key) {
        HashMap<String, Object> fields = getKeySet(key, classFields);
        Connection connection = null;
        PreparedStatement p = null;
        ResultSet resultSet = null;
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", connectionProps);
            Map.Entry<String, List<Object>> merged = mergeParams(fields, " and ");
            p = connection.prepareStatement("select * from " + tableName + " where " + merged.getKey());
            for (int i = 0; i < merged.getValue().size(); i++) {
                p.setObject(i + 1, merged.getValue().get(i));
            }
            resultSet = p.executeQuery();
            return transformSQLtoObject(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            closeConnections(connection, p, resultSet);
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
            closeConnections(connection, p, resultSet);
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

    private void closeConnections(Connection c, PreparedStatement p) {
        try {
            if (p != null) p.close();
            if (c != null) c.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error while closing", e);
        }
    }

    private void closeConnections(Connection c, PreparedStatement p, ResultSet set) {
        try {
            if (p != null) p.close();
            if (c != null) c.close();
            if (set != null) set.close();
        } catch (SQLException e) {
            throw new RuntimeException("Error while closing", e);
        }
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

    private boolean checkKeyExist(List<Field> fields){
        for (Field field : fields) {
            Annotation a = field.getAnnotation(Key.class);
            if (a != null) return true;
        }
        return false;
    }

    private InsertMerged mergeInsertParams(HashMap<String, Object> params) {
        StringBuilder f = new StringBuilder(" ( ");
        StringBuilder p = new StringBuilder(" ( ");
        List<Object> values = new ArrayList<>();
        boolean isFirst = true;
        for (Map.Entry<String, Object> param : params.entrySet()) {
            values.add(param.getValue());
            if (!isFirst) {
                f.append(" , ");
                p.append(" , ");
            }
            f.append(param.getKey());
            p.append("?");
            isFirst = false;
        }
        f.append(" ) ");
        p.append(" ) ");
        return new InsertMerged(f.toString(),
                p.toString(),
                values);
    }

    private Map.Entry<String, List<Object>> mergeParams(HashMap<String, Object> params,String division) {
        StringBuilder result = new StringBuilder();
        List<Object> paramsValue = new ArrayList<>();
        boolean isFirst = true;
        for (Map.Entry<String, Object> p : params.entrySet()) {
            if (!isFirst) {
                result.append(division);
            } else {
                isFirst = false;
            }
            result.append(p.getKey()).append(" = ? ");
            paramsValue.add(p.getValue());
        }
        return new HashMap.SimpleEntry<>(result.toString(), paramsValue);
    }


    private T transformSQLtoObject(ResultSet resultSet) {
        T t;
        try {
            t = (T) targetClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
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
                        throw new RuntimeException("WTF", e);
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
                T t = (T) targetClass.newInstance();
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
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
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
