package com.opensearch.queries;

import com.opensearch.config.OpenSearchRepository;
import java.util.stream.Collectors;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
public class ProductRepository {
    private final OpenSearchRepository openSearchRepository;

    public ProductRepository(OpenSearchRepository openSearchRepository) {
        this.openSearchRepository = openSearchRepository;
    }

    public void index(Product product) throws IOException {
        IndexRequest<Product> request = new IndexRequest.Builder<Product>()
            .index("products")
            .id(product.getId())
            .document(product)
            .build();
        openSearchRepository.index(request);
    }

    // Term queries
    public List<Product> findByCategory(String category) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.term(term -> 
                term.field("category").value(value -> value.stringValue(category))))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Terms queries
    public List<Product> findByCategories(List<String> categories) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.terms(terms -> 
                terms.field("category").terms(termsValue -> 
                    termsValue.value(categories.stream().map(FieldValue::of).collect(Collectors.toList()))
                )))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Range queries
    public List<Product> findByPriceRange(Double gte, Double lte) throws IOException {
    SearchRequest request =
        new SearchRequest.Builder()
            .index("products")
            .query(
                query ->
                    query.range(
                        range -> {
                          RangeQuery.Builder rangeQuery = range.field("price");
                          if (gte != null) rangeQuery.gte(JsonData.of(gte));
                          if (lte != null) rangeQuery.lte(JsonData.of(lte));
                          return rangeQuery;
                        }))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    public List<Product> findByRatingGte(Integer rating) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.range(range -> 
                range.field("rating").gte(JsonData.of(rating))))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Prefix queries
    public List<Product> findByNamePrefix(String prefix) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.prefix(prefixQuery -> 
                prefixQuery.field("name").value(prefix)))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Exists queries
    public List<Product> findWithEmail() throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.exists(exists -> 
                exists.field("email")))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Fuzzy queries
    public List<Product> findByNameFuzzy(String name, Integer fuzziness) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.fuzzy(fuzzy -> 
                fuzzy.field("name").value(FieldValue.of(name)).fuzziness(String.valueOf(fuzziness))))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Wildcard queries
    public List<Product> findByNameWildcard(String pattern) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.wildcard(wildcard -> 
                wildcard.field("name").value(pattern)))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Regexp queries
    public List<Product> findByEmailPattern(String pattern) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.regexp(regexp -> 
                regexp.field("email").value(pattern)))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Match queries
    public List<Product> findByDescriptionMatch(String text) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.match(match -> 
                match.field("description").query(FieldValue.of(text))))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // Bool queries
    public List<Product> findByComplexQuery(String category, Double minPrice, Boolean inStock) throws IOException {
    SearchRequest request =
        new SearchRequest.Builder()
            .index("products")
            .query(
                query ->
                    query.bool(
                        bool -> {
                          if (category != null) {
                            bool.must(
                                must ->
                                    must.term(
                                        term ->
                                            term.field("category")
                                                .value(value -> value.stringValue(category))));
                          }

                          if (inStock != null) {
                            bool.must(
                                must ->
                                    must.term(
                                        term ->
                                            term.field("inStock")
                                                .value(value -> value.booleanValue(inStock))));
                          }

                          // Filter clause
                          if (minPrice != null) {
                            bool.filter(
                                filter ->
                                    filter.range(
                                        range -> range.field("price").gte(JsonData.of(minPrice))));
                          }

                          return bool;
                        }))
            .build();
        return openSearchRepository.search(request, Product.class);
    }

    // IDs queries
    public List<Product> findByIds(List<String> ids) throws IOException {
        SearchRequest request = new SearchRequest.Builder()
            .index("products")
            .query(query -> query.ids(idsQuery -> 
                idsQuery.values(ids)))
            .build();
        return openSearchRepository.search(request, Product.class);
    }
}
