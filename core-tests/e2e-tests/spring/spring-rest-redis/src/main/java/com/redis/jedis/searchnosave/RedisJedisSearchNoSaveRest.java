package com.redis.jedis.searchnosave;

import com.redis.jedis.AbstractRedisJedisRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.search.schemafields.TagField;
import redis.clients.jedis.search.schemafields.TextField;

import java.util.Arrays;

/**
 * Unlike {@link com.redis.jedis.search.RedisJedisSearchRest}, none of these endpoints save any
 * data themselves: the queries are fixed, and the documents needed to satisfy them are expected
 * to come from EvoMaster's Redis data generation (based on the failed FT.SEARCH commands it
 * observes), not from a POST exposed by this controller.
 */
@RestController
@RequestMapping(path = "/redisjedissearchnosave")
public class RedisJedisSearchNoSaveRest extends AbstractRedisJedisRest {

    private static final String INDEX = "idx:peoplenosave";
    private static final String PREFIX = "personnosave:";

    @Override
    protected void ensureIndexes() {
        try {
            jedis.ftCreate(INDEX,
                    FTCreateParams.createParams().prefix(PREFIX),
                    Arrays.<SchemaField>asList(TextField.of("name"), NumericField.of("age"), TagField.of("street")));
        } catch (JedisDataException e) {
            if (!isIndexAlreadyExists(e)) {
                throw e;
            }
        }
    }

    @GetMapping("/searchByName")
    public ResponseEntity<Void> searchByName() {
        return search("@name:alice");
    }

    @GetMapping("/searchByStreet")
    public ResponseEntity<Void> searchByStreet() {
        return search("@street:{main}");
    }

    @GetMapping("/searchByAge")
    public ResponseEntity<Void> searchByAge() {
        return search("@age:[18 65]");
    }

    @GetMapping("/searchCombined")
    public ResponseEntity<Void> searchCombined() {
        return search("@name:alice @street:{main} @age:[18 65]");
    }

    private ResponseEntity<Void> search(String query) {
        long total = jedis.ftSearch(INDEX, query).getTotalResults();
        return total > 0 ? ResponseEntity.status(200).build() : ResponseEntity.status(404).build();
    }

}
