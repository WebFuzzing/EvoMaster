package com.foo.rest.emb.json.restcountries;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This code is taken from REST Countries
 * G: https://github.com/apilayer/restcountries
 * L: MPL-2.0
 * P: src/main/java/eu/fayder/restcountries/rest/CountryServiceBase.java
 */
public class CountryServiceBase {

//    private static final Logger LOG = Logger.getLogger(CountryServiceBase.class);
    private static final SimpleLogger LOG = new SimpleLogger();

    protected List<? extends BaseCountry> loadJson(String filename, Class<? extends BaseCountry> clazz) {
        LOG.debug("Loading JSON " + filename);
        List<BaseCountry> countries = new ArrayList<>();
        InputStream is = CountryServiceBase.class.getClassLoader().getResourceAsStream(filename);
        Gson gson = new Gson();
        JsonReader reader;
        try {
            reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
            reader.beginArray();
            while(reader.hasNext()) {
                BaseCountry country = gson.fromJson(reader, clazz);
                countries.add(country);
            }
        } catch (Exception e) {
            LOG.error("Could not load JSON " + filename);
        }
        return countries;
    }
}
