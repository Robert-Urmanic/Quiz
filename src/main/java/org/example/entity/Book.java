package org.example.entity;

import jakarta.persistence.*;
import java.util.List;

/**
 * The book a set of chapters was taken from. It is the top of the hierarchy:
 * every chapter belongs to exactly one book, which is what lets the same
 * chapter name exist in two books without colliding.
 */
@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Long enough that no real title reaches it, but still a bounded string:
    // the duplicate check compares these with lower(), and Hibernate refuses
    // that function on an unbounded column.
    @Column(name = "title", length = 1000)
    private String title;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL)
    private List<Chapter> chapters;

    public Book() {} // Required

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
