package org.example.entity;

import jakarta.persistence.*;
import org.hibernate.Length;
import java.util.List;

@Entity
@Table(name = "question")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "chapterId") // This is the column name in your SQL table
    private Chapter chapter;

    @ManyToOne
    @JoinColumn(name = "subchapterId")
    private Subchapter subchapter;

    // Without an explicit length these default to varchar(255), which an
    // answer that explains something properly runs past without warning. The
    // dialect turns LONG32 into the database's own unbounded text type, so the
    // mapping stays the same on SQL Server and PostgreSQL alike.
    @Column(name = "question", length = Length.LONG32)
    private String question;

    @Column(name = "answer", length = Length.LONG32)
    private String answer;

    // Integer rather than int: a question that did not come off a page leaves
    // this empty, and 0 is not the same thing as "not filled in".
    @Column(name = "page")
    private Integer page;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public Subchapter getSubchapter() {
        return subchapter;
    }

    public void setSubchapter(Subchapter subchapter) {
        this.subchapter = subchapter;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}