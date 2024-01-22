package com.mongo.reservations;

import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reservation {
    @Id
    public String id;

    public LocalDateTime getCheckInDateTime() {
        return checkInDateTime;
    }


    @NotNull
    public LocalDateTime checkInDateTime;

    @NotNull
    public LocalDate checkOutDate;

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckInDateTime(LocalDateTime checkInDateTime) {
        this.checkInDateTime = checkInDateTime;
    }

}

