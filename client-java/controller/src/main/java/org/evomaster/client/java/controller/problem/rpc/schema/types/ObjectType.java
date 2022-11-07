package org.evomaster.client.java.controller.problem.rpc.schema.types;

import org.evomaster.client.java.controller.api.dto.problem.rpc.RPCSupportedDataType;
import org.evomaster.client.java.controller.api.dto.problem.rpc.TypeDto;
import org.evomaster.client.java.controller.problem.rpc.CodeJavaGenerator;
import org.evomaster.client.java.controller.problem.rpc.schema.params.NamedTypedValue;

import java.util.ArrayList;
import java.util.List;

/**
 * object type
 */
public class ObjectType extends TypeSchema {
    /**
     * a list of fields of the object
     */
    private final List<NamedTypedValue> fields;

    /**
     * a list of generic types
     */
    private final List<String> genericTypes;


    public ObjectType(String type, String fullTypeName, List<NamedTypedValue> fields, Class<?> clazz, List<String> genericTypes) {
        super(type, fullTypeName, clazz);
        this.fields = fields;
        this.genericTypes = genericTypes;
    }

    public List<NamedTypedValue> getFields() {
        return fields;
    }

    @Override
    public TypeDto getDto() {
        TypeDto dto = super.getDto();
        dto.depth = depth;
        dto.type = RPCSupportedDataType.CUSTOM_OBJECT;
        return dto;
    }

    public ObjectType copyContent(){
        List<NamedTypedValue> cfields = new ArrayList<>();
        if (fields != null){
            for (NamedTypedValue f: fields){
                cfields.add(f.copyStructureWithProperties());
            }
        }
        List<String> genericTypes = this.genericTypes != null? new ArrayList<>(this.genericTypes): null;
        ObjectType objectType = new ObjectType(getType(), getFullTypeName(), cfields ,getClazz(), genericTypes);
        objectType.depth = depth;
        return objectType;
    }

    @Override
    public String getFullTypeNameWithGenericType() {
        if (genericTypes == null || genericTypes.isEmpty())
            return getFullTypeName();
        else
            return CodeJavaGenerator.handleClassNameWithGeneric(getFullTypeName(),genericTypes);
    }
}
