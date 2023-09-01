package com.tommy.AnkaraPassportBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDate;

/**
 * For each chat only one user will be used for checking available dates.
 * chatId is unique but could be changed in group chats.
 */
@Entity
public class UserForAnkara {

    @Id
    Long chatId;
    String name;
    String surname;
    String patronymicName;
    String email;
    String phone;
    LocalDate birthday;

    public UserForAnkara() {
    }

    public UserForAnkara(Long chatId, String name, String surname, String patronymicName, String email,
                         String phone, LocalDate birthday) {
        this.chatId = chatId;
        this.name = name;
        this.surname = surname;
        this.patronymicName = patronymicName;
        this.email = email;
        this.phone = phone;
        this.birthday = birthday;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getPatronymicName() {
        return patronymicName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public LocalDate getBirthday() {
        return birthday;
    }
}
