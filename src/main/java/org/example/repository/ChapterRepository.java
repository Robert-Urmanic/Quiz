package org.example.repository;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.entity.Chapter;
import org.example.util.HibernateUtil;
import org.hibernate.Session;
import java.util.List;

public class ChapterRepository {

    public List<Chapter> findAll() {
        // 1. Open a session from our Utility
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            // 2. Write HQL (Notice: "Chapter" is the Class name, not the table name!)
            return session.createQuery("from Chapter", Chapter.class).list();
        }
        // try-with-resources automatically closes the session for you!
    }

    public ObservableList<String> findAllNames() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            List<String> names = session.createQuery(
                    "select c.name from Chapter c", String.class
            ).list();

            return FXCollections.observableArrayList(names);
        }
    }

//    public ObservableList<String> findAllObsList() {
//        // 1. Open a session from our Utility
//        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
//
//            // 2. Write HQL (Notice: "Chapter" is the Class name, not the table name!)
//            return session.createQuery("from Chapter", Chapter.class).list();
//        }
//        // try-with-resources automatically closes the session for you!
//    }

    public Chapter findById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Chapter.class, id);
        }
    }
}