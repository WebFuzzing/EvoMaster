package com.cassandra.findbyagereadonly;

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
public class CassandraFindByAgeReadOnlyApp extends SwaggerConfiguration {

    public static final String KEYSPACE = "evomaster_e2e";
    
    public static final String TABLE = "person_by_age_read_only";

    public CassandraFindByAgeReadOnlyApp() {
        super("cassandrafindbyagereadonly");
    }

    public static void main(String[] args) {
        SpringApplication.run(CassandraFindByAgeReadOnlyApp.class, args);
    }

    /**
     * The SUT uses the driver directly, with no Spring Data in between, which is the case the
     * instrumentation of {@code CqlSession} is designed around. The schema is created here, once, so
     * that clearing the data between tests can truncate the tables instead of dropping them.
     */
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
            age is the partition key, so a query on it needs no ALLOW FILTERING, and name completes
            the primary key so that several people can share an age. Both types have a gene, so
            EvoMaster can build an insertion for this table.
         */
        session.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + "." + TABLE +
                " (age int, name text, PRIMARY KEY (age, name))");

        return session;
    }
}