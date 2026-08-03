package org.example.repository;

import org.example.entity.Chapter;
import org.example.entity.Subchapter;
import org.example.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class SubchapterRepository {

    public List<Subchapter> findByChapterId(int chapterId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Subchapter s where s.chapter.id = :chapterId",
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
}