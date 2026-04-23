package com.foo.spring.rest.mongo;

import com.mongo.reservations.ReservationsApp;

public class ReservationsAppController extends MongoController {
    public ReservationsAppController() {
        super("reservations", ReservationsApp.class);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongo.reservations";
    }
}
