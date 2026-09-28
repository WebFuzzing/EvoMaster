package com.redis.jedis.search;

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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/redisjedissearch")
public class RedisJedisSearchRest extends AbstractRedisJedisRest {

    private static final String INDEX = "idx:people";
    private static final String PREFIX = "person:";

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

    @PostMapping("/person/{name}/{age}/{street}")
    public ResponseEntity<Void> savePerson(@PathVariable String name, @PathVariable int age, @PathVariable String street) {
        Map<String, String> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("age", String.valueOf(age));
        fields.put("street", street);
        jedis.hset(PREFIX + name, fields);
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/search/name/{name}")
    public ResponseEntity<Void> searchByName(@PathVariable String name) {
        return search("@name:" + name);
    }

    @GetMapping("/search/street/{street}")
    public ResponseEntity<Void> searchByStreet(@PathVariable String street) {
        return search("@street:{" + street + "}");
    }

    @GetMapping("/search/age/{min}/{max}")
    public ResponseEntity<Void> searchByAge(@PathVariable int min, @PathVariable int max) {
        return search("@age:[" + min + " " + max + "]");
    }

    private ResponseEntity<Void> search(String query) {
        try {
            long total = jedis.ftSearch(INDEX, query).getTotalResults();
            return total > 0 ? ResponseEntity.status(200).build() : ResponseEntity.status(404).build();
        } catch (JedisDataException e) {
            // Redis rejects queries whose values contain characters with special meaning
            return ResponseEntity.status(400).build();
        }
    }

}
