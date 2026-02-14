package com.opensearch.queries;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(path = "/queries")
public class OpenSearchQueriesRest {

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("setup")
    public void setupTestData() throws IOException {
        // Create test products with various characteristics
        productRepository.index(new Product("1", "iPhone 15", "Latest Apple smartphone with advanced features", 
            "electronics", 999.99, 5, true, Arrays.asList("apple", "smartphone", "mobile"), "Apple", "contact@apple.com"));
        
        productRepository.index(new Product("2", "Samsung Galaxy", "Android smartphone with great camera", 
            "electronics", 799.99, 4, true, Arrays.asList("samsung", "android", "mobile"), "Samsung", null));
        
        productRepository.index(new Product("3", "MacBook Pro", "Professional laptop for developers", 
            "computers", 1999.99, 5, false, Arrays.asList("apple", "laptop", "professional"), "Apple", "support@apple.com"));
        
        productRepository.index(new Product("4", "Dell XPS", "Windows laptop for business", 
            "computers", 1299.99, 4, true, Arrays.asList("dell", "windows", "business"), "Dell", "info@dell.com"));
        
        productRepository.index(new Product("5", "iPad Air", "Tablet for creativity and productivity", 
            "tablets", 599.99, 4, true, Arrays.asList("apple", "tablet", "creative"), "Apple", null));
    }

    // Term selector tests
    @GetMapping("category/{category}")
    public ResponseEntity<List<Product>> findByCategory(@PathVariable String category) throws IOException {
        List<Product> results = productRepository.findByCategory(category);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Terms selector tests
    @GetMapping("categories")
    public ResponseEntity<List<Product>> findByCategories(@RequestParam List<String> cats) throws IOException {
        List<Product> results = productRepository.findByCategories(cats);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Range selector tests
    @GetMapping("price-range")
    public ResponseEntity<List<Product>> findByPriceRange(@RequestParam(required = false) Double min, 
                                         @RequestParam(required = false) Double max) throws IOException {
        List<Product> results = productRepository.findByPriceRange(min, max);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    @GetMapping("rating-gte/{rating}")
    public ResponseEntity<List<Product>> findByRatingGte(@PathVariable Integer rating) throws IOException {
        List<Product> results = productRepository.findByRatingGte(rating);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Prefix selector tests
    @GetMapping("name-prefix/{prefix}")
    public ResponseEntity<List<Product>> findByNamePrefix(@PathVariable String prefix) throws IOException {
        List<Product> results = productRepository.findByNamePrefix(prefix);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Exists selector tests
    @GetMapping("with-email")
    public ResponseEntity<List<Product>> findWithEmail() throws IOException {
        List<Product> results = productRepository.findWithEmail();
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Fuzzy selector tests
    @GetMapping("name-fuzzy/{name}")
    public ResponseEntity<List<Product>> findByNameFuzzy(@PathVariable String name, 
                                        @RequestParam(defaultValue = "2") Integer fuzziness) throws IOException {
        List<Product> results = productRepository.findByNameFuzzy(name, fuzziness);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Wildcard selector tests
    @GetMapping("name-wildcard/{pattern}")
    public ResponseEntity<List<Product>> findByNameWildcard(@PathVariable String pattern) throws IOException {
        List<Product> results = productRepository.findByNameWildcard(pattern);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Regexp selector tests
    @GetMapping("email-pattern/{pattern}")
    public ResponseEntity<List<Product>> findByEmailPattern(@PathVariable String pattern) throws IOException {
        List<Product> results = productRepository.findByEmailPattern(pattern);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Match selector tests
    @GetMapping("description-match/{text}")
    public ResponseEntity<List<Product>> findByDescriptionMatch(@PathVariable String text) throws IOException {
        List<Product> results = productRepository.findByDescriptionMatch(text);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // Bool selector tests
    @GetMapping("complex")
    public ResponseEntity<List<Product>> findByComplexQuery(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) Double minPrice,
                                           @RequestParam(required = false) Boolean inStock) throws IOException {
        List<Product> results = productRepository.findByComplexQuery(category, minPrice, inStock);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }

    // IDs selector tests
    @GetMapping("by-ids")
    public ResponseEntity<List<Product>> findByIds(@RequestParam List<String> ids) throws IOException {
        List<Product> results = productRepository.findByIds(ids);
        return results.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(results);
    }
}
