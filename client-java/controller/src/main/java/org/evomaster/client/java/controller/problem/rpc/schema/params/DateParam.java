package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.DateType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * handle date param with java.util.Date
 */
public class DateParam extends NamedTypedValue<DateType, List<IntParam>>{

    public DateParam(String name, DateType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }

    public DateParam(String name, AccessibleSchema accessibleSchema){
        this(name, new DateType(), accessibleSchema);
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
        setValue(getType().getIntValues((Date) instance));
    }

    @Override
    public List<String> newInstanceWithJava(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent) {
        String typeName = getType().getTypeNameForInstance();
        String varName = variableName;

        List<String> codes = new ArrayList<>();
        boolean isNull = (getValue() == null);

        String var = CodeJavaGenerator.oneLineInstance(isDeclaration, doesIncludeName, typeName, varName, null);
        CodeJavaGenerator.addCode(codes, var, indent);
        if (isNull) return codes;

        CodeJavaGenerator.addCode(codes, "{", indent);
        CodeJavaGenerator.addComment(codes, "Date is " + getType().getDateString(getValue()), indent+1);
        String time = getType().getDateLong(getValue())+"L";
        CodeJavaGenerator.addCode(codes, CodeJavaGenerator.setInstance(varName, CodeJavaGenerator.newObjectConsParams(typeName, time)), indent+1);
        CodeJavaGenerator.addCode(codes, "}", indent);

        return codes;
    }

    @Override
    public List<String> newAssertionWithJava(int indent, String responseVarName, int maxAssertionForDataInCollection) {
        StringBuilder sb = new StringBuilder();
        sb.append(CodeJavaGenerator.getIndent(indent));
        if (getValue() == null)
            sb.append(CodeJavaGenerator.junitAssertNull(responseVarName));
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
    public String getValueAsJavaString() {
        return null;
    }
}
