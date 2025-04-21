/*
 * Hey guys; now remember, T is always the type were manipulating
 * The providers just define where we store them
 */

public interface DataProvider<T> {
    T get(Object obj) throws Exception;
    void post(T entity) throws Exception;
    void update(T entity) throws Exception;
    void delete(T obj) throws Exception;
}
