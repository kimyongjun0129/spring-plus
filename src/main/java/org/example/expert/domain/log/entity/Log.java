package org.example.expert.domain.log.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log")
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    private LocalDateTime createdAt;

    public Log(String message, LocalDateTime createdAt) {
        this.message = message;
        this.createdAt = createdAt;
    }

    protected Log() {

    }
}
