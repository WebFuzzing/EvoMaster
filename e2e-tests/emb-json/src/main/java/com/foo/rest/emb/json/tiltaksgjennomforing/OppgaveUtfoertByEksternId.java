package com.foo.rest.emb.json.tiltaksgjennomforing;

/**
 * This code is taken from tiltaksgjennomforing-api
 * G: https://github.com/navikt/tiltaksgjennomforing-api
 * L: MIT
 * P: tiltaksgjennomforing-api/src/main/java/no/nav/tag/tiltaksgjennomforing/varsel/notifikasjon/response/oppgaveUtfoertByEksternId/OppgaveUtfoertByEksternId.java
 */
public class OppgaveUtfoertByEksternId {
    String __typename;
    String id;
    String feilmelding;

    public String get__typename() {
        return __typename;
    }

    public String getId() {
        return id;
    }

    public String getFeilmelding() {
        return feilmelding;
    }
}
