package org.example.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "chapter")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    // This tells Hibernate that one Chapter has many Subchapters
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL)
    private List<Subchapter> subchapters;

    public Chapter() {} // Required

    // Getters and Setters...
}