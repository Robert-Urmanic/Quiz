package org.example.repository;

import org.example.entity.Question;
import org.example.entity.Subchapter;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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

    /**
     * Every question of a book in reading order: numbered positions first,
     * then the ones never reordered in the order they were added. The id is
     * the tie-breaker in both cases, so the order is stable even before a
     * subchapter has been touched.
     */
    public List<Question> findByBookId(int bookId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "select q from Question q"
                                    + " join fetch q.chapter c"
                                    + " left join fetch q.subchapter"
                                    + " where c.book.id = :bookId"
                                    + " order by q.position asc nulls last, q.id asc",
                            Question.class
                    )
                    .setParameter("bookId", bookId)
                    .list();
        }
    }

    /**
     * Rewrites the text, answer and page of one question, and moves it to
     * another subchapter when subchapterId names a different one. Returns
     * null when the question or the target subchapter is gone, so the caller
     * can tell the user to reload.
     */
    public Question update(int id, String text, String answer, Integer page, Integer subchapterId) {
        return HibernateUtil.inTransaction(session -> {
            Question question = session.get(Question.class, id);

            if (question == null) {
                return null;
            }

            question.setQuestion(text);
            question.setAnswer(answer);
            question.setPage(page);

            boolean moving = subchapterId != null
                    && (question.getSubchapter() == null
                        || question.getSubchapter().getId() != subchapterId);

            if (moving) {
                Subchapter target = session.get(Subchapter.class, subchapterId);

                if (target == null) {
                    return null;
                }

                moveToEnd(session, question, target);
            }

            return question;
        });
    }

    /**
     * Files the question as the last one of the target subchapter. The
     * target is numbered through first: its never-reordered questions sort
     * by id, and a moved question with an older id would otherwise slip in
     * among them instead of landing at the end. The numbers the question
     * leaves behind need no closing up, since only their order is shown.
     */
    private static void moveToEnd(Session session, Question question, Subchapter target) {
        List<Question> already = session.createQuery(
                        "from Question q where q.subchapter.id = :subchapterId"
                                + " order by q.position asc nulls last, q.id asc",
                        Question.class
                )
                .setParameter("subchapterId", target.getId())
                .list();

        for (int i = 0; i < already.size(); i++) {
            already.get(i).setPosition(i + 1);
        }

        question.setSubchapter(target);
        // Derived from the subchapter, the same way a new question gets it,
        // so the two can never disagree.
        question.setChapter(target.getChapter());
        question.setPosition(already.size() + 1);
    }

    /**
     * Numbers the questions of one subchapter 1..n in the order given. The ids
     * have to be exactly the questions the subchapter holds: a list that
     * misses one or brings a stranger along was built from a stale page, and
     * applying it would leave two questions sharing a number. Returns false
     * in that case and changes nothing.
     */
    public boolean reorder(int subchapterId, List<Integer> orderedIds) {
        return HibernateUtil.inTransaction(session -> {
            Map<Integer, Question> byId = new HashMap<>();

            session.createQuery(
                            "from Question q where q.subchapter.id = :subchapterId",
                            Question.class
                    )
                    .setParameter("subchapterId", subchapterId)
                    .list()
                    .forEach(question -> byId.put(question.getId(), question));

            if (orderedIds.size() != byId.size()
                    || !byId.keySet().equals(new HashSet<>(orderedIds))) {
                return false;
            }

            for (int i = 0; i < orderedIds.size(); i++) {
                byId.get(orderedIds.get(i)).setPosition(i + 1);
            }

            return true;
        });
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
