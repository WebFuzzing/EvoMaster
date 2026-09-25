package org.evomaster.client.java.controller.mongo;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Additional operator cases; the live oracle verifies every expectation before regression analysis. */
final class MongoQueryRegressionCases {
    private MongoQueryRegressionCases() {
    }

    static Stream<MongoQueryCase> scenarios() {
        List<MongoQueryCase> cases = new ArrayList<>();
        add(cases, "logical operator must retain sibling filter",
                "{$or:[{b:1},{b:2}],b:2}", "{b:1}", false);
        add(cases, "sibling filter before logical operator is valid",
                "{b:1,$or:[{b:1},{b:2}]}", "{b:1}", true);
        add(cases, "standalone logical operator control",
                "{$or:[{b:1},{b:2}]}", "{b:1}", true);

        add(cases, "elemMatch supports type operator", "{a:{$elemMatch:{$type:'number'}}}", "{a:[1,2]}", true);
        add(cases, "elemMatch supports exists operator", "{a:{$elemMatch:{$exists:true}}}", "{a:[1,2]}", true);
        add(cases, "elemMatch must not split range across nested array elements",
                "{a:{$elemMatch:{$gt:2,$lt:5}}}", "{a:[[1,6]]}", false);
        add(cases, "elemMatch scalar equality must not unwrap a nested array",
                "{a:{$elemMatch:{$eq:1}}}", "{a:[[1,2]]}", false);
        add(cases, "elemMatch negation compares the nested array itself",
                "{a:{$elemMatch:{$not:{$eq:1}}}}", "{a:[[1,2]]}", true);
        add(cases, "empty elemMatch accepts an array element", "{a:{$elemMatch:{}}}", "{a:[[]]}", true);
        add(cases, "elemMatch supports numeric array index", "{a:{$elemMatch:{'0':1}}}", "{a:[[1,2]]}", true);
        add(cases, "elemMatch scalar range control", "{a:{$elemMatch:{$gt:2,$lt:5}}}", "{a:[3,6]}", true);
        add(cases, "not equals must exclude matching nested array", "{a:{$ne:[1,2]}}", "{a:[[1,2],3]}", false);
        add(cases, "not equals different nested array control", "{a:{$ne:[1,2]}}", "{a:[[1,3],3]}", true);

        add(cases, "type query inspects immediate array elements", "{a:{$type:'int'}}", "{a:[1,2]}", true);
        add(cases, "type array still matches the array itself", "{a:{$type:'array'}}", "{a:[1,2]}", true);
        add(cases, "bitwise query accepts binary stored value", "{a:{$bitsAllSet:1}}",
                "{a:{$binary:{'base64':'AQ==','subType':'00'}}}", true);
        add(cases, "bitwise integer control", "{a:{$bitsAllSet:1}}", "{a:1}", true);
        add(cases, "modulo rejects NaN instead of converting it to zero", "{a:{$mod:[2,0]}}",
                "{a:{$numberDouble:'NaN'}}", false);
        add(cases, "modulo integer control", "{a:{$mod:[2,0]}}", "{a:4}", true);
        add(cases, "bitwise double must fit signed 64 bit range", "{a:{$bitsAllSet:1}}",
                "{a:{$numberDouble:'9.223372036854776E18'}}", false);
        add(cases, "distinct int64 values above double precision stay unequal",
                "{a:{$eq:{$numberLong:'9007199254740992'}}}", "{a:{$numberLong:'9007199254740993'}}", false);
        add(cases, "identical int64 precision control",
                "{a:{$eq:{$numberLong:'9007199254740993'}}}", "{a:{$numberLong:'9007199254740993'}}", true);
        add(cases, "timestamp seconds use unsigned ordering",
                "{a:{$gt:{$timestamp:{'t':2147483647,'i':0}}}}", "{a:{$timestamp:{'t':2147483648,'i':0}}}", true);
        add(cases, "timestamp increments remain distinct",
                "{a:{$eq:{$timestamp:{'t':1700000000,'i':0}}}}", "{a:{$timestamp:{'t':1700000000,'i':1}}}", false);

        add(cases, "document inequality does not require unequal field names",
                "{a:{$ne:{x:1}}}", "{a:{x:2}}", true);
        add(cases, "strict document greater than compares values after equal keys",
                "{a:{$gt:{x:1}}}", "{a:{x:2}}", true);
        add(cases, "strict document less than compares values after equal keys",
                "{a:{$lt:{x:2}}}", "{a:{x:1}}", true);
        add(cases, "numeric BSON representation must not skip remaining fields",
                "{a:{$eq:{x:1,y:2}}}", "{a:{x:1.0,y:99}}", false);
        add(cases, "numeric BSON representation must not skip extra fields",
                "{a:{$eq:{x:1}}}", "{a:{x:1.0,y:99}}", false);
        add(cases, "equivalent document numeric representation control",
                "{a:{$eq:{x:1,y:2}}}", "{a:{x:1.0,y:2}}", true);
        add(cases, "binary equality includes subtype",
                "{a:{$eq:{$binary:{'base64':'AQI=','subType':'00'}}}}",
                "{a:{$binary:{'base64':'AQI=','subType':'80'}}}", false);
        add(cases, "identical binary subtype control",
                "{a:{$eq:{$binary:{'base64':'AQI=','subType':'80'}}}}",
                "{a:{$binary:{'base64':'AQI=','subType':'80'}}}", true);

        add(cases, "regex ignores nonstring elements of a mixed array", "{v:{$regex:'foo'}}", "{v:[1,'foo']}", true);
        add(cases, "in regex preserves case insensitive options",
                "{v:{$in:[{$regularExpression:{'pattern':'^abc$','options':'i'}}]}}", "{v:'ABC'}", true);
        add(cases, "in regex does not stringify numeric values",
                "{v:{$in:[{$regularExpression:{'pattern':'^123$','options':''}}]}}", "{v:123}", false);
        add(cases, "in regex safely rejects null",
                "{v:{$in:[{$regularExpression:{'pattern':'foo','options':''}}]}}", "{v:null}", false);
        add(cases, "in numeric literal control", "{v:{$in:[123]}}", "{v:123}", true);
        add(cases, "all regex matches array strings",
                "{v:{$all:[{$regularExpression:{'pattern':'foo','options':''}}]}}", "{v:['foobar']}", true);
        add(cases, "all regex matches a scalar string",
                "{v:{$all:[{$regularExpression:{'pattern':'foo','options':''}}]}}", "{v:'foobar'}", true);
        add(cases, "extended regex preserves space inside character class",
                "{v:{$regex:'^[ a]$',$options:'x'}}", "{v:' '}", true);
        add(cases, "default regex dot matches carriage return", "{v:{$regex:'^.$'}}", "{v:'\\r'}", true);
        add(cases, "default regex end anchor does not ignore carriage return",
                "{v:{$regex:'^foo$'}}", "{v:'foo\\r'}", false);
        add(cases, "literal regex query can match stored regex",
                "{v:{$regularExpression:{'pattern':'foo','options':'i'}}}",
                "{v:{$regularExpression:{'pattern':'foo','options':'i'}}}", true);
        add(cases, "stored regex equality includes options",
                "{v:{$eq:{$regularExpression:{'pattern':'foo','options':''}}}}",
                "{v:{$regularExpression:{'pattern':'foo','options':'i'}}}", false);
        add(cases, "BSON string ordering follows UTF8 rather than UTF16",
                "{v:{$gt:'\\uE000'}}", "{v:'\\uD800\\uDC00'}", true);

        cases.add(new MongoQueryCase("legacy near accepts legacy stored point",
                Document.parse("{loc:{$near:[0,0],$maxDistance:1}}"), Document.parse("{loc:[0,0]}"), true,
                new Document("loc", "2d")));
        cases.add(new MongoQueryCase("legacy near excludes distant point control",
                Document.parse("{loc:{$near:[0,0],$maxDistance:1}}"), Document.parse("{loc:[2,2]}"), false,
                new Document("loc", "2d")));

        add(cases, "implicit empty document equality", "{a:{}}", "{a:{}}", true);
        add(cases, "explicit empty document equality control", "{a:{$eq:{}}}", "{a:{}}", true);
        add(cases, "literal plural comments field must be preserved",
                "{a:{$eq:{$comments:'x',v:1}}}", "{a:{$comments:'x',v:1}}", true);
        add(cases, "removing literal plural comments changes equality",
                "{a:{$eq:{$comments:'x',v:1}}}", "{a:{v:1}}", false);

        add(cases, "numeric high bits are sign extended for negative values",
                "{a:{$bitsAllSet:[64]}}", "{a:-1}", true);
        add(cases, "numeric high bits are clear for positive values",
                "{a:{$bitsAllClear:[64]}}", "{a:1}", true);
        add(cases, "positive numeric high bit is not set", "{a:{$bitsAllSet:[64]}}", "{a:1}", false);
        add(cases, "negative numeric high bit is not clear", "{a:{$bitsAllClear:[64]}}", "{a:-1}", false);
        add(cases, "MinKey equality", "{a:{$minKey:1}}", "{a:{$minKey:1}}", true);
        add(cases, "MaxKey sorts above an ordinary number", "{a:{$lt:{$maxKey:1}}}", "{a:1}", true);
        add(cases, "MinKey and MaxKey remain distinct", "{a:{$eq:{$minKey:1}}}", "{a:{$maxKey:1}}", false);
        add(cases, "bitwise decimal integrality must not be rounded through double",
                "{a:{$bitsAllSet:1}}", "{a:{$numberDecimal:'1.0000000000000000000000001'}}", false);
        add(cases, "integral decimal bitwise control", "{a:{$bitsAllSet:1}}", "{a:{$numberDecimal:'1'}}", true);

        add(cases, "size accepts an integral double", "{a:{$size:2.0}}", "{a:[1,2]}", true);
        add(cases, "size accepts an integral Int64", "{a:{$size:{$numberLong:'2'}}}", "{a:[1,2]}", true);
        add(cases, "size accepts an integral decimal", "{a:{$size:{$numberDecimal:'2'}}}", "{a:[1,2]}", true);
        add(cases, "size Int32 control", "{a:{$size:2}}", "{a:[1,2]}", true);
        add(cases, "fixture preserves the type of a small Int64", "{a:{$type:'long'}}",
                "{a:{$numberLong:'2'}}", true);

        add(cases, "array greater than uses array ordering", "{a:{$gt:[1]}}", "{a:[2]}", true);
        add(cases, "array less than uses array ordering", "{a:{$lt:[2]}}", "{a:[1]}", true);
        add(cases, "nested array ordering does not throw", "{a:{$gt:[1]}}", "{a:[[2]]}", true);
        add(cases, "empty array satisfies inclusive array comparison", "{a:{$lte:[]}}", "{a:[]}", true);
        add(cases, "array greater than negative control", "{a:{$gt:[2]}}", "{a:[1]}", false);
        return cases.stream();
    }

    private static void add(List<MongoQueryCase> cases, String name, String query, String document, boolean matches) {
        cases.add(new MongoQueryCase(name, Document.parse(query), Document.parse(document), matches));
    }
}
