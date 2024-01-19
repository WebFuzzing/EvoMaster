package com.mongo.reservations;

import org.springframework.data.annotation.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Reservation {
    @Id
    public String id;

    public LocalDateTime getCheckInDateTime() {
        return checkInDateTime;
    }


    public LocalDateTime checkInDateTime;

//    public LocalDate checkOutDate;

//    public void setCheckOutDate(LocalDate checkOutDate) {
//        this.checkOutDate = checkOutDate;
//    }
//
//    public LocalDate getCheckOutDate() {
//        return checkOutDate;
//    }

//
//    public LocalTime checkOutTime;

//    public void setCheckOutTime(LocalTime checkOutTime) {
//        this.checkOutTime = checkOutTime;
//    }
//
//    public LocalTime getCheckOutTime() {
//        return checkOutTime;
//    }

    public void setCheckInDateTime(LocalDateTime checkInDateTime) {
        this.checkInDateTime = checkInDateTime;
    }

}

