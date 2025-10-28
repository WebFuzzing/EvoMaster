package com.opensearch.queries;

import org.springframework.beans.factory.annotation.Autowired;
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
    public List<Product> findByCategory(@PathVariable String category) throws IOException {
        return productRepository.findByCategory(category);
    }

    // Terms selector tests
    @GetMapping("categories")
    public List<Product> findByCategories(@RequestParam List<String> cats) throws IOException {
        return productRepository.findByCategories(cats);
    }

    // Range selector tests
    @GetMapping("price-range")
    public List<Product> findByPriceRange(@RequestParam(required = false) Double min, 
                                         @RequestParam(required = false) Double max) throws IOException {
        return productRepository.findByPriceRange(min, max);
    }

    @GetMapping("rating-gte/{rating}")
    public List<Product> findByRatingGte(@PathVariable Integer rating) throws IOException {
        return productRepository.findByRatingGte(rating);
    }

    // Prefix selector tests
    @GetMapping("name-prefix/{prefix}")
    public List<Product> findByNamePrefix(@PathVariable String prefix) throws IOException {
        return productRepository.findByNamePrefix(prefix);
    }

    // Exists selector tests
    @GetMapping("with-email")
    public List<Product> findWithEmail() throws IOException {
        return productRepository.findWithEmail();
    }

    // Fuzzy selector tests
    @GetMapping("name-fuzzy/{name}")
    public List<Product> findByNameFuzzy(@PathVariable String name, 
                                        @RequestParam(defaultValue = "2") Integer fuzziness) throws IOException {
        return productRepository.findByNameFuzzy(name, fuzziness);
    }

    // Wildcard selector tests
    @GetMapping("name-wildcard/{pattern}")
    public List<Product> findByNameWildcard(@PathVariable String pattern) throws IOException {
        return productRepository.findByNameWildcard(pattern);
    }

    // Regexp selector tests
    @GetMapping("email-pattern/{pattern}")
    public List<Product> findByEmailPattern(@PathVariable String pattern) throws IOException {
        return productRepository.findByEmailPattern(pattern);
    }

    // Match selector tests
    @GetMapping("description-match/{text}")
    public List<Product> findByDescriptionMatch(@PathVariable String text) throws IOException {
        return productRepository.findByDescriptionMatch(text);
    }

    // Bool selector tests
    @GetMapping("complex")
    public List<Product> findByComplexQuery(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) Double minPrice,
                                           @RequestParam(required = false) Boolean inStock) throws IOException {
        return productRepository.findByComplexQuery(category, minPrice, inStock);
    }

    // IDs selector tests
    @GetMapping("by-ids")
    public List<Product> findByIds(@RequestParam List<String> ids) throws IOException {
        return productRepository.findByIds(ids);
    }
}
