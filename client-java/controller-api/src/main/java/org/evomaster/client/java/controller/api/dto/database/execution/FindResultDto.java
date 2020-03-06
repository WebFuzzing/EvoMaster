package org.evomaster.client.java.controller.api.dto.database.execution;

public class FindResultDto {

    public enum FindResultType { SUMMARY }

    public FindResultType findResultType;

    public boolean hasReturnedAnyDocument;

}
