package com.foo.rest.examples.spring.adaptivehypermutation;

import com.p6spy.engine.spy.P6SpyDriver;
import kotlin.random.Random;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.db.DbCleaner;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.hibernate.dialect.H2Dialect;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** automatically created on 2020-10-22 */
public class AHypermutationRestController extends EmbeddedSutController {

  private List<String> skip = null;

  public AHypermutationRestController() {
    this(40100);
  }

  public AHypermutationRestController(int controllerPort) {
    setControllerPort(controllerPort);
  }

  public AHypermutationRestController(List<String> skip) {
    this.skip = skip;
  }
  protected ConfigurableApplicationContext ctx;
  private Connection connection;
  protected static InstrumentedSutStarter embeddedStarter;

  public static void main(String[] args) {
    int port = 40100;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }

    AHypermutationRestController controller = new AHypermutationRestController(port);
    InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

    starter.start();
  }

  @Override
  public ProblemInfo getProblemInfo() {
    return new RestProblem("http://localhost:" + getSutPort() + "/v2/api-docs", skip);
  }

  @Override
  public List<AuthenticationDto> getInfoForAuthentication() {
    return null;
  }

  @Override
  public SutInfoDto.OutputFormat getPreferredOutputFormat() {
    return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
  }

  protected int getSutPort() {
    return (Integer)
        ((Map) ctx.getEnvironment().getPropertySources().get("server.ports").getSource())
            .get("local.server.port");
  }

  @Override
  public String getPackagePrefixesToCover() {
    return "com.foo.rest.examples.spring.adaptivehypermutation.";
  }

  @Override
  public boolean isSutRunning() {
    return ctx != null && ctx.isRunning();
  }

  @Override
  public String getDatabaseDriverName() {
    return "org.h2.Driver";
  }

  @Override
  public Connection getConnection() {
    return connection;
  }

  @Override
  public String startSut() {
    int rand = Random.Default.nextInt();
    ctx =
        SpringApplication.run(
            ResApp.class,
            new String[] {
              "--server.port=0",
              "--spring.datasource.url=jdbc:p6spy:h2:mem:testdb_"+rand+";DB_CLOSE_DELAY=-1;",
              "--spring.datasource.driver-class-name=" + P6SpyDriver.class.getName(),
              "--spring.jpa.database-platform=" + H2Dialect.class.getName(),
              "--spring.datasource.username=sa",
              "--spring.datasource.password",
              "--spring.jpa.properties.hibernate.show_sql=true"
            });

    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

    try {
      connection = jdbc.getDataSource().getConnection();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    return "http://localhost:" + getSutPort();
  }

  @Override
  public void stopSut() {
    ctx.stop();
    ctx.close();
  }

  @Override
  public void resetStateOfSUT() {
    if(connection != null) {
      DbCleaner.clearDatabase_H2(connection);
    }
  }
}
