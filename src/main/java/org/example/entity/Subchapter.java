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

    @Column(name = "name")
    private String name;

    public Subchapter() {} // Required

    // Getters and Setters...
}