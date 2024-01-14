package com.example.springdemobot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.ToString;

import java.sql.Timestamp;

@Entity(name = "UsersDataTable")
@Data
@ToString
public class User {
    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;
    private Timestamp registeredAt;


}
