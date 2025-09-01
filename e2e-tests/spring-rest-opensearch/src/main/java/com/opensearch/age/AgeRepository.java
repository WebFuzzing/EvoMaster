package com.opensearch.age;

import com.opensearch.config.OpenSearchRepository;
import java.io.IOException;
import java.util.List;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.stereotype.Repository;

@Repository
public class AgeRepository {
    private final OpenSearchRepository openSearchRepository;

    public AgeRepository(OpenSearchRepository openSearchRepository) {
        this.openSearchRepository = openSearchRepository;
    }

    public void index(Age s) throws IOException {
        IndexRequest<Age> request = new IndexRequest.Builder<Age>().index("age").document(s).build();
        openSearchRepository.index(request);
    }

    public List<Age> findByAge(Integer age) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.term(term ->
            term.field("number").value(value -> value.longValue(age)))).build();
        return openSearchRepository.search(request, Age.class);
    }

    public List<Age> findGteAge(Integer gte) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.range(range ->
            range.field("number").gte(JsonData.of(gte)))).build();
        return openSearchRepository.search(request, Age.class);
    }

    public List<Age> findLteAge(Integer lte) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.range(range ->
            range.field("number").lte(JsonData.of(lte)))).build();
        return openSearchRepository.search(request, Age.class);
    }

    public List<Age> findGtAge(Integer gt) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.range(range ->
            range.field("number").gt(JsonData.of(gt)))).build();
        return openSearchRepository.search(request, Age.class);
    }

    public List<Age> findLtAge(Integer lt) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.range(range ->
            range.field("number").lt(JsonData.of(lt)))).build();
        return openSearchRepository.search(request, Age.class);
    }

    public List<Age> findGteLteAge(Integer gte, Integer lte) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.range(range ->
            range.field("number").gte(JsonData.of(gte)).lte(JsonData.of(lte)))).build();
        return openSearchRepository.search(request, Age.class);
    }

    public List<Age> findGteLtAge(Integer gte, Integer lt) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.range(range ->
            range.field("number").gte(JsonData.of(gte)).lt(JsonData.of(lt)))).build();
        return openSearchRepository.search(request, Age.class);
    }

    public List<Age> findGtLteAge(Integer gt, Integer lte) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.range(range ->
            range.field("number").gt(JsonData.of(gt)).lte(JsonData.of(lte)))).build();
        return openSearchRepository.search(request, Age.class);
    }

    public List<Age> findGtLtAge(Integer gt, Integer lt) throws IOException {
        SearchRequest request = new SearchRequest.Builder().index("age").query(query -> query.range(range ->
            range.field("number").gt(JsonData.of(gt)).lt(JsonData.of(lt)))).build();
        return openSearchRepository.search(request, Age.class);
    }
}
