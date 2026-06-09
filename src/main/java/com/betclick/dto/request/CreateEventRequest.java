package com.betclick.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateEventRequest {

    @NotNull(message = "Liga jest wymagana")
    private Long leagueId;

    @NotBlank(message = "Nazwa meczu jest wymagana")
    private String name;

    @NotBlank(message = "Gospodarz (drużyna A) jest wymagany")
    private String teamA;

    @NotBlank(message = "Gość (drużyna B) jest wymagany")
    private String teamB;

    @NotNull(message = "Data i godzina rozpoczęcia są wymagane")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @NotNull(message = "Przewidywany czas trwania jest wymagany")
    @Min(value = 1, message = "Czas trwania musi wynosić co najmniej 1 minutę")
    private Integer expectedDurationMinutes = 90;

    @NotNull(message = "Kurs na 1 jest wymagany")
    @DecimalMin(value = "1.00", message = "Kurs na 1 nie może być mniejszy niż 1.00")
    private BigDecimal homeOdds = new BigDecimal("2.00");

    @DecimalMin(value = "1.00", message = "Kurs na X nie może być mniejszy niż 1.00")
    private BigDecimal drawOdds = new BigDecimal("3.20");

    @NotNull(message = "Kurs na 2 jest wymagany")
    @DecimalMin(value = "1.00", message = "Kurs na 2 nie może być mniejszy niż 1.00")
    private BigDecimal awayOdds = new BigDecimal("2.00");
}
