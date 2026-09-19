package org.example.repository;

import org.example.entity.Book;
import org.example.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class BookRepository {

    public List<Book> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from Book order by title", Book.class).list();
        }
    }

    public Book findById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Book.class, id);
        }
    }

    /**
     * Case-insensitive lookup used to spot a book the user is about to add for
     * the second time. Returns null when the title is still free.
     */
    public Book findByTitle(String title) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Book b where lower(b.title) = lower(:title)",
                            Book.class
                    )
                    .setParameter("title", title)
                    .list()
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
    }
}
