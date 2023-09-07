package org.evomaster.client.java.controller.problem.rpc.schema.params;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.PrimitiveOrWrapperType;
import org.evomaster.client.java.controller.problem.rpc.schema.types.TypeSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * a named instance of the type with its value, eg Param/Field
 * it could be a request param or a response
 */
public abstract class NamedTypedValue<T extends TypeSchema, V> {



    protected final static ObjectMapper objectMaper = new ObjectMapper();

    /**
     * name of the instance, eg param name
     */
    private final String name;

    /**
     * its type
     */
    private final T type;

    /**
     * its value
     */
    private V value;

    /**
     * represent whether this value is nullable
     */
    private boolean isNullable = true;

    /**
     * represent whether the value is mutable
     *
     * note that if the param is not mutable and the default value is null,
     * it represents that the value is a fixed NULL
     */
    private boolean isMutable = true;

    /**
     * default value for the parameter
     * it is nullable
     */
    private NamedTypedValue defaultValue;

    /**
     * a schema for collecting if the param is accessible
     */
    public final AccessibleSchema accessibleSchema;


    public boolean isHasDependentCandidates() {
        return hasDependentCandidates;
    }

    public void setHasDependentCandidates(boolean hasDependentCandidates) {
        this.hasDependentCandidates = hasDependentCandidates;
    }

    /**
     * represent whether there are specified dependent candidates
     */
    private boolean hasDependentCandidates = false;

    public List<NamedTypedValue> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<NamedTypedValue> candidates) {
        this.candidates = candidates;
    }

    /**
     * represent candidates
     */
    private List<NamedTypedValue> candidates;

    public List<String> getCandidateReferences() {
        return candidateReferences;
    }

    public void setCandidateReferences(List<String> candidateReferences) {
        this.candidateReferences = candidateReferences;
    }

    /**
     * represent candidates
     */
    private List<String> candidateReferences;


    public NamedTypedValue(String name, T type, AccessibleSchema accessibleSchema) {
        this.name = name;
        this.type = type;
        this.accessibleSchema = accessibleSchema;
    }

    public String getName() {
        return (name != null)? name:"Untitled";
    }

    public T getType() {
        return type;
    }

    public V getValue() {
        return value;
    }

    public abstract Object newInstance() throws ClassNotFoundException;

    public abstract List<String> referenceTypes();

    public void setValue(V value) {
        this.value = value;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public boolean isNullable() {
        return isNullable;
    }

    /**
     * get its dto format in order to further handle it with evomastr core
     * @return its corresponding dto
     */
    public ParamDto getDto(){
        ParamDto dto = new ParamDto();
        dto.name = name;
        dto.type = type.getDto();
        dto.isNullable = isNullable;
        if (candidates!=null)
            dto.candidates = candidates.stream().map(NamedTypedValue::getDto).collect(Collectors.toList());
        if (candidateReferences!=null)
            dto.candidateReferences = new ArrayList<>(candidateReferences);

        dto.isMutable = isMutable;
        if (defaultValue != null)
            dto.defaultValue = defaultValue.getDto();
        return dto;
    }

    public NamedTypedValue<T, V> copyStructureWithProperties(){
        NamedTypedValue copy = copyStructure();
        copyProperties(copy);
        return copy;
    }

    public abstract NamedTypedValue<T, V> copyStructure();

    public void copyProperties(NamedTypedValue copy){
        copy.setNullable(isNullable);
        copy.setHasDependentCandidates(isHasDependentCandidates());
        copy.setMutable(isMutable());
        copy.setDefaultValue(getDefaultValue());
        if (getCandidates() != null && !getCandidates().isEmpty())
            copy.setCandidates(getCandidates().stream().map(c-> c.copyStructureWithProperties()).collect(Collectors.toList()));
        if (getCandidateReferences()!= null && !getCandidateReferences().isEmpty())
            copy.setCandidateReferences(new ArrayList<>(getCandidateReferences()));
    }


    /**
     * it is used to find a param schema based on info specified with dto format
     * @param dto specifies a param to check
     * @return whether [this] param schema info is consistent with the given dto
     */
    public boolean sameParam(ParamDto dto){
        return dto.name.equals(name) && type.sameType(dto.type);
    }

    /**
     * set value based on dto
     * the value is basically obtained from evomaster core
     * @param dto contains value info with string
     */
    public abstract void setValueBasedOnDto(ParamDto dto);

    /**
     * set value of param schema based on its instance
     * it is mainly used to parse response
     * @param instance a java object which is an instance of this param schema
     */
    public void setValueBasedOnInstance(Object instance){
        if (instance == null) return;
        if (isValidInstance(instance))
            setValueBasedOnValidInstance(instance);
        else
            throw new IllegalStateException("class of the instance ("+ instance.getClass().getName() +") does not conform with this param: "+getType().getFullTypeName());
    }

    /**
     * set value based on json
     * @param json contains value info with json format
     */
    public void setValueBasedOnInstanceOrJson(Object json) throws JsonProcessingException{
        Object instance = null;
        if (!isValidInstance(json)){
            if (json instanceof String)
                instance = parseValueWithJson((String) json);
            else if (PrimitiveOrWrapperType.isPrimitiveOrTypes(json.getClass())){
                instance = ((PrimitiveOrWrapperParam)this).convertValueTo(json);
            } else
                throw new RuntimeException("Fail to extract value from json for "+ getType().getFullTypeName());
        }else
            instance = json;


        setValueBasedOnInstance(instance);
    }

    public Object parseValueWithJson(String json) throws JsonProcessingException {
        return objectMaper.readValue(json, getType().getClazz());
    }

    /**
     * set the value of the param based on instance
     * compared with [setValueBasedOnInstance], the type of the instance here is evaluated as valid
     * @param instance is the instance
     */
    protected abstract void setValueBasedOnValidInstance(Object instance);

    /**
     *
     * @param instance a java object which is an instance of this param schema
     * @return if the specified instance conforms with this param schema
     */
    public boolean isValidInstance(Object instance){
        return getType().getClazz().isInstance(instance);
    }


    /**
     * create instances with Java
     *
     * @param isDeclaration      specifies whether it is used to declare the instance (ie, with type name).
     * @param doesIncludeName    specifies whether it is required to have the variable name
     *                           eg, if true, var = new instance(); if yes, new instance();
     * @param variableName       specifies the name of variable
     * @param indent             specifies the indent of this block of the code
     * @param isJava
     * @param isVariableNullable
     * @return a list of string which could create instance with java
     */
    public abstract List<String> newInstanceWithJavaOrKotlin(boolean isDeclaration, boolean doesIncludeName, String variableName, int indent, boolean isJava, boolean isVariableNullable);


    /**
     * create instances with Java
     *
     * @param indent             specifies the current indent of the code
     * @param isJava
     * @param isVariableNullable
     * @return a list of string which could create instance with java
     */
    public List<String> newInstanceWithJavaOrKotlin(int indent, boolean isJava, boolean isVariableNullable){
        return newInstanceWithJavaOrKotlin(true, true, getName(), indent, isJava, isVariableNullable);
    }

    /**
     * create assertions with java for response
     *
     * @param indent                          specifies the current indent of the code
     * @param responseVarName                 a variable name for responses
     * @param maxAssertionForDataInCollection
     * @param isJava
     * @return a list of string for assertions
     */
    public abstract List<String> newAssertionWithJavaOrKotlin(int indent, String responseVarName, int maxAssertionForDataInCollection, boolean isJava);

    /**
     * @param responseVarName is the variable name of the response
     * @param isJava
     * @return a list of assertions based on this which could be a response
     */
    public List<String> newAssertionWithJavaOrKotlin(String responseVarName, int maxAssertionForDataInCollection, boolean isJava){
        return newAssertionWithJavaOrKotlin(0, responseVarName, maxAssertionForDataInCollection, isJava);
    }

    /**
     *
     * @return a string which could representing the value of the param with java
     * eg, float 4.2 could be 4.2f
     */
    public abstract String getValueAsJavaString(boolean isJava);

    public boolean isMutable() {
        return isMutable;
    }

    public void setMutable(boolean mutable) {
        isMutable = mutable;
    }

    public NamedTypedValue getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(NamedTypedValue defaultValue) {
        this.defaultValue = defaultValue;
    }

}
