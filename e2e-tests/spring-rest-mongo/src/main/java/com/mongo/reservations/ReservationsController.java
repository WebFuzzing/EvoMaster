package com.mongo.reservations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/reservations")
public class ReservationsController {

    @Autowired
    private ReservationRepository reservations;

    @GetMapping("findAll")
    public ResponseEntity<Void> findAll() {
        if (!reservations.findAll().isEmpty()) {
            Reservation reservation = reservations.findAll().iterator().next();
            if (reservation.getCheckInDateTime() != null && reservation.getCheckOutDate() != null) {
                return ResponseEntity.status(200).build();
            }
        }
        return ResponseEntity.status(400).build();
    }
}


