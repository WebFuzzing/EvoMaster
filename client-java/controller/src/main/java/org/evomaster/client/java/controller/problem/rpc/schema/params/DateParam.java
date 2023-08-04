package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaOrKotlinGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.DateType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.JavaDtoSpec;

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

    public DateParam(String name, AccessibleSchema accessibleSchema, JavaDtoSpec spec){
        this(name, new DateType(spec), accessibleSchema);
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
    public List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava) {
        String typeName = getType().getTypeNameForInstanceInJavaOrKotlin(isJava);
        String varName = variableName;

        List<String> codes = new ArrayList<>();
        boolean isNull = (getValue() == null);

        String var = CodeJavaOrKotlinGenerator.oneLineInstance(isDeclaration, doesIncludeName, typeName, varName, null, isJava);
        CodeJavaOrKotlinGenerator.addCode(codes, var, indent);
        if (isNull) return codes;

        CodeJavaOrKotlinGenerator.addCode(codes, "{", indent);
        CodeJavaOrKotlinGenerator.addComment(codes, "Date is " + getType().getDateString(getValue()), indent+1);
        String time = getType().getDateLong(getValue())+"L";
        CodeJavaOrKotlinGenerator.addCode(codes, CodeJavaOrKotlinGenerator.setInstance(varName, CodeJavaOrKotlinGenerator.newObjectConsParams(typeName, time, isJava), isJava), indent+1);
        CodeJavaOrKotlinGenerator.addCode(codes, "}", indent);

        return codes;
    }

    @Override
    public List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava) {
        StringBuilder sb = new StringBuilder();
        sb.append(CodeJavaOrKotlinGenerator.getIndent(indent));
        if (getValue() == null)
            sb.append(CodeJavaOrKotlinGenerator.junitAssertNull(responseVarName, isJava));
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
