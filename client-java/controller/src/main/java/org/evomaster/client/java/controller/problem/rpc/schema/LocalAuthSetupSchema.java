package org.evomaster.client.java.controller.problem.rpc.schema;

import org.evomaster.client.java.controller.DtoUtils;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCActionDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;
import org.evomaster.client.java.controller.problem.rpc.schema.params.StringParam;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.JavaDtoSpec;
import org.evomaster.client.java.controller.problem.rpc.schema.types.StringType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;


public class LocalAuthSetupSchema extends EndpointSchema{

    public final static String EM_LOCAL_METHOD = "__EM__LOCAL__";
    public final static String HANDLE_LOCAL_AUTHENTICATION_SETUP_METHOD_NAME = "handleLocalAuthenticationSetup";

    public LocalAuthSetupSchema() {
        super(HANDLE_LOCAL_AUTHENTICATION_SETUP_METHOD_NAME,
                EM_LOCAL_METHOD, null, Arrays.asList(new StringParam("arg0", new StringType(JavaDtoSpec.DEFAULT), new AccessibleSchema())), null, null, false, null, null);
    }

    /**
     *
     * @return value of AuthenticationInfo
     */
    public String getAuthenticationInfo(){
        return ((StringParam)getRequestParams().get(0)).getValue();
    }

    @Override
    public List<String> newInvocationWithJavaOrKotlin(String responseVarName, String controllerVarName, String clientVariable, SutInfoDto.OutputFormat outputFormat) {
        List<String> javaCode = new ArrayList<>();
        javaCode.add(codeBlockStart(DtoUtils.isJava(outputFormat)));
        int indent = 1;
        for (NamedTypedValue param: getRequestParams()){
            javaCode.addAll(param.newInstanceWithJavaOrKotlin(indent, DtoUtils.isJava(outputFormat), true));
        }
        String paramVars = getRequestParams().stream().map(NamedTypedValue::getName).collect(Collectors.joining(","));

        addCode(
                javaCode,
                CodeJavaOrKotlinGenerator.methodInvocation(controllerVarName, getName(), paramVars,DtoUtils.isJava(outputFormat), true, false) + getStatementLast(DtoUtils.isJava(outputFormat)),
                indent);

        javaCode.add(codeBlockEnd(DtoUtils.isJava(outputFormat)));
        return javaCode;
    }

    /**
     *
     * @param dto a RPCAction dto
     * @return if the action is to local method
     */
    public static boolean isLocalAuthSetup(RPCActionDto dto){
        return dto.actionName.equals(HANDLE_LOCAL_AUTHENTICATION_SETUP_METHOD_NAME) && dto.interfaceId.equals(EM_LOCAL_METHOD) && dto.clientInfo == null;
    }
}
