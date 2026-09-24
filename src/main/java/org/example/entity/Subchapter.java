package org.example.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "subchapter")
public class Subchapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "chapterId") // This is the column name in your SQL table
    private Chapter chapter;

    // Long enough that no real title reaches it, but still a bounded string:
    // the duplicate check compares these with lower(), and Hibernate refuses
    // that function on an unbounded column.
    @Column(name = "name", length = 1000)
    private String name;

    public Subchapter() {} // Required

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public void setName(String name) {
        this.name = name;
    }
}