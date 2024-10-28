package com.foo.rest.emb.json.signalserver;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

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
