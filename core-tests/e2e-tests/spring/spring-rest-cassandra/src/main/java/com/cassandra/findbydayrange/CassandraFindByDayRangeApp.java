package com.cassandra.findbydayrange;

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
public class CassandraFindByDayRangeApp extends SwaggerConfiguration {

    public static final String KEYSPACE = "evomaster_e2e";
    public static final String TABLE = "measurement_by_day";

    public CassandraFindByDayRangeApp() {
        super("cassandrafindbydayrange");
    }

    public static void main(String[] args) {
        SpringApplication.run(CassandraFindByDayRangeApp.class, args);
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
            day is the partition key and at a clustering column, which is the only shape letting a
            query range over a temporal column without ALLOW FILTERING.
         */
        session.execute("CREATE TABLE IF NOT EXISTS " + KEYSPACE + "." + TABLE +
                " (day date, at timestamp, value double, PRIMARY KEY (day, at))");

        return session;
    }
}