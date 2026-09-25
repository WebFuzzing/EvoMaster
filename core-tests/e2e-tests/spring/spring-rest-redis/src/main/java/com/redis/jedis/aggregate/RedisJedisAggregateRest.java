package com.redis.jedis.aggregate;

import com.redis.jedis.AbstractRedisJedisRest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.aggr.AggregationBuilder;
import redis.clients.jedis.search.aggr.Reducers;
import redis.clients.jedis.search.schemafields.NumericField;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.search.schemafields.TagField;
import redis.clients.jedis.search.schemafields.TextField;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/redisjedisaggregate")
public class RedisJedisAggregateRest extends AbstractRedisJedisRest {

    private static final String INDEX = "idx:items";
    private static final String PREFIX = "item:";

    /**
     * Index definitions do not survive a flush of the database, so they are made sure to exist
     * before any operation relies on them.
     */
    private void ensureIndexes() {
        try {
            jedis.ftCreate(INDEX,
                    FTCreateParams.createParams().prefix(PREFIX),
                    Arrays.<SchemaField>asList(
                            TextField.of("name"),
                            TagField.of("category").sortable(),
                            NumericField.of("price").sortable()));
        } catch (JedisDataException e) {
            if (!isIndexAlreadyExists(e)) {
                throw e;
            }
        }
    }

    @PostMapping("/item/{name}/{category}/{price}")
    public ResponseEntity<Void> saveItem(@PathVariable String name, @PathVariable String category, @PathVariable int price) {
        ensureIndexes();
        Map<String, String> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("category", category);
        fields.put("price", String.valueOf(price));
        jedis.hset(PREFIX + name, fields);
        return ResponseEntity.status(200).build();
    }

    @GetMapping("/aggregate/countByCategory/{name}")
    public ResponseEntity<Void> countByCategory(@PathVariable String name) {
        return aggregate(new AggregationBuilder("@name:" + name)
                .groupBy("@category", Reducers.count().as("count")));
    }

    @GetMapping("/aggregate/avgPriceByCategory/{min}/{max}")
    public ResponseEntity<Void> avgPriceByCategory(@PathVariable int min, @PathVariable int max) {
        return aggregate(new AggregationBuilder("@price:[" + min + " " + max + "]")
                .groupBy("@category", Reducers.avg("@price").as("avgPrice")));
    }

    private ResponseEntity<Void> aggregate(AggregationBuilder aggregation) {
        ensureIndexes();
        try {
            boolean hasGroups = !jedis.ftAggregate(INDEX, aggregation).getRows().isEmpty();
            return hasGroups ? ResponseEntity.status(200).build() : ResponseEntity.status(404).build();
        } catch (JedisDataException e) {
            // Redis rejects queries whose values contain characters with special meaning
            return ResponseEntity.status(400).build();
        }
    }

}
