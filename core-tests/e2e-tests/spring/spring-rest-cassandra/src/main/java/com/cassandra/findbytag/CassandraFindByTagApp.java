package com.cassandra.findbytag;

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
public class CassandraFindByTagApp extends SwaggerConfiguration {

    public static final String KEYSPACE = "evomaster_e2e";
    public static final String TABLE = "session_by_id";

    public CassandraFindByTagApp() {
        super("cassandrafindbytag");
    }

    public static void main(String[] args) {
        SpringApplication.run(CassandraFindByTagApp.class, args);
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
        session.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + "." + TABLE +
                " (id uuid PRIMARY KEY, tags set<text>, props map<text, int>)");
        /*
            Cassandra only allows a CONTAINS on an indexed collection, and the index is part of the
            schema, so it is created here once, like the table.
         */
        session.execute("CREATE INDEX IF NOT EXISTS session_by_id_tags ON " + KEYSPACE + "." + TABLE + " (tags)");

        return session;
    }
}