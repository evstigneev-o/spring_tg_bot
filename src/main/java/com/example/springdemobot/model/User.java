package com.example.springdemobot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.sql.Timestamp;

@Entity(name = "UsersDataTable")
@Data
public class User {
    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;
    private Timestamp registeredAt;

    @Override
    public String toString() {
        return "Идентификатор пользователя: " + chatId +
                ", Имя: " + firstName +
                ", Фамилия: " + lastName +
                ", Никнейм: " + userName +
                ", Дата регистрации: " + registeredAt;
    }
}
