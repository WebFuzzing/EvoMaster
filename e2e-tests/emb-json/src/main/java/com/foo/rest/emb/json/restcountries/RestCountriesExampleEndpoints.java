package com.foo.rest.emb.json.restcountries;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.core.MediaType;
import java.util.List;

@RestController
@RequestMapping(path = "/api")
public class RestCountriesExampleEndpoints {

    @RequestMapping(
            value = "/json",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity parseJson(@RequestParam String name) {
        CountryService countryService = CountryService.getInstance();

        List<Country> countries = countryService.getAll();
        Country norway = countries.stream()
                .filter(country -> name.equals(country.name))
                .findAny()
                .orElse(null);

        if (norway != null) {
            return ResponseEntity.status(200).body(norway.name);
        }

        return ResponseEntity.status(204).build();

    }
}
