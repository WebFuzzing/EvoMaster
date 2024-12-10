package com.foo.rest.examples.spring.db.preparedstatement;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.PreparedStatementClassReplacement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import java.sql.*;
import java.util.List;

@RestController
@RequestMapping(path = "/api/db/preparedStatement")
public class PreparedStatementRest {


    @Autowired
    private DataSource dataSource;

    private  Connection connection;

    @PostConstruct
    public void init(){
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @RequestMapping(
            method = RequestMethod.POST
    )
    public void post() {

        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO Foo (integervalue, stringvalue, booleanvalue) VALUES(?, ?, ?)");
            statement.setInt(1, 42);
            statement.setString(2, "BAR");
            statement.setBoolean(3, false);

            PreparedStatementClassReplacement.executeUpdate(statement);

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    @RequestMapping(
            path = "/{integerValue}/{stringValue}/{booleanValue}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON
    )
    public ResponseEntity<Void> get(@PathVariable("integerValue") int x, @PathVariable("stringValue") String y, @PathVariable("booleanValue") boolean z) {

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM Foo WHERE integerValue=? AND stringValue=?");
            statement.setInt(1, x);
            statement.setString(2, y);
            //statement.setBoolean(3, z);

            ResultSet results = PreparedStatementClassReplacement.executeQuery(statement);

            // check if any results are returned
            if (results.next()) {
                return ResponseEntity.status(200).build();
            } else {
                return ResponseEntity.status(400).build();
            }
        }
        catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }
}