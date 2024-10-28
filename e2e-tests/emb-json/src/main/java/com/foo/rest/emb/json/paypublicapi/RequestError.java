package com.foo.rest.emb.json.paypublicapi;

import io.swagger.v3.oas.annotations.media.Schema;

import static com.google.common.collect.ObjectArrays.concat;
import static java.lang.String.format;

public class RequestError {

    public enum Code {

        CREATE_PAYMENT_ACCOUNT_ERROR("P0199", "There is an error with this account. Contact support with your error code - https://www.payments.service.gov.uk/support/ ."),
        CREATE_PAYMENT_CONNECTOR_ERROR("P0198", "Downstream system error"),
        CREATE_PAYMENT_PARSING_ERROR("P0197", "Unable to parse JSON"),
        CREATE_PAYMENT_MOTO_NOT_ENABLED("P0196", "MOTO payments are not enabled for this account. Please contact support if you would like to process MOTO payments - https://www.payments.service.gov.uk/support/ ."),
        CREATE_PAYMENT_AUTHORISATION_API_NOT_ENABLED("P0195","Using authorisation_mode of moto_api is not allowed for this account"),
        CREATE_PAYMENT_AGREEMENT_ID_ERROR("P0103", "Invalid attribute value: set_up_agreement. Agreement ID does not exist"),
        CREATE_PAYMENT_CARD_NUMBER_IN_PAYMENT_LINK_REFERENCE_ERROR("P0105", "%s"),

        GENERIC_MISSING_FIELD_ERROR_MESSAGE_FROM_CONNECTOR("P0101", "%s"),
        GENERIC_VALIDATION_EXCEPTION_MESSAGE_FROM_CONNECTOR("P0102", "%s"),
        GENERIC_UNEXPECTED_FIELD_ERROR_MESSAGE_FROM_CONNECTOR("P0104", "%s"),

        CREATE_PAYMENT_MISSING_FIELD_ERROR("P0101", "Missing mandatory attribute: %s"),
        CREATE_PAYMENT_UNEXPECTED_FIELD_ERROR("P0104", "Unexpected attribute: %s"),
        CREATE_PAYMENT_VALIDATION_ERROR("P0102", "Invalid attribute value: %s. %s"),
        CREATE_PAYMENT_HEADER_VALIDATION_ERROR("P0102", "%s"),
        CREATE_PAYMENT_IDEMPOTENCY_KEY_ALREADY_USED("P0191", "The `Idempotency-Key` you sent in the request header has already been used to create a payment."),

        GET_PAYMENT_NOT_FOUND_ERROR("P0200", "Not found"),
        GET_PAYMENT_CONNECTOR_ERROR("P0298", "Downstream system error"),

        GET_PAYMENT_EVENTS_NOT_FOUND_ERROR("P0300", "Not found"),
        GET_PAYMENT_EVENTS_CONNECTOR_ERROR("P0398", "Downstream system error"),

        SEARCH_PAYMENTS_VALIDATION_ERROR("P0401", "Invalid parameters: %s. See Public API documentation for the correct data formats"),
        SEARCH_PAYMENTS_NOT_FOUND("P0402", "Page not found"),
        SEARCH_PAYMENTS_CONNECTOR_ERROR("P0498", "Downstream system error"),

        SEARCH_AGREEMENTS_VALIDATION_ERROR("P2401", "Invalid parameters: %s. See Public API documentation for the correct data formats"),
        SEARCH_AGREEMENTS_NOT_FOUND("P2402", "Page not found"),
        SEARCH_AGREEMENTS_LEDGER_ERROR("P2498", "Downstream system error"),

        CANCEL_PAYMENT_NOT_FOUND_ERROR("P0500", "Not found"),
        CANCEL_PAYMENT_CONNECTOR_BAD_REQUEST_ERROR("P0501", "Cancellation of payment failed"),
        CANCEL_PAYMENT_CONNECTOR_CONFLICT_ERROR("P0502", "Cancellation of payment failed"),
        CANCEL_PAYMENT_CONNECTOR_ERROR("P0598", "Downstream system error"),

        CAPTURE_PAYMENT_NOT_FOUND_ERROR("P1000", "Not found"),
        CAPTURE_PAYMENT_CONNECTOR_BAD_REQUEST_ERROR("P1001", "Capture of payment failed"),
        CAPTURE_PAYMENT_CONNECTOR_CONFLICT_ERROR("P1003", "Payment cannot be captured"),
        CAPTURE_PAYMENT_CONNECTOR_ERROR("P1098", "Downstream system error"),

        CREATE_PAYMENT_REFUND_CONNECTOR_ERROR("P0698", "Downstream system error"),
        CREATE_PAYMENT_REFUND_PARSING_ERROR("P0697", "Unable to parse JSON"),
        CREATE_PAYMENT_REFUND_NOT_FOUND_ERROR("P0600", "Not found"),
        CREATE_PAYMENT_REFUND_MISSING_FIELD_ERROR("P0601", "Missing mandatory attribute: %s"),
        CREATE_PAYMENT_REFUND_VALIDATION_ERROR("P0602", "Invalid attribute value: %s. %s"),
        CREATE_PAYMENT_REFUND_NOT_AVAILABLE("P0603", "The payment is not available for refund. Payment refund status: %s"),
        CREATE_PAYMENT_REFUND_NOT_AVAILABLE_DUE_TO_DISPUTE("P0603", "The payment is disputed and cannot be refunded"),
        CREATE_PAYMENT_REFUND_AMOUNT_AVAILABLE_MISMATCH("P0604", "Refund amount available mismatch."),

        GET_PAYMENT_REFUND_NOT_FOUND_ERROR("P0700", "Not found"),
        GET_PAYMENT_REFUND_CONNECTOR_ERROR("P0798", "Downstream system error"),

        GET_PAYMENT_REFUNDS_NOT_FOUND_ERROR("P0800", "Not found"),
        GET_PAYMENT_REFUNDS_CONNECTOR_ERROR("P0898", "Downstream system error"),

        TOO_MANY_REQUESTS_ERROR("P0900", "Too many requests"),
        REQUEST_DENIED_ERROR("P0920", "Request blocked by security rules. Please consult API documentation for more information."),
        RESOURCE_ACCESS_FORBIDDEN("P0930", "Access to this resource is not enabled for this account. Contact support with your error code - https://www.payments.service.gov.uk/support/ ."),
        ACCOUNT_NOT_LINKED_WITH_PSP("P0940", "Account is not fully configured. Please refer to documentation to setup your account or contact support with your error code - https://www.payments.service.gov.uk/support/ ."),
        ACCOUNT_DISABLED("P0941", "GOV.UK Pay has disabled payment and refund creation on this account. Contact support with your error code - https://www.payments.service.gov.uk/support/ ."),
        RECURRING_CARD_PAYMENTS_NOT_ALLOWED_ERROR("P0942", "Recurring card payments are currently disabled for this service. Contact support with your error code - https://www.payments.service.gov.uk/support/"),

        SEARCH_REFUNDS_VALIDATION_ERROR("P1101", "Invalid parameters: %s. See Public API documentation for the correct data formats"),
        SEARCH_REFUNDS_NOT_FOUND("P1100", "Page not found"),
        SEARCH_REFUNDS_CONNECTOR_ERROR("P1898", "Downstream system error"),

        AUTHORISATION_CARD_NUMBER_REJECTED_ERROR("P0010", "%s"),
        AUTHORISATION_REJECTED_ERROR("P0010", "%s"),
        AUTHORISATION_ERROR("P0050", "%s"),
        AUTHORISATION_TIMEOUT_ERROR("P0050", "%s"),
        AUTHORISATION_ONE_TIME_TOKEN_ALREADY_USED_ERROR("P1212", "%s"),
        AUTHORISATION_ONE_TIME_TOKEN_INVALID_ERROR("P1211", "%s"),

        CREATE_AGREEMENT_CONNECTOR_ERROR("P2198", "Downstream system error"),
        CREATE_AGREEMENT_PARSING_ERROR("P2197", "Unable to parse JSON"),

        CREATE_AGREEMENT_MISSING_FIELD_ERROR("P2101", "Missing mandatory attribute: %s"),
        CREATE_AGREEMENT_VALIDATION_ERROR("P2102", "Invalid attribute value: %s. %s"),
        GET_AGREEMENT_NOT_FOUND_ERROR("P2200", "Not found"),
        GET_AGREEMENT_LEDGER_ERROR("P2298", "Downstream system error"),

        CANCEL_AGREEMENT_NOT_FOUND_ERROR("P2500", "Not found"),
        CANCEL_AGREEMENT_CONNECTOR_BAD_REQUEST_ERROR("P2501", "Cancellation of agreement failed"),
        CANCEL_AGREEMENT_CONNECTOR_ERROR("P2598", "Downstream system error"),

        SEARCH_DISPUTES_VALIDATION_ERROR("P0401", "Invalid parameters: %s. See Public API documentation for the correct data formats"),
        GET_DISPUTE_LEDGER_ERROR("P0498", "Downstream system error"),
        SEARCH_DISPUTES_NOT_FOUND("P0402", "Page not found");

        private String value;
        private String format;

        Code(String value, String format) {
            this.value = value;
            this.format = format;
        }

        public String value() {
            return value;
        }

        public String getFormat() {
            return format;
        }
    }

    private String field;
    private String header;
    private final Code code;
    private final String description;

    public static RequestError aRequestError(Code code, Object... parameters) {
        return new RequestError(code, parameters);
    }

    public static RequestError aRequestError(String fieldName, Code code, Object... parameters) {
        return new RequestError(null, fieldName, code, parameters);
    }

    public static RequestError aHeaderRequestError(String header, Code code, Object... parameters) {
        return new RequestError(header, null, code, parameters);
    }

    private RequestError(Code code, Object... parameters) {
        this.code = code;
        this.description = format(code.getFormat(), parameters);
    }

    private RequestError(String header, String fieldName, Code code, Object... parameters) {
        this.header = header;
        this.field = fieldName;
        this.code = code;
        this.description = format(code.getFormat(), fieldName != null ? concat(fieldName, parameters) : parameters);
    }

    @Schema(example = "amount", description = "The parameter in your request that's causing the error.")
    public String getField() {
        return field;
    }

    @Schema(example = "Idempotency-Key", description = "The header in your request that's causing the error.")
    public String getHeader() {
        return header;
    }

    @Schema(example = "P0102",
            description = "An [API error code](https://docs.payments.service.gov.uk/api_reference/#gov-uk-pay-api-error-codes)" +
                    "that explains why the payment failed.<br><br>`code` only appears if the payment failed.")
    public String getCode() {
        return code.value();
    }

    @Schema(example = "Invalid attribute value: amount. Must be less than or equal to 10000000",
            description = "Additional details about the error.")
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "RequestError{" +
                "field=" + field +
                ", code=" + code.value() +
                ", name=" + code +
                ", description='" + description + '\'' +
                '}';
    }
}
