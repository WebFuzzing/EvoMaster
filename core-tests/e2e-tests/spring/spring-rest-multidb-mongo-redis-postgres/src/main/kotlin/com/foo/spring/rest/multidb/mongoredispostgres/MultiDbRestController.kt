package com.foo.spring.rest.multidb.mongoredispostgres

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.persistence.EntityManager

@RestController
@RequestMapping(path = ["/api"])
 class MultiDbRestController {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var mongoPersons: MongoPersonRepository

    private lateinit var sync: RedisCommands<String, String>
    private lateinit var redisClient: RedisClient
    private lateinit var connection: StatefulRedisConnection<String, String>

    @PostConstruct
    open fun init() {
        val redisHost = System.getProperty("spring.redis.host", "localhost")
        val redisPort = System.getProperty("spring.redis.port", "6379")
        val redisUri = "redis://$redisHost:$redisPort"
        redisClient = RedisClient.create(redisUri)
        connection = redisClient.connect()
        sync = connection.sync()
    }

    @PreDestroy
    open fun shutdown() {
        connection.close()
        redisClient.shutdown()
    }

    // 1 GET endpoint reading postgres, mongo, and redis
    @GetMapping("/get/{idsql}/{idmongo}/{idredis}")
    open fun getCombined(@PathVariable idsql: Long,
                         @PathVariable idmongo: Long,
                         @PathVariable idredis: Long): ResponseEntity<CombinedDataDto> {
        val postgresQuery = em.createNativeQuery("select * from X where id = ?")
            .setParameter(1, idsql)
        val postgresList = postgresQuery.resultList
        val postgresFound = postgresList.isNotEmpty()

        val mongoList = mongoPersons.findByAge(idmongo.toInt())
        val mongoFound = mongoList.isNotEmpty()

        val redisVal = sync.get(idredis.toString())
        val redisFound = redisVal != null

        if (postgresFound && mongoFound && redisFound) {
            return ResponseEntity.ok(CombinedDataDto(postgresFound, mongoFound, redisFound))
        }

        return ResponseEntity.status(400).body(CombinedDataDto(postgresFound, mongoFound, redisFound))
    }

    // POST endpoint for Postgres
    @PostMapping("/postgres/{id}")
    @Transactional
    open fun postPostgres(@PathVariable id: Long): ResponseEntity<Void> {
        em.createNativeQuery("insert into X (id) values (?)")
            .setParameter(1, id)
            .executeUpdate()
        return ResponseEntity.status(200).build()
    }

    // POST endpoint for Mongo
    @PostMapping("/mongo/{age}")
    open fun postMongo(@PathVariable age: Int): ResponseEntity<Void> {
        val s = MongoPerson(age)
        mongoPersons.save(s)
        return ResponseEntity.status(200).build()
    }

    // POST endpoint for Redis
    @PostMapping("/redis/{key}")
    open fun postRedis(@PathVariable key: String): ResponseEntity<Void> {
        sync.set(key, "value")
        return ResponseEntity.status(200).build()
    }
}
