using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using Controller;
using Controller.Api;

namespace RestApis.Tests.HelloWorld.Controller {
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

        public override async Task<Process> StartSutAsync () {

            //RestApis.HelloWorld.Program.Main (null);

            //TODO: Remove hardcoded path
            var process = "dotnet ../../../../../src/RestApis.HelloWorld/bin/Debug/netcoreapp3.1/RestApis.HelloWorld.dll".Bash ();

            await Task.Delay (1000);

            return process;
        }

        public override void StopSut (Process process) {

            // RestApis.HelloWorld.Program.Shutdown ();

            process.Kill (true);
        }
    }
}