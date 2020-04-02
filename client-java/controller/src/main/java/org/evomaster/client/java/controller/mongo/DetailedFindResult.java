package org.evomaster.client.java.controller.mongo;

import org.bson.Document;
import org.evomaster.client.java.controller.api.dto.mongo.FindResultDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DetailedFindResult extends FindResult {

    private final List<Document> documentList = new ArrayList<>();

    public void addDocument(Document document) {
        Objects.requireNonNull(document);
        documentList.add(document);
    }

    public boolean hasOperationFoundAnyDocuments() {
        return !documentList.isEmpty();
    }

    @Override
    public FindResultDto toDto() {
        FindResultDto dto = new FindResultDto();
        dto.findResultType = FindResultDto.FindResultType.DETAILED;
        dto.documents = new ArrayList<>(this.documentList);
        dto.hasReturnedAnyDocument = !this.documentList.isEmpty();
        return dto;
    }

}
