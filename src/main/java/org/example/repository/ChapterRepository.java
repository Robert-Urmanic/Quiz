package org.example.repository;

import org.example.entity.Chapter;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import java.util.List;

public class ChapterRepository {

    public List<Chapter> findByBookId(int bookId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // "Chapter" is the class name, not the table name.
            return session.createQuery(
                            "from Chapter c where c.book.id = :bookId order by c.name",
                            Chapter.class
                    )
                    .setParameter("bookId", bookId)
                    .list();
        }
        // try-with-resources automatically closes the session for you!
    }

    public Chapter findById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Chapter.class, id);
        }
    }

    /**
     * Case-insensitive lookup within one book. Names only have to be unique per
     * book, so "Generics" may exist in both Effective Java and Java in a
     * Nutshell without either one being a duplicate.
     */
    public Chapter findByBookIdAndName(int bookId, String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Chapter c"
                                    + " where c.book.id = :bookId"
                                    + " and lower(c.name) = lower(:name)",
                            Chapter.class
                    )
                    .setParameter("bookId", bookId)
                    .setParameter("name", name)
                    .list()
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
    }
}
