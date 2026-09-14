package org.example.repository;

import org.example.entity.Question;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class QuestionRepository {

    /**
     * Newest questions first. A null chapterId or subchapterId means "do not
     * filter on that level", so the list can show everything before anything is
     * picked in the dropdowns. Ordering by id gives "last added" because the id
     * is an IDENTITY column.
     */
    public List<Question> findLatest(Integer chapterId, Integer subchapterId, int limit) {
        StringBuilder hql = new StringBuilder(
                "select q from Question q"
                        + " left join fetch q.chapter"
                        + " left join fetch q.subchapter"
        );

        appendFilters(hql, chapterId, subchapterId);
        hql.append(" order by q.id desc");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Question> query = session.createQuery(hql.toString(), Question.class);
            bindFilters(query, chapterId, subchapterId);

            return query.setMaxResults(limit).list();
        }
    }

    /**
     * How many questions the current selection holds in total, which is more
     * than {@link #findLatest} shows once a subchapter fills up.
     */
    public long count(Integer chapterId, Integer subchapterId) {
        StringBuilder hql = new StringBuilder("select count(q) from Question q");

        appendFilters(hql, chapterId, subchapterId);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(hql.toString(), Long.class);
            bindFilters(query, chapterId, subchapterId);

            return query.uniqueResult();
        }
    }

    private static void appendFilters(StringBuilder hql, Integer chapterId, Integer subchapterId) {
        hql.append(" where 1 = 1");

        if (chapterId != null) {
            hql.append(" and q.chapter.id = :chapterId");
        }

        if (subchapterId != null) {
            hql.append(" and q.subchapter.id = :subchapterId");
        }
    }

    private static void bindFilters(Query<?> query, Integer chapterId, Integer subchapterId) {
        if (chapterId != null) {
            query.setParameter("chapterId", chapterId);
        }

        if (subchapterId != null) {
            query.setParameter("subchapterId", subchapterId);
        }
    }
}
