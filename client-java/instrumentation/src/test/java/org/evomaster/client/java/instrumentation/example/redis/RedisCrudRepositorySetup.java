package org.evomaster.client.java.instrumentation.example.redis;

import com.foo.somedifferentpackage.examples.methodreplacement.subclass.RedisCrudRepository;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.convert.RedisCustomConversions;
import org.springframework.data.redis.core.mapping.RedisMappingContext;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.repository.support.KeyValueRepositoryFactory;

public class RedisCrudRepositorySetup {

    public static RedisCrudRepository createRepository(RedisTemplate<RedisEntity, String> redisTemplate) {
        RedisMappingContext mappingContext = new RedisMappingContext();
        mappingContext.setSimpleTypeHolder(new RedisCustomConversions().getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        RedisKeyValueAdapter adapter = new RedisKeyValueAdapter(redisTemplate, mappingContext);
        KeyValueOperations kvOperations = new RedisKeyValueTemplate(adapter, mappingContext);
        KeyValueRepositoryFactory factory = new KeyValueRepositoryFactory(kvOperations);
        return factory.getRepository(RedisCrudRepository.class);
    }
}
