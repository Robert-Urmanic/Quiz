package org.example.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "chapter")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // optional = false because a chapter without a book has nowhere to sit in
    // the hierarchy the page navigates.
    @ManyToOne(optional = false)
    @JoinColumn(name = "bookId", nullable = false)
    private Book book;

    // Long enough that no real title reaches it, but still a bounded string:
    // the duplicate check compares these with lower(), and Hibernate refuses
    // that function on an unbounded column.
    @Column(name = "name", length = 1000)
    private String name;

    // This tells Hibernate that one Chapter has many Subchapters
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL)
    private List<Subchapter> subchapters;

    public Chapter() {} // Required

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Book getBook() {
        return book;
    }

    public List<Subchapter> getSubchapters() {
        return subchapters;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBook(Book book) {
        this.book = book;
    }
}