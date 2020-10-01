package org.evomaster.protobuf;


import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProtoParserTest {

    @Test
    public void testParserHelloWorld() {

        Location location = Location.get("file.proto");
        String data = "syntax = \"proto3\";\n" +
                "\n" +
                "option java_multiple_files = true;\n" +
                "option java_package = \"io.grpc.examples.helloworld\";\n" +
                "option java_outer_classname = \"HelloWorldProto\";\n" +
                "option objc_class_prefix = \"HLW\";\n" +
                "\n" +
                "package helloworld;\n" +
                "\n" +
                "// The greeting service definition.\n" +
                "service Greeter {\n" +
                "  // Sends a greeting\n" +
                "  rpc SayHello (HelloRequest) returns (HelloReply) {}\n" +
                "}\n" +
                "\n" +
                "// The request message containing the user's name.\n" +
                "message HelloRequest {\n" +
                "  string name = 1;\n" +
                "}\n" +
                "\n" +
                "// The response message containing the greetings\n" +
                "message HelloReply {\n" +
                "  string message = 1;\n" +
                "}";
        ProtoFileElement parsedFile = ProtoParser.Companion.parse(location, data);

        assertEquals("helloworld", parsedFile.getPackageName());
        assertEquals(2, parsedFile.getTypes().size());
        assertEquals(1, parsedFile.getServices().size());

        ServiceElement service = parsedFile.getServices().get(0);
        assertEquals("Greeter", service.getName());
        assertEquals(1, service.getRpcs().size());

        RpcElement rpc = service.getRpcs().get(0);
        assertEquals("SayHello", rpc.getName());

        String requestType = rpc.getRequestType();
        assertEquals("HelloRequest", requestType);

        String responseType = rpc.getResponseType();
        assertEquals("HelloReply", responseType);
        {
            assertTrue(
                    parsedFile.getTypes()
                            .stream()
                            .anyMatch(typeElement -> typeElement.getName().equals("HelloReply")));

            parsedFile.getTypes()
                    .stream()
                    .filter(typeElement -> typeElement.getName().equals("HelloReply"))
                    .findFirst()
                    .ifPresent(
                            helloReply -> {
                                assertEquals("HelloReply", helloReply.getName());
                                assertEquals(0, helloReply.getOptions().size());
                                assertEquals(0, helloReply.getNestedTypes().size());

                                assertTrue(helloReply instanceof MessageElement);
                                MessageElement helloReplyAsMessageElement = (MessageElement) helloReply;

                                assertEquals(1, helloReplyAsMessageElement.getFields().size());
                                FieldElement messageFileld = helloReplyAsMessageElement.getFields().get(0);


                                assertEquals("message", messageFileld.getName());
                                assertEquals("string", messageFileld.getType());
                            }
                    );

        }
        {
            assertTrue(
                    parsedFile.getTypes()
                            .stream()
                            .anyMatch(typeElement -> typeElement.getName().equals("HelloRequest")));

            parsedFile.getTypes()
                    .stream()
                    .filter(typeElement -> typeElement.getName().equals("HelloRequest"))
                    .findFirst().ifPresent(
                    helloRequest -> {
                        assertEquals("HelloRequest", helloRequest.getName());
                        assertEquals(0, helloRequest.getOptions().size());
                        assertEquals(0, helloRequest.getNestedTypes().size());

                        assertTrue(helloRequest instanceof MessageElement);
                        MessageElement helloRequestAsMessageElement = (MessageElement) helloRequest;

                        assertEquals(1, helloRequestAsMessageElement.getFields().size());
                        FieldElement nameFileld = helloRequestAsMessageElement.getFields().get(0);


                        assertEquals("name", nameFileld.getName());
                        assertEquals("string", nameFileld.getType());

                    }
            );
        }
    }

    @Test
    public void testParserInd1() {
        Location location = Location.get("file.proto");
        String data = "syntax = \"proto3\";\n" +
                "\n" +
                "option java_multiple_files = true;\n" +
                "option java_package = \"com.company\";\n" +
                "option java_outer_classname = \"Ind_1Proto\";\n" +
                "option objc_class_prefix = \"Ind_1\";\n" +
                "\n" +
                "package com.company;\n" +
                "\n" +
                "message FieldNameA {}\n" +
                "\n" +
                "message FieldNameB {}\n" +
                "\n" +
                "message FieldE {}\n" +
                "\n" +
                "message FieldF {}\n" +
                "\n" +
                "\n" +
                "message A {\n" +
                "    com.company.FieldNameB FieldB = 1;\n" +
                "}\n" +
                "\n" +
                "message B {\n" +
                "    com.company.FieldNameA FieldA = 1;\n" +
                "    A FieldX = 2;\n" +
                "}\n" +
                "\n" +
                "message C {\n" +
                "    repeated com.company.FieldNameB FieldB = 1;\n" +
                "}\n" +
                "\n" +
                "message D {\n" +
                "    com.company.FieldNameA FieldA = 1;\n" +
                "    C FieldX = 2;\n" +
                "}\n" +
                "\n" +
                "message E {\n" +
                "    com.company.FieldF FieldF = 1;\n" +
                "    com.company.FieldE FieldE = 2;\n" +
                "}\n" +
                "\n" +
                "message F {\n" +
                "    com.company.FieldNameA FieldA = 1;\n" +
                "    E FieldX = 2;\n" +
                "}\n" +
                "\n" +
                "message G {\n" +
                "    com.company.FieldF FieldF = 1;\n" +
                "    com.company.FieldE FieldE = 2;\n" +
                "}\n" +
                "\n" +
                "message H {\n" +
                "    com.company.FieldNameA FieldA = 1;\n" +
                "    G FieldX = 2;\n" +
                "}\n" +
                "\n" +
                "/***************************************************************\n" +
                "RESPONSE messages\n" +
                "****************************************************************/\n" +
                "\n" +
                "message BResponse {\n" +
                "    com.company.FieldNameA FieldA = 1;\n" +
                "    A FieldName = 2;\n" +
                "}\n" +
                "\n" +
                "message I {\n" +
                "    string Field1 = 1;\n" +
                "    bool Field2 = 2;\n" +
                "    string Field3 = 3;\n" +
                "}\n" +
                "\n" +
                "message IResult {\n" +
                "    repeated I FieldName = 1;\n" +
                "}\n" +
                "\n" +
                "message DResponse {\n" +
                "    com.company.FieldNameA FieldA = 1;\n" +
                "    IResult FieldName = 2;\n" +
                "}\n" +
                "\n" +
                "message FResult {\n" +
                "    com.company.FieldNameB FieldB = 1;\n" +
                "}\n" +
                "\n" +
                "message FResponse {\n" +
                "    com.company.FieldNameA FieldA = 1;\n" +
                "    FResult FieldName = 2;\n" +
                "}\n" +
                "\n" +
                "message HResult {\n" +
                "    com.company.FieldF FieldF = 1;\n" +
                "    repeated I FieldName = 2;\n" +
                "}\n" +
                "\n" +
                "message HResponse {\n" +
                "    com.company.FieldNameA FieldA = 1;\n" +
                "    HResult FieldName = 2;\n" +
                "}\n" +
                "\n" +
                "service Ind_1 {\n" +
                "    rpc EndpointA(B)\n" +
                "        returns (BResponse) {}\n" +
                "    \n" +
                "    rpc EndpointB(F)\n" +
                "        returns (FResponse) {}\n" +
                "    \n" +
                "    rpc EndpointC(D)\n" +
                "        returns (DResponse) {}\n" +
                "    \n" +
                "    rpc EndpointD(H)\n" +
                "        returns (HResponse) {}\n" +
                "}";
        ProtoFileElement parsedFile = ProtoParser.Companion.parse(location, data);

        assertEquals("com.company", parsedFile.getPackageName());
        assertEquals(20, parsedFile.getTypes().size());
        assertEquals(1, parsedFile.getServices().size());


        ServiceElement service = parsedFile.getServices().get(0);
        assertEquals("Ind_1", service.getName());
        assertEquals(4, service.getRpcs().size());


        // Endpoints
        checkEndpointA(service);
        checkEndpointB(service);
        checkEndpointC(service);
        checkEndpointD(service);

        // Request messages
        checkFieldNameAMessage(parsedFile);
        checkFieldNameBMessage(parsedFile);
        checkFieldEMessage(parsedFile);
        checkFieldFMessage(parsedFile);

        // Response messages
        checkHResponseMessage(parsedFile);
        checkHResultMessage(parsedFile);
        checkFResponseMessage(parsedFile);
        checkFResultMessage(parsedFile);
        checkDResponseMessage(parsedFile);
        checkIResultMessage(parsedFile);
        checkIMessage(parsedFile);
        checkBResponseMessage(parsedFile);

    }

    private void checkIResultMessage(ProtoFileElement parsedFile) {
        // message DResponse
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("IResult"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("IResult"))
                .findFirst().ifPresent(
                iResult -> {
                    assertTrue(iResult instanceof MessageElement);
                    MessageElement iResultAsMessageElement = (MessageElement) iResult;

                    assertEquals(1, iResultAsMessageElement.getFields().size());

                    FieldElement fieldName = iResultAsMessageElement
                            .getFields().get(0);

                    assertEquals("FieldName", fieldName.getName());
                    assertEquals("I", fieldName.getType());
                    assertEquals(Field.Label.REPEATED, fieldName.getLabel());

                }
        );
    }

    private void checkIMessage(ProtoFileElement parsedFile) {
        // message I
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("I"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("I"))
                .findFirst().ifPresent(
                i -> {
                    assertTrue(i instanceof MessageElement);
                    MessageElement iAsMessageElement = (MessageElement) i;

                    assertEquals(3, iAsMessageElement.getFields().size());

                    FieldElement field1 = iAsMessageElement
                            .getFields().get(0);

                    assertEquals("Field1", field1.getName());
                    assertEquals("string", field1.getType());

                    FieldElement field2 = iAsMessageElement
                            .getFields().get(1);

                    assertEquals("Field2", field2.getName());
                    assertEquals("bool", field2.getType());

                    FieldElement field3 = iAsMessageElement
                            .getFields().get(2);

                    assertEquals("Field3", field3.getName());
                    assertEquals("string", field3.getType());

                }

        );


    }


    private void checkDResponseMessage(ProtoFileElement parsedFile) {
        // message DResponse
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("DResponse"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("DResponse"))
                .findFirst().ifPresent(
                dResponse -> {
                    assertTrue(dResponse instanceof MessageElement);
                    MessageElement dResponseAsMessageElement = (MessageElement) dResponse;

                    assertEquals(2, dResponseAsMessageElement.getFields().size());

                    FieldElement fieldA = dResponseAsMessageElement
                            .getFields().get(0);

                    assertEquals("FieldA", fieldA.getName());
                    assertEquals("com.company.FieldNameA", fieldA.getType());

                    FieldElement fieldName = dResponseAsMessageElement
                            .getFields().get(1);

                    assertEquals("FieldName", fieldName.getName());
                    assertEquals("IResult", fieldName.getType());

                }
        );

    }

    private void checkFResultMessage(ProtoFileElement parsedFile) {
        // message FResult
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("FResult"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("FResult"))
                .findFirst().ifPresent(
                fResult -> {
                    assertTrue(fResult instanceof MessageElement);
                    MessageElement fResultAsMessageElement = (MessageElement) fResult;

                    assertEquals(1, fResultAsMessageElement.getFields().size());

                    FieldElement fieldB = fResultAsMessageElement
                            .getFields().get(0);

                    assertEquals("FieldB", fieldB.getName());
                    assertEquals("com.company.FieldNameB", fieldB.getType());
                }
        );


    }

    private void checkFResponseMessage(ProtoFileElement parsedFile) {
        // message FResponse
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("FResponse"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("FResponse"))
                .findFirst().ifPresent(
                fResponse -> {
                    assertTrue(fResponse instanceof MessageElement);
                    MessageElement fResponseAsMessageElement = (MessageElement) fResponse;

                    assertEquals(2, fResponseAsMessageElement.getFields().size());

                    FieldElement fieldA = fResponseAsMessageElement
                            .getFields().get(0);

                    assertEquals("FieldA", fieldA.getName());
                    assertEquals("com.company.FieldNameA", fieldA.getType());

                    FieldElement fieldName = fResponseAsMessageElement
                            .getFields().get(1);

                    assertEquals("FieldName", fieldName.getName());
                    assertEquals("FResult", fieldName.getType());
                }
        );

    }

    private void checkHResultMessage(ProtoFileElement parsedFile) {
        // message HResult
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("HResult"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("HResult"))
                .findFirst().ifPresent(
                hResult -> {
                    assertTrue(hResult instanceof MessageElement);
                    MessageElement hResultAsMessageElement = (MessageElement) hResult;

                    assertEquals(2, hResultAsMessageElement.getFields().size());

                    FieldElement fieldF = hResultAsMessageElement
                            .getFields().get(0);

                    assertEquals("FieldF", fieldF.getName());
                    assertEquals("com.company.FieldF", fieldF.getType());

                    FieldElement fieldName = hResultAsMessageElement
                            .getFields().get(1);

                    assertEquals("FieldName", fieldName.getName());
                    assertEquals("I", fieldName.getType());
                    assertEquals(Field.Label.REPEATED, fieldName.getLabel());

                }
        );

    }

    private void checkHResponseMessage(ProtoFileElement parsedFile) {
        // message HResponse
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("HResponse"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("HResponse"))
                .findFirst().ifPresent(
                hResponse -> {
                    assertTrue(hResponse instanceof MessageElement);
                    MessageElement hResponseAsMessageElement = (MessageElement) hResponse;

                    assertEquals(2, hResponseAsMessageElement.getFields().size());

                    FieldElement fieldA = hResponseAsMessageElement
                            .getFields()
                            .get(0);

                    assertEquals("FieldA", fieldA.getName());
                    assertEquals("com.company.FieldNameA", fieldA.getType());

                    FieldElement fieldName = hResponseAsMessageElement
                            .getFields()
                            .get(1);

                    assertEquals("FieldName", fieldName.getName());
                    assertEquals("HResult", fieldName.getType());

                }
        );

    }

    private void checkEndpointD(ServiceElement service) {
        // EndPointD
        assertTrue(
                service.getRpcs()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("EndpointD"))
        );

        service.getRpcs()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("EndpointD"))
                .findFirst().ifPresent(
                endpointC -> {
                    assertEquals("H", endpointC.getRequestType());
                    assertEquals("HResponse", endpointC.getResponseType());

                }
        );

    }

    private void checkEndpointC(ServiceElement service) {
        // EndPointC
        assertTrue(
                service.getRpcs()
                        .stream()
                        .anyMatch(rpcElement -> rpcElement.getName().equals("EndpointC"))
        );

        service.getRpcs()
                .stream()
                .filter(rpcElement -> rpcElement.getName().equals("EndpointC"))
                .findFirst().ifPresent(
                endpointC -> {
                    assertEquals("D", endpointC.getRequestType());
                    assertEquals("DResponse", endpointC.getResponseType());

                }
        );

    }

    private void checkEndpointB(ServiceElement service) {
        // EndPointB
        assertTrue(
                service.getRpcs()
                        .stream()
                        .anyMatch(rpcElement -> rpcElement.getName().equals("EndpointB"))
        );

        service.getRpcs()
                .stream()
                .filter(rpcElement -> rpcElement.getName().equals("EndpointB"))
                .findFirst().ifPresent(
                endpointA -> {
                    assertEquals("F", endpointA.getRequestType());
                    assertEquals("FResponse", endpointA.getResponseType());
                }
        );

    }

    private void checkEndpointA(ServiceElement service) {
        // EndPointA
        assertTrue(
                service.getRpcs()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("EndpointA"))
        );

        service.getRpcs()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("EndpointA"))
                .findFirst().ifPresent(
                endpointA -> {
                    assertEquals("B", endpointA.getRequestType());
                    assertEquals("BResponse", endpointA.getResponseType());

                }
        );

    }

    private void checkBResponseMessage(ProtoFileElement parsedFile) {
        // message BResponse
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("BResponse"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("BResponse"))
                .findFirst().ifPresent(
                bResponse -> {
                    assertTrue(bResponse instanceof MessageElement);
                    MessageElement bResponseAsMessageElement = (MessageElement) bResponse;

                    assertEquals(2, bResponseAsMessageElement.getFields().size());

                    FieldElement fieldA = bResponseAsMessageElement
                            .getFields().get(0);

                    assertEquals("FieldA", fieldA.getName());
                    assertEquals("com.company.FieldNameA", fieldA.getType());

                    FieldElement fieldName = bResponseAsMessageElement
                            .getFields().get(1);

                    assertEquals("FieldName", fieldName.getName());
                    assertEquals("A", fieldName.getType());


                }
        );


    }

    private void checkFieldNameAMessage(ProtoFileElement parsedFile) {
        // message FieldNameA
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("FieldNameA"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("FieldNameA"))
                .findFirst().ifPresent(
                fieldNameA -> {
                    assertTrue(fieldNameA instanceof MessageElement);
                    MessageElement fieldNameAAsMessageElement = (MessageElement) fieldNameA;

                    assertEquals(0, fieldNameAAsMessageElement.getFields().size());

                }
        );


    }

    private void checkFieldNameBMessage(ProtoFileElement parsedFile) {
        // message FieldNameA
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("FieldNameB"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("FieldNameB"))
                .findFirst().ifPresent(
                fieldNameB -> {
                    assertTrue(fieldNameB instanceof MessageElement);
                    MessageElement fieldNameBAsMessageElement = (MessageElement) fieldNameB;

                    assertEquals(0, fieldNameBAsMessageElement.getFields().size());

                }
        );


    }

    private void checkFieldEMessage(ProtoFileElement parsedFile) {
        // message FieldE
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("FieldE"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("FieldE"))
                .findFirst().ifPresent(
                fieldE ->
                {
                    assertTrue(fieldE instanceof MessageElement);
                    MessageElement fieldEAsMessageElement = (MessageElement) fieldE;

                    assertEquals(0, fieldEAsMessageElement.getFields().size());
                }
        );


    }

    private void checkFieldFMessage(ProtoFileElement parsedFile) {
        // message FieldF
        assertTrue(
                parsedFile.getTypes()
                        .stream()
                        .anyMatch(typeElement -> typeElement.getName().equals("FieldF"))
        );

        parsedFile.getTypes()
                .stream()
                .filter(typeElement -> typeElement.getName().equals("FieldF"))
                .findFirst().ifPresent(
                fieldF -> {
                    assertTrue(fieldF instanceof MessageElement);
                    MessageElement fieldFAsMessageElement = (MessageElement) fieldF;

                    assertEquals(0, fieldFAsMessageElement.getFields().size());

                }
        );


    }

}
