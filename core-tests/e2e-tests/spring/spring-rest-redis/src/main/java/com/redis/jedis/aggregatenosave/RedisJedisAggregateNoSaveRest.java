package com.redis.jedis.aggregatenosave;

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

/**
 * Unlike {@link com.redis.jedis.aggregate.RedisJedisAggregateRest}, none of these endpoints save
 * any data themselves: both aggregations only require a "category" field to exist on at least one
 * candidate document, which is expected to come from EvoMaster's Redis data generation.
 */
@RestController
@RequestMapping(path = "/redisjedisaggregatenosave")
public class RedisJedisAggregateNoSaveRest extends AbstractRedisJedisRest {

    private static final String INDEX = "idx:itemsnosave";
    private static final String PREFIX = "itemnosave:";

    @Override
    protected void ensureIndexes() {
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

    @GetMapping("/countByCategory")
    public ResponseEntity<Void> countByCategory() {
        return aggregate(new AggregationBuilder("*").groupBy("@category", Reducers.count().as("count")));
    }

    @GetMapping("/avgPriceByCategory")
    public ResponseEntity<Void> avgPriceByCategory() {
        return aggregate(new AggregationBuilder("*").groupBy("@category", Reducers.avg("@price").as("avgPrice")));
    }

    private ResponseEntity<Void> aggregate(AggregationBuilder aggregation) {
        boolean hasGroups = !jedis.ftAggregate(INDEX, aggregation).getRows().isEmpty();
        return hasGroups ? ResponseEntity.status(200).build() : ResponseEntity.status(404).build();
    }

}
