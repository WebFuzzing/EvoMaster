package com.foo.rest.emb.json.restcountries;

/**
 * This code is taken from REST Countries
 * G: https://github.com/apilayer/restcountries
 * L: MPL-2.0
 * P: src/main/java/eu/fayder/restcountries/v2/domain/Country.java
 */
public class Country extends BaseCountry {

//    private List<Currency> currencies;
//    private List<Language> languages;
//    private Translations translations;
    private String flag;
//    private List<RegionalBloc> regionalBlocs;
    private String cioc;

//    public List<Currency> getCurrencies() {
//        return currencies;
//    }

//    public List<Language> getLanguages() {
//        return languages;
//    }

//    public Translations getTranslations() {
//        return translations;
//    }

    public String getFlag() {
        return flag;
    }

//    public List<RegionalBloc> getRegionalBlocs() {
//        return regionalBlocs;
//    }

    public String getCioc() {
        return cioc;
    }
}
