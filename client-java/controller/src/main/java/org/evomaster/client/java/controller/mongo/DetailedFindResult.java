package org.evomaster.client.java.controller.mongo;

import org.bson.Document;
import org.evomaster.client.java.controller.api.dto.database.execution.FindResultDto;

import java.util.ArrayList;

public class DetailedFindResult extends FindResult {

    private final ArrayList<Document> documentList = new ArrayList<>();

    public void addDocument(Document document) {
        documentList.add(document);
    }

    public boolean hasOperationFoundAnyDocuments() {
        return !documentList.isEmpty();
    }

    @Override
    public FindResultDto toDto() {
        FindResultDto dto = new FindResultDto();
        dto.findResultType = FindResultDto.FindResultType.DETAILED;
        dto.documents = this.documentList.toArray(new Document[]{});
        dto.hasReturnedAnyDocument = !this.documentList.isEmpty();
        return dto;
    }

}
