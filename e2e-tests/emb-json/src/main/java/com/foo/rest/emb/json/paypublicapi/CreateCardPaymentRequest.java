package com.foo.rest.emb.json.paypublicapi;

/**
 * This code is taken from pay-publicapi
 * G: https://github.com/alphagov/pay-publicapi
 * L: MIT
 * P: src/main/java/uk/gov/pay/api/model/CreateCardPaymentRequest.java
 */
public class CreateCardPaymentRequest {
    public static final int EMAIL_MAX_LENGTH = 254;
    public static final String AMOUNT_FIELD_NAME = "amount";
    public static final String REFERENCE_FIELD_NAME = "reference";
    public static final String DESCRIPTION_FIELD_NAME = "description";
    public static final String LANGUAGE_FIELD_NAME = "language";
    public static final String EMAIL_FIELD_NAME = "email";
    public static final int REFERENCE_MAX_LENGTH = 255;
    public static final int AMOUNT_MAX_VALUE = 10000000;
    public static final int AMOUNT_MIN_VALUE = 0;
    public static final int DESCRIPTION_MAX_LENGTH = 255;
    public static final String RETURN_URL_FIELD_NAME = "return_url";
    public static final int URL_MAX_LENGTH = 2000;
    public static final String PREFILLED_CARDHOLDER_DETAILS_FIELD_NAME = "prefilled_cardholder_details";
    public static final String PREFILLED_CARDHOLDER_NAME_FIELD_NAME = "cardholder_name";
    public static final String PREFILLED_BILLING_ADDRESS_FIELD_NAME = "billing_address";
    public static final String PREFILLED_ADDRESS_LINE1_FIELD_NAME = "line1";
    public static final String PREFILLED_ADDRESS_LINE2_FIELD_NAME = "line2";
    public static final String PREFILLED_ADDRESS_CITY_FIELD_NAME = "city";
    public static final String PREFILLED_ADDRESS_POSTCODE_FIELD_NAME = "postcode";
    public static final String PREFILLED_ADDRESS_COUNTRY_FIELD_NAME = "country";
    public static final String DELAYED_CAPTURE_FIELD_NAME = "delayed_capture";
    public static final String MOTO_FIELD_NAME = "moto";
    public static final String SET_UP_AGREEMENT_FIELD_NAME = "set_up_agreement";
    public static final String AGREEMENT_ID_FIELD_NAME = "agreement_id";
    public static final String SOURCE_FIELD_NAME = "source";
    public static final String METADATA = "metadata";
    public static final String INTERNAL = "internal";
    public static final String AUTHORISATION_MODE = "authorisation_mode";
    private static final String PREFILLED_CARDHOLDER_DETAILS = "prefilled_cardholder_details";
    private static final String BILLING_ADDRESS = "billing_address";
}
