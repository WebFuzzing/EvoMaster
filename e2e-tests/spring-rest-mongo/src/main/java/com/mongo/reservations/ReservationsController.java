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
        int status = (reservations.findAll().isEmpty()) ? 400 : 200 ;
        return ResponseEntity.status(status).build();
    }
}


