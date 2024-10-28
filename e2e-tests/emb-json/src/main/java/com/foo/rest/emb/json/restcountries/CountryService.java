package com.foo.rest.emb.json.restcountries;

import java.util.List;

/**
 * This code is taken from REST Countries
 * G: https://github.com/apilayer/restcountries
 * L: MPL-2.0
 * P: src/main/java/eu/fayder/restcountries/v2/rest/CountryService.java
 */
public class CountryService extends CountryServiceBase {
    private static List<Country> countries;


    private CountryService() {
        initialize();
    }

    private static class InstanceHolder {
        private static final CountryService INSTANCE = new CountryService();
    }

    public static CountryService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public List<Country> getAll() {
        return countries;
    }

    private void initialize() {
        countries = (List<Country>) super.loadJson("countriesV2.json", Country.class);
    }
}
