package com.example.trading.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "users", schema = "public")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(unique = true)
    private Long chatId;
    private String userName;
    private Boolean gbp;
    private Boolean eur;
    @Lob
    private String metaToken;
    private String metaAccountId;
}
