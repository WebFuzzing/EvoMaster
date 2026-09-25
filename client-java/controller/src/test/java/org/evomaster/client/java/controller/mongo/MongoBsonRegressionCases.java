package org.evomaster.client.java.controller.mongo;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** BSON ordering and stored-value equality checked independently by the live oracle. */
final class MongoBsonRegressionCases {
    private MongoBsonRegressionCases() {
    }

    static Stream<MongoQueryCase> scenarios() {
        List<MongoQueryCase> cases = new ArrayList<>();
        add(cases, "ObjectId greater than supports range queries",
                "{a:{$gt:{$oid:'000000000000000000000001'}}}",
                "{a:{$oid:'000000000000000000000002'}}", true);
        add(cases, "ObjectId less than uses unsigned byte order",
                "{a:{$lt:{$oid:'800000000000000000000000'}}}",
                "{a:{$oid:'7fffffffffffffffffffffff'}}", true);
        add(cases, "ObjectId inclusive comparison accepts equality",
                "{a:{$lte:{$oid:'000000000000000000000001'}}}",
                "{a:{$oid:'000000000000000000000001'}}", true);
        add(cases, "ObjectId nonmatching range must not throw",
                "{a:{$gt:{$oid:'000000000000000000000002'}}}",
                "{a:{$oid:'000000000000000000000001'}}", false);
        add(cases, "ObjectId equality control",
                "{a:{$eq:{$oid:'000000000000000000000001'}}}",
                "{a:{$oid:'000000000000000000000001'}}", true);
        add(cases, "distinct ObjectId equality control",
                "{a:{$eq:{$oid:'000000000000000000000001'}}}",
                "{a:{$oid:'000000000000000000000002'}}", false);

        add(cases, "binary greater than supports ordered bytes",
                "{a:{$gt:{$binary:{'base64':'AQ==','subType':'00'}}}}",
                "{a:{$binary:{'base64':'Ag==','subType':'00'}}}", true);
        add(cases, "binary ordering compares length before bytes",
                "{a:{$gt:{$binary:{'base64':'/w==','subType':'00'}}}}",
                "{a:{$binary:{'base64':'AAA=','subType':'00'}}}", true);

        add(cases, "stored BSON symbol equals its string value", "{a:{$eq:'hello'}}",
                "{a:{$symbol:'hello'}}", true);
        add(cases, "different BSON symbol and string control", "{a:{$eq:'other'}}",
                "{a:{$symbol:'hello'}}", false);
        add(cases, "stored BSON JavaScript equality", "{a:{$eq:{$code:'return 1'}}}",
                "{a:{$code:'return 1'}}", true);
        add(cases, "different stored BSON JavaScript inequality control", "{a:{$ne:{$code:'return 2'}}}",
                "{a:{$code:'return 1'}}", true);
        add(cases, "stored BSON JavaScript with scope equality",
                "{a:{$eq:{$code:'return x','$scope':{x:1}}}}", "{a:{$code:'return x','$scope':{x:1}}}", true);
        add(cases, "stored BSON JavaScript different scope control",
                "{a:{$eq:{$code:'return x','$scope':{x:2}}}}", "{a:{$code:'return x','$scope':{x:1}}}", false);

        add(cases, "maximum BSON date equality control",
                "{a:{$eq:{$date:{$numberLong:'9223372036854775807'}}}}",
                "{a:{$date:{$numberLong:'9223372036854775807'}}}", true);
        add(cases, "extreme BSON date ordering control",
                "{a:{$gt:{$date:{$numberLong:'-9223372036854775808'}}}}",
                "{a:{$date:{$numberLong:'9223372036854775807'}}}", true);
        return cases.stream();
    }

    private static void add(List<MongoQueryCase> cases, String name, String query, String document, boolean matches) {
        cases.add(new MongoQueryCase(name, Document.parse(query), Document.parse(document), matches));
    }
}
