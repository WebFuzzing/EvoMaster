package com.foo.jakarta;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;

@Entity
public class PersonEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Min(value = 0)
    public int age;

    @Max(value = 200)
    public int maxAge;

    @NotBlank
    public String name;

    @Email
    public String emailAddress;

    @Negative
    public int debt;

    @NegativeOrZero
    public int netWorth;

    @Positive
    public int income;

    @PositiveOrZero
    public int savings;

    @Future
    public LocalDate birthDate;

    @FutureOrPresent
    public LocalDate nextAppointment;

    @Past
    public LocalDate graduationDate;

    @PastOrPresent
    public LocalDate lastUpdate;

    @Null
    public Object nullField;

    @DecimalMin(value = "0.0", message = "Value must be at least 0.0")
    @DecimalMax(value = "1000000.0", message = "Value must be at most 1,000,000.0")
    private Float annualIncome;

    @Pattern(regexp = "^[A-Za-z]+$", message = "Only alphabetic characters are allowed")
    private String nickname;

    @Size(min = 0, max = 100, message = "Must contain between 0 and 100 items")
    @ElementCollection
    private List<String> otherDetails;

    @Digits(integer = 6, fraction = 2, message = "Invalid format, max 6 digits with 2 decimal places")
    private float weight;
}
