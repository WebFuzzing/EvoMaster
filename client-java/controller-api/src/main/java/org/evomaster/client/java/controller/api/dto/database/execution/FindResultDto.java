package org.evomaster.client.java.controller.api.dto.database.execution;

import org.bson.Document;

public class FindResultDto {

    public static final String HAS_RETURNED_ANY_DOCUMENT_FIELD_NAME = "hasReturnedAnyDocument";

    public static final String DOCUMENTS_FIELD_NAME = "documents";

    public enum FindResultType {SUMMARY, DETAILED}

    public FindResultType findResultType;

    public boolean hasReturnedAnyDocument;

    public Document[] documents;

}
