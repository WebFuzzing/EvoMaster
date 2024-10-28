package com.foo.rest.emb.json.signalserver;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * This code is taken from Signal-Server
 * G: https://github.com/signalapp/Signal-Server
 * L: MIT
 * P: signal-server/service/src/main/java/org/whispersystems/textsecuregcm/storage/Account.java
 */

@JsonFilter("Account")
public class Account {

    @JsonProperty
    private UUID uuid;

    @JsonProperty("pni")
    private UUID phoneNumberIdentifier;

    @JsonProperty
    private String number;

    public void setUuid(final UUID uuid) {
//        requireNotStale();

        this.uuid = uuid;
    }

    public UUID getPhoneNumberIdentifier() {
//        requireNotStale();

        return phoneNumberIdentifier;
    }

    public String getNumber() {
//        requireNotStale();

        return number;
    }
}
