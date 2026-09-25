package com.cassandra.findbyuuid;

import com.cassandra.SwaggerConfiguration;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.net.InetSocketAddress;

@EnableSwagger2
@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
public class CassandraFindByUuidApp extends SwaggerConfiguration {

    public static final String KEYSPACE = "evomaster_e2e";
    public static final String TABLE = "record_by_id";

    public CassandraFindByUuidApp() {
        super("cassandrafindbyuuid");
    }

    public static void main(String[] args) {
        SpringApplication.run(CassandraFindByUuidApp.class, args);
    }

    @Bean
    public CqlSession cqlSession(@Value("${cassandra.host}") String host,
                                 @Value("${cassandra.port}") int port) {

        CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withLocalDatacenter("datacenter1")
                .build();

        session.execute("CREATE KEYSPACE IF NOT EXISTS " + KEYSPACE +
                " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
        /*
            The payload carries only the types no other scenario covers, as each column is one more
            gene to generate with no bearing on the query, which the key alone decides. Note that a
            duration cannot be part of a primary key in Cassandra, so it can only be a payload column.
         */
        session.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + "." + TABLE +
                " (id uuid PRIMARY KEY, created timestamp, elapsed duration, ip inet, tags set<text>)");

        return session;
    }
}