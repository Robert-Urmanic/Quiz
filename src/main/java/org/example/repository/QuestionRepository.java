package org.example.repository;

import org.example.entity.Question;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class QuestionRepository {

    /**
     * Newest questions first. A null bookId, chapterId or subchapterId means
     * "do not filter on that level", so the list can show everything before
     * anything is picked in the dropdowns. Ordering by id gives "last added"
     * because the id is an IDENTITY column.
     */
    public List<Question> findLatest(Integer bookId, Integer chapterId, Integer subchapterId, int limit) {
        // The chapter is fetched under an alias so the book filter can hang off
        // it, and the book itself is fetched along to keep the panel from
        // issuing one extra select per row just to print its title.
        StringBuilder hql = new StringBuilder(
                "select q from Question q"
                        + " left join fetch q.chapter c"
                        + " left join fetch c.book"
                        + " left join fetch q.subchapter"
        );

        appendFilters(hql, bookId, chapterId, subchapterId);
        hql.append(" order by q.id desc");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Question> query = session.createQuery(hql.toString(), Question.class);
            bindFilters(query, bookId, chapterId, subchapterId);

            return query.setMaxResults(limit).list();
        }
    }

    /**
     * How many questions the current selection holds in total, which is more
     * than {@link #findLatest} shows once a subchapter fills up.
     */
    public long count(Integer bookId, Integer chapterId, Integer subchapterId) {
        // The join is unconditional so the filter clauses can be written once
        // and reused by both queries; on a to-one it costs nothing.
        StringBuilder hql = new StringBuilder(
                "select count(q) from Question q left join q.chapter c");

        appendFilters(hql, bookId, chapterId, subchapterId);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(hql.toString(), Long.class);
            bindFilters(query, bookId, chapterId, subchapterId);

            return query.uniqueResult();
        }
    }

    private static void appendFilters(
            StringBuilder hql, Integer bookId, Integer chapterId, Integer subchapterId) {

        hql.append(" where 1 = 1");

        if (bookId != null) {
            // Reached through the chapter rather than a third foreign key on
            // the question, which would be one more thing able to disagree.
            hql.append(" and c.book.id = :bookId");
        }

        if (chapterId != null) {
            hql.append(" and q.chapter.id = :chapterId");
        }

        if (subchapterId != null) {
            hql.append(" and q.subchapter.id = :subchapterId");
        }
    }

    private static void bindFilters(
            Query<?> query, Integer bookId, Integer chapterId, Integer subchapterId) {

        if (bookId != null) {
            query.setParameter("bookId", bookId);
        }

        if (chapterId != null) {
            query.setParameter("chapterId", chapterId);
        }

        if (subchapterId != null) {
            query.setParameter("subchapterId", subchapterId);
        }
    }
}
