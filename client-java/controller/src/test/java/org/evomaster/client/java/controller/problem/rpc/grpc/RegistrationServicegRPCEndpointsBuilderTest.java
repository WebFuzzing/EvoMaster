package org.evomaster.client.java.controller.problem.rpc.grpc;

import com.google.protobuf.ByteString;
import org.evomaster.client.java.controller.problem.rpc.RPCEndpointsBuilderTestBase;
import org.evomaster.client.java.controller.problem.rpc.schema.EndpointSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.params.ObjectParam;
import org.evomaster.client.java.controller.problem.rpc.schema.types.ObjectType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.Protobuf3ByteStringType;
import org.junit.Test;
import org.signal.registration.rpc.GetRegistrationSessionMetadataRequest;
import org.signal.registration.rpc.RegistrationServiceGrpc;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegistrationServicegRPCEndpointsBuilderTest extends RPCEndpointsBuilderTestBase {
    @Override
    public String getInterfaceName() {
        return RegistrationServiceGrpc.RegistrationServiceBlockingStub.class.getName();
    }

    @Override
    public int expectedNumberOfEndpoints() {
        return 5;
    }


    @Test
    public void testAllFunctions(){
        List<String> expectedFunctions = Arrays.asList(
            "createSession",
            "getSessionMetadata",
            "sendVerificationCode",
            "checkVerificationCode",
            "legacyCheckVerificationCode"
        );

        for (String fun : expectedFunctions){
            EndpointSchema endpoint = getOneEndpoint(fun);
            assertNotNull(endpoint);
        }

    }

    @Test
    public void testGetSessionMetadata() throws ClassNotFoundException {
        EndpointSchema endpoint = getOneEndpoint("getSessionMetadata");

        List<NamedTypedValue> params = endpoint.getRequestParams();
        assertEquals(1, params.size());
        NamedTypedValue inputParam = params.get(0);
        assertTrue(inputParam instanceof ObjectParam);
        ObjectType paramType = ((ObjectParam) inputParam).getType();
        assertEquals(1, paramType.getFields().size());
        assertEquals("sessionId", paramType.getFields().get(0).getName());
        assertEquals(Protobuf3ByteStringType.PROTOBUF3_BYTE_STRING_TYPE_NAME, paramType.getFields().get(0).getType().getFullTypeName());

        ByteString sb = ByteString.copyFromUtf8("foo");
        GetRegistrationSessionMetadataRequest obj = GetRegistrationSessionMetadataRequest.newBuilder().setSessionId(sb).build();
        inputParam.setValueBasedOnInstance(obj);
        Object inputInstance = inputParam.newInstance();
        assertTrue(inputInstance instanceof GetRegistrationSessionMetadataRequest);
        assertEquals("foo", ((GetRegistrationSessionMetadataRequest) inputInstance).getSessionId().toStringUtf8());

        List<String> param1InstanceJava = inputParam.newInstanceWithJavaOrKotlin(true, true, "request", 0, true, true);

        String[] expectedContents = ("org.signal.registration.rpc.GetRegistrationSessionMetadataRequest request = null;\n" +
            "{\n" +
            " org.signal.registration.rpc.GetRegistrationSessionMetadataRequest.Builder requestbuilder = org.signal.registration.rpc.GetRegistrationSessionMetadataRequest.newBuilder();\n" +
            " com.google.protobuf.ByteString request_sessionId = null;\n" +
            " {\n" +
            "  request_sessionId = com.google.protobuf.ByteString.copyFromUtf8(\"foo\");\n" +
            " }\n" +
            " requestbuilder.setSessionId(request_sessionId);\n" +
            " request = requestbuilder.build();\n" +
            "}").split("\n");

        assertEquals(expectedContents.length, param1InstanceJava.size());

        for (int i = 0; i < param1InstanceJava.size(); i++)
            assertEquals(expectedContents[i], param1InstanceJava.get(i));

        List<String> param1Assertions = inputParam.newAssertionWithJavaOrKotlin("request", -1, true);

        String[] expectedAssertions = ("assertEquals(\"foo\", request.getSessionId().toStringUtf8());").split("\n");

        assertEquals(expectedAssertions.length, param1Assertions.size());

        for (int i = 0; i < param1Assertions.size(); i++)
            assertEquals(expectedAssertions[i], param1Assertions.get(i));

        org.signal.registration.rpc.GetRegistrationSessionMetadataRequest request = null;
        {
            org.signal.registration.rpc.GetRegistrationSessionMetadataRequest.Builder requestbuilder = org.signal.registration.rpc.GetRegistrationSessionMetadataRequest.newBuilder();
            com.google.protobuf.ByteString request_sessionId = null;
            {
                request_sessionId = com.google.protobuf.ByteString.copyFromUtf8("foo");
            }
            requestbuilder.setSessionId(request_sessionId);
            request = requestbuilder.build();
        }
        assertEquals("foo", request.getSessionId().toStringUtf8());



        List<String> param1InstanceKotlin = inputParam.newInstanceWithJavaOrKotlin(true, true, "request", 0, false, true);

    }
}
