package org.example;

import org.example.util.HibernateUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        // Fails fast on a bad or missing database configuration, before the
        // web server starts accepting requests against a dead connection.
        HibernateUtil.getSessionFactory();

        SpringApplication.run(Main.class, args);
    }
}
