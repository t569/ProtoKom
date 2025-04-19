import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import java.io.Serializable;

public class ORMDataProvider<T, ID extends Serializable> implements DataProvider {
    private final SessionFactory sessionFactory;
    private final Class<T> entityClass;


    public ORMDataProvider(SessionFactory sessionFactory, Class<T>entityClass){
        this.sessionFactory = sessionFactory;
        this.entityClass = entityClass;
    }

    @Override
    public T get(Object id) throws Exception {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            T entity = session.get(entityClass, (ID) id);
            tx.commit();
            return entity;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new Exception("Error fetching " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void post(T entity) throws Exception {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.save(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new Exception("Error saving " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void update(T entity) throws Exception {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            session.update(entity);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new Exception("Error updating " + entityClass.getSimpleName(), e);
        }
    }

    @Override
    public void delete(Object id) throws Exception {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            T entity = session.get(entityClass, (ID) id);
            if (entity != null) {
                session.delete(entity);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new Exception("Error deleting " + entityClass.getSimpleName(), e);
        }
    }
}
