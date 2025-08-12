package com.opensearch.students;

import com.opensearch.config.OpenSearchRepository;
import java.io.IOException;
import java.util.List;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class StudentsRepository {
    private final OpenSearchRepository openSearchRepository;

    public StudentsRepository(OpenSearchRepository openSearchRepository) {
        this.openSearchRepository = openSearchRepository;
    }


    public void index(Student s) throws IOException {
        IndexRequest<Student> request = new IndexRequest.Builder<Student>().index("students").document(s).build();
        openSearchRepository.index(request);
    }

    public List<Student> findByLastName(String lastName) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("students")
            .query(
                query -> query
                    .match(matcher -> matcher.field("lastName").query(value -> value.stringValue(lastName)))
            ).build();

        return openSearchRepository.search(request, Student.class);
    }

    public List<Student> findByFirstName(String firstName) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("students")
            .query(
                query -> query
                    .match(matcher -> matcher.field("firstName").query(value -> value.stringValue(firstName)))
            ).build();

        return openSearchRepository.search(request, Student.class);
    }
}
