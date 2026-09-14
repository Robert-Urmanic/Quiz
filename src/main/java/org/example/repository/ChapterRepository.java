package org.example.repository;

import org.example.entity.Chapter;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import java.util.List;

public class ChapterRepository {

    public List<Chapter> findAll() {
        // 1. Open a session from our Utility
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // 2. Write HQL (Notice: "Chapter" is the Class name, not the table name!)
            return session.createQuery("from Chapter order by name", Chapter.class).list();
        }
        // try-with-resources automatically closes the session for you!
    }

    public Chapter findById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Chapter.class, id);
        }
    }

    /**
     * Case-insensitive lookup used to spot a chapter the user is about to
     * create for the second time. Returns null when the name is still free.
     */
    public Chapter findByName(String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Chapter c where lower(c.name) = lower(:name)",
                            Chapter.class
                    )
                    .setParameter("name", name)
                    .list()
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
    }
}
