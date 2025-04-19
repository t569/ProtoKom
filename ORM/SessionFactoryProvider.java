public class SessionFactoryProvider {
    public static SessionFactory provideSessionFactory()
    {
        Configuration config = new Configuration();
        config.configure();
        return config.buildSessionFactory();
    }
}
