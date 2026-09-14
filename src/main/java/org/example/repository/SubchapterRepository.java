package org.example.repository;

import org.example.entity.Subchapter;
import org.example.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class SubchapterRepository {

    public List<Subchapter> findByChapterId(int chapterId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Subchapter s where s.chapter.id = :chapterId order by s.name",
                            Subchapter.class
                    )
                    .setParameter("chapterId", chapterId)
                    .list();
        }
    }

    public Subchapter findById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Subchapter.class, id);
        }
    }

    /**
     * Case-insensitive lookup within one chapter. Names only have to be unique
     * per chapter, so "Basics" may exist under both Java and SQL.
     */
    public Subchapter findByChapterIdAndName(int chapterId, String name) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Subchapter s"
                                    + " where s.chapter.id = :chapterId"
                                    + " and lower(s.name) = lower(:name)",
                            Subchapter.class
                    )
                    .setParameter("chapterId", chapterId)
                    .setParameter("name", name)
                    .list()
                    .stream()
                    .findFirst()
                    .orElse(null);
        }
    }
}
