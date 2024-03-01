package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator.*;

/**
 * handle date param with java.util.Date
 */
public class DateParam extends NamedTypedValue<DateType, List<IntParam>>{

    public DateParam(String name, DateType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    @Override
    public Object newInstance() throws ClassNotFoundException {
        if (getValue() == null) return null;
        return getType().getDateInstance(getValue());
    }

    @Override
    public DateParam copyStructure() {
        return new DateParam(getName(), getType(), accessibleSchema);
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (getValue() != null){
            dto.innerContent = getValue().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
            dto.setNotNullValue();
        } else
            dto.innerContent = getType().getDateFields().stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        return dto;
    }

    @Override
    public void setValueBasedOnDto(ParamDto dto) {
        if (dto.innerContent!=null && !dto.innerContent.isEmpty()){
            List<IntParam> fields = getType().getDateFields();
            List<IntParam> values = new ArrayList<>();

            for (ParamDto p: dto.innerContent){
                IntParam f = (IntParam) fields.stream().filter(s-> s.sameParam(p)).findFirst().get().copyStructureWithProperties();
                f.setValueBasedOnDto(p);
                values.add(f);
            }

            setValue(values);
        }
    }

    @Override
    protected void setValueBasedOnValidInstance(Object instance) {
        if (instance == null) return;
        setValue(getType().getIntValues(instance));
    }

    @Override
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable) {
        String typeName = getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
        String varName = variableName;

        List<String> codes = new ArrayList<>();
        boolean isNull = (getValue() == null);

        String var = oneLineInstance(isDeclaration, doesIncludeName, typeName, varName, null, isJava, isNullable());
        addCode(codes, var, indent);
        if (isNull) return codes;

        addCode(codes, codeBlockStart(isJava), indent);
        addComment(codes, "Date is " + getType().getDateString(getValue()), indent+1);
        String time = getType().getDateLong(getValue())+"L";
        if (getType() instanceof UtilDateType)
            addCode(codes, setInstance(varName, newObjectConsParams(typeName, time, isJava), isJava), indent+1);
        else if (getType() instanceof LocalDateType)
            addCode(codes, setInstance(varName, methodInvocation(typeName, LocalDateType.INSTANCE_LOCALDATE_OF_EPOCHDAY, time, isJava, false, false), isJava), indent+1);
        addCode(codes, codeBlockEnd(isJava), indent);

        return codes;
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent(indent));
        if (getValue() == null)
            sb.append(junitAssertNull(responseVarName, isJava));
        else{
            /*
                it might be tricky to handle date assertion since it might be `now`
                then here we just append runtime value as comments
             */
            sb.append("// runtime value is ");
            sb.append(getType().getDateString(getValue()));
        }

        return Collections.singletonList(sb.toString());
    }

    @Override
    public String getValueAsJavaString(boolean isJava) {
        return null;
    }

    @Override
    public List<String> referenceTypes() {
        return null;
    }
}
