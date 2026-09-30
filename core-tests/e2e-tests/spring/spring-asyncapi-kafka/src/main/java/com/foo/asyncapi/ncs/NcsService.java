package com.foo.asyncapi.ncs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.foo.asyncapi.ncs.imp.Bessj;
import com.foo.asyncapi.ncs.imp.Expint;
import com.foo.asyncapi.ncs.imp.Fisher;
import com.foo.asyncapi.ncs.imp.Gammq;
import com.foo.asyncapi.ncs.imp.Remainder;
import com.foo.asyncapi.ncs.imp.TriangleClassification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * The NCS operations, keyed by the topic their requests arrive on. Each answers with a result
 * message or, for the inputs the REST version answers with a 400, an error message.
 *
 * The checks mirror {@code NcsRest} in EMB: bessj rejects an order outside 3..1000, fisher
 * degrees of freedom above 1000, remainder operands beyond 10000 in either direction, and
 * expint and gammq whatever their routines throw on. A request that is not the JSON the
 * contract describes is an error too, as it would be over HTTP.
 */
@Service
public class NcsService {

    /**
     * What goes back: on which topic, and the JSON body.
     */
    public static final class Reply {

        public final String topic;

        public final String body;

        Reply(String topic, String body) {
            this.topic = topic;
            this.body = body;
        }
    }

    static final String TRIANGLE_REQUEST = "ncs.triangle.request";
    static final String TRIANGLE_REPLY = "ncs.triangle.reply";
    static final String BESSJ_REQUEST = "ncs.bessj.request";
    static final String BESSJ_REPLY = "ncs.bessj.reply";
    static final String EXPINT_REQUEST = "ncs.expint.request";
    static final String EXPINT_REPLY = "ncs.expint.reply";
    static final String FISHER_REQUEST = "ncs.fisher.request";
    static final String FISHER_REPLY = "ncs.fisher.reply";
    static final String GAMMQ_REQUEST = "ncs.gammq.request";
    static final String GAMMQ_REPLY = "ncs.gammq.reply";
    static final String REMAINDER_REQUEST = "ncs.remainder.request";
    static final String REMAINDER_REPLY = "ncs.remainder.reply";

    private static final String RESULT_AS_INT = "resultAsInt";
    private static final String RESULT_AS_DOUBLE = "resultAsDouble";
    private static final String ERROR = "error";
    private static final String CODE = "code";
    private static final String MESSAGE = "message";

    private static final int BAD_REQUEST = 400;

    private static final int REMAINDER_LIMIT = 10_000;
    private static final int MAX_DEGREES_OF_FREEDOM = 1000;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * A request the service could not use, answered as the REST version answers with a 400.
     */
    private static final class Rejected extends RuntimeException {
        Rejected(String message) {
            super(message);
        }
    }

    public List<String> requestTopics() {
        return Arrays.asList(
                TRIANGLE_REQUEST, BESSJ_REQUEST, EXPINT_REQUEST, FISHER_REQUEST, GAMMQ_REQUEST, REMAINDER_REQUEST);
    }

    public Reply handle(String requestTopic, String json) {

        String replyTopic = replyTopicOf(requestTopic);

        try {
            JsonNode request = parse(json);
            return new Reply(replyTopic, compute(requestTopic, request));
        } catch (Rejected e) {
            return new Reply(replyTopic, error(e.getMessage()));
        } catch (RuntimeException e) {
            //what the numerical routines throw on inputs they cannot handle
            return new Reply(replyTopic, error(e.getMessage()));
        }
    }

    private String compute(String requestTopic, JsonNode request) {

        switch (requestTopic) {

            case TRIANGLE_REQUEST:
                return intResult(TriangleClassification.classify(integer(request, "a"), integer(request, "b"), integer(request, "c")));

            case BESSJ_REQUEST: {
                int n = integer(request, "n");
                if (n <= 2 || n > MAX_DEGREES_OF_FREEDOM) {
                    throw new Rejected("n must be in 3..1000");
                }
                return doubleResult(new Bessj().bessj(n, number(request, "x")));
            }

            case EXPINT_REQUEST:
                return doubleResult(Expint.exe(integer(request, "n"), number(request, "x")));

            case FISHER_REQUEST: {
                int m = integer(request, "m");
                int n = integer(request, "n");
                if (m > MAX_DEGREES_OF_FREEDOM || n > MAX_DEGREES_OF_FREEDOM) {
                    throw new Rejected("m and n must not exceed 1000");
                }
                return doubleResult(Fisher.exe(m, n, number(request, "x")));
            }

            case GAMMQ_REQUEST:
                return doubleResult(new Gammq().exe(number(request, "a"), number(request, "x")));

            case REMAINDER_REQUEST: {
                int a = integer(request, "a");
                int b = integer(request, "b");
                if (a > REMAINDER_LIMIT || a < -REMAINDER_LIMIT || b > REMAINDER_LIMIT || b < -REMAINDER_LIMIT) {
                    throw new Rejected("a and b must be within -10000..10000");
                }
                return intResult(Remainder.exe(a, b));
            }

            default:
                throw new IllegalArgumentException("Not a request topic: " + requestTopic);
        }
    }

    private String replyTopicOf(String requestTopic) {
        switch (requestTopic) {
            case TRIANGLE_REQUEST: return TRIANGLE_REPLY;
            case BESSJ_REQUEST: return BESSJ_REPLY;
            case EXPINT_REQUEST: return EXPINT_REPLY;
            case FISHER_REQUEST: return FISHER_REPLY;
            case GAMMQ_REQUEST: return GAMMQ_REPLY;
            case REMAINDER_REQUEST: return REMAINDER_REPLY;
            default: throw new IllegalArgumentException("Not a request topic: " + requestTopic);
        }
    }

    private JsonNode parse(String json) {
        try {
            JsonNode node = json == null ? null : mapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new Rejected("the request must be a JSON object");
            }
            return node;
        } catch (IOException e) {
            throw new Rejected("the request is not JSON: " + e.getMessage());
        }
    }

    private static int integer(JsonNode request, String field) {
        JsonNode value = request.get(field);
        if (value == null || !value.canConvertToInt()) {
            throw new Rejected("'" + field + "' must be an integer");
        }
        return value.intValue();
    }

    private static double number(JsonNode request, String field) {
        JsonNode value = request.get(field);
        if (value == null || !value.isNumber()) {
            throw new Rejected("'" + field + "' must be a number");
        }
        return value.doubleValue();
    }

    private String intResult(int value) {
        return mapper.createObjectNode().put(RESULT_AS_INT, value).toString();
    }

    private String doubleResult(double value) {
        /*
            JSON has no NaN or infinity. Rather than emit a token no reader accepts, or a string
            where the contract promises a number, a result the routine could not compute is an
            error, as the request was one the service cannot serve.
         */
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new Rejected("the result is not a finite number");
        }
        return mapper.createObjectNode().put(RESULT_AS_DOUBLE, value).toString();
    }

    private String error(String message) {
        ObjectNode error = mapper.createObjectNode();
        error.put(CODE, BAD_REQUEST);
        error.put(MESSAGE, message == null ? "rejected" : message);
        return mapper.createObjectNode().set(ERROR, error).toString();
    }
}
