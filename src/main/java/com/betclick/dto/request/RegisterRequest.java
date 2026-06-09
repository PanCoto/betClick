package com.betclick.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Login jest wymagany")
    @Size(min = 3, max = 50, message = "Login musi mieć od 3 do 50 znaków")
    private String login;

    @NotBlank(message = "Hasło jest wymagane")
    @Size(min = 6, max = 100, message = "Hasło musi mieć co najmniej 6 znaków")
    private String password;

    @NotBlank(message = "Email jest wymagany")
    @Email(message = "Nieprawidłowy adres email")
    private String email;

    @NotBlank(message = "Imię jest wymagane")
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane")
    private String lastName;

    @NotNull(message = "Data urodzenia jest wymagana")
    private LocalDate dateOfBirth;

    private String phoneNumber;
}
