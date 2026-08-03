package org.example.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Loads hibernate.cfg.xml from resources, which holds the local
            // SQL Server defaults used during development.
            Configuration configuration = new Configuration().configure();

            // Hosting providers expose the database as a single DATABASE_URL in
            // postgres://user:password@host:port/database form, so translate it
            // into the three settings Hibernate expects.
            applyDatabaseUrl(configuration, System.getenv("DATABASE_URL"));

            // Anything set in the environment wins, so the same build can run
            // locally against SQL Server and in the cloud against another
            // database without touching the code.
            overrideFromEnv(configuration, "DB_URL", "hibernate.connection.url");
            overrideFromEnv(configuration, "DB_USER", "hibernate.connection.username");
            overrideFromEnv(configuration, "DB_PASSWORD", "hibernate.connection.password");
            overrideFromEnv(configuration, "DB_DRIVER", "hibernate.connection.driver_class");
            overrideFromEnv(configuration, "DB_DDL_AUTO", "hibernate.hbm2ddl.auto");

            return configuration.buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Translates a postgres://user:password@host:port/database URL into the
     * JDBC url plus separate credentials. Anything else is left alone so an
     * unexpected value fails loudly on connect instead of being half-applied.
     */
    private static void applyDatabaseUrl(Configuration configuration, String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        URI uri = URI.create(databaseUrl.trim());
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("postgres") || scheme.equals("postgresql"))) {
            System.err.println("Ignoring DATABASE_URL with unsupported scheme: " + scheme);
            return;
        }

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String database = uri.getPath() == null ? "" : uri.getPath();
        String query = uri.getQuery() == null ? "sslmode=require" : uri.getQuery();

        configuration.setProperty("hibernate.connection.url",
                "jdbc:postgresql://" + uri.getHost() + ":" + port + database + "?" + query);

        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            int separator = userInfo.indexOf(':');
            String user = separator == -1 ? userInfo : userInfo.substring(0, separator);
            configuration.setProperty("hibernate.connection.username", decode(user));
            if (separator != -1) {
                configuration.setProperty("hibernate.connection.password",
                        decode(userInfo.substring(separator + 1)));
            }
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void overrideFromEnv(Configuration configuration, String envName, String property) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            configuration.setProperty(property, value);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        // Close caches and connection pools
        getSessionFactory().close();
    }

    public static void save(Object object) {

        Session session = getSessionFactory().openSession();

        Transaction tx = session.beginTransaction();

        session.persist(object);

        tx.commit();

        session.close();
    }
}
