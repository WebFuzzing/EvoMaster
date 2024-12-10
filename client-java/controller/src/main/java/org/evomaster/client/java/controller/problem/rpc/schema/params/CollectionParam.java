package org.evomaster.client.java.controller.problem.rpc.schema.params;

import org.evomaster.client.java.controller.api.dto.problem.rpc.ParamDto;
import org.evomaster.client.java.controller.problem.rpc.schema.types.AccessibleSchema;
import org.evomaster.client.java.controller.problem.rpc.schema.types.CollectionType;

import java.util.ArrayList;
import java.util.List;

/**
 * collection param
 * @param <V> representing the value of the collection
 */
public abstract class CollectionParam<V> extends NamedTypedValue<CollectionType, V>{

    /**
     * min size of the collection if it is specified
     */
    private Integer minSize;

    /**
     * max size of the collection if it is specified
     */
    private Integer maxSize;

    public CollectionParam(String name, CollectionType type, AccessibleSchema accessibleSchema) {
        super(name, type, accessibleSchema);
    }


    public Integer getMinSize() {
        return minSize;
    }

    public void setMinSize(Integer minSize) {
        if (this.minSize != null && this.minSize >= minSize)
            return;
        this.minSize = minSize;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public ParamDto getDto() {
        ParamDto dto = super.getDto();
        if (maxSize != null)
            dto.maxSize = Long.valueOf(maxSize);
        if (minSize != null)
            dto.minSize = Long.valueOf(minSize);
        return dto;
    }

    @Override
    public void copyProperties(NamedTypedValue copy) {
        super.copyProperties(copy);
        if (copy instanceof CollectionParam){
            ((CollectionParam)copy).setMinSize(minSize);
            ((CollectionParam)copy).setMaxSize(maxSize);
        }
    }

    @Override
    public List<String> referenceTypes() {
        List<String> references = new ArrayList<>();
        NamedTypedValue template = getType().getTemplate();
        references.add(template.getType().getFullTypeName());

        List<String> refrefTypes = template.referenceTypes();
        if (refrefTypes != null)
            references.addAll(refrefTypes);

        if (references.isEmpty()) return null;
        return references;
    }
}
