package com.foo.rest.examples.spring.adaptivehypermutation;

import com.foo.rest.examples.spring.db.SpringWithDbController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import java.util.List;

public class AHypermutationRestController extends SpringWithDbController {

  private List<String> skip = null;

  public AHypermutationRestController() {
    this(40100);
  }

  public AHypermutationRestController(int controllerPort) {
    super(AWHResApp.class);
    setControllerPort(controllerPort);
  }

  public AHypermutationRestController(List<String> skip) {
    super(AWHResApp.class);
    this.skip = skip;
  }

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

}
