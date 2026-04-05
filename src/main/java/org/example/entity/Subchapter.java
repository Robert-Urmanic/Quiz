package org.example.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "subchapter")
public class Subchapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "chapterId")
    private String chapterId;

    @Column(name = "name")
    private String name;

    public Subchapter() {} // Required

    // Getters and Setters...
}