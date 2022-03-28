package com.foo.rest.examples.spring.bodytypes;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Created by arcuri82 on 07-Nov-18.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BodyTypesDto {

    @XmlElement(nillable = true, required = false)
    public Integer value;
}
