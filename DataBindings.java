
// The general idea is DataBindings provide an interface to access databases or just general structures
public class DataBindings <T>{
    
    private DataProvider<T> provider;

    public DataBindings()
    {
        // initially unbound
        this.provider = null;
    }

    /* now we have to make sure we cant do anthing stupid */
    public T get(Object obj) throws Exception
    {
        ensureProvider();
        return provider.get(obj);
    }

    public void post(T entity) throws Exception
    {
        ensureProvider();
        provider.post(entity);
    }

    public void update(T entity) throws Exception
    {
        ensureProvider();
        provider.update(entity);
    }

    public void delete(T obj) throws Exception
    {
        ensureProvider();
        provider.delete(obj);
    }

    public void bindToDataBase(DataProvider<T> provider)
    {
        this.provider = provider;
    }


    private void ensureProvider() {
        if (provider == null) {
            throw new IllegalStateException("No DataProvider bound. Call bindToDatabase() first.");
        }
    }
}   
