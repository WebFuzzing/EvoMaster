using System.Collections.Generic;
using Controller;
using Controller.Api;

namespace E2E.HelloWorld {
  public class EmbeddedEvoMasterController : EmbeddedSutController {
    public static void Main (string[] args) {

      System.Console.WriteLine ("Driver is starting...\n");

      EmbeddedEvoMasterController embeddedEvoMasterController = new EmbeddedEvoMasterController ();

      InstrumentedSutStarter instrumentedSutStarter = new InstrumentedSutStarter (embeddedEvoMasterController);

      instrumentedSutStarter.Start ();
    }

    public override void ExecInsertionsIntoDatabase (IList<InsertionDto> insertions) {

      throw new System.NotImplementedException ();
    }

    public override void ResetStateOfSut () {
      throw new System.NotImplementedException ();
    }

    public override string StartSut () {
      throw new System.NotImplementedException ();
    }

    public override void StopSut () {
      throw new System.NotImplementedException ();
    }
  }
}