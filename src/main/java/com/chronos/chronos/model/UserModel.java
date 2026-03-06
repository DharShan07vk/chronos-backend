package com.chronos.chronos.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Table (name = "users")
@Entity
public class UserModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false)
    private String name;
    @Column (name = "email", nullable = false)
    @PrimaryKeyJoinColumn
    private String email;
    @Column(name = "password" , nullable = false)
    private String password;

}
