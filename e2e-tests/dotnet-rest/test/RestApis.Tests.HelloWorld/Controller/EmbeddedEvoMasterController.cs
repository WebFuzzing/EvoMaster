using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using Controller;
using Controller.Api;
using Controller.Problem;

namespace RestApis.Tests.HelloWorld.Controller {

    public class EmbeddedEvoMasterController : EmbeddedSutController {

        private bool isSutRunning;
        private int sutPort;

        public static void Main (string[] args) {

            System.Console.WriteLine ("Driver is starting...\n");

            EmbeddedEvoMasterController embeddedEvoMasterController = new EmbeddedEvoMasterController ();

            InstrumentedSutStarter instrumentedSutStarter = new InstrumentedSutStarter (embeddedEvoMasterController);

            instrumentedSutStarter.Start ();
        }

        public override string GetDatabaseDriverName () => null;

        public override List<AuthenticationDto> GetInfoForAuthentication () => null;

        public override string GetPackagePrefixesToCover () => "RestApis.HelloWorld";

        //TODO: later on we should create sth specific for C#
        public override OutputFormat GetPreferredOutputFormat () => OutputFormat.JAVA_JUNIT_5;

        //TODO: check again
        public override IProblemInfo GetProblemInfo () {
            return new RestProblem ("http://localhost:" + GetSutPort () + "/swagger", null);
        }

        public override bool IsSutRunning () => isSutRunning;

        public override void ResetStateOfSut () {
            throw new System.NotImplementedException ();
        }

        //This method in java client is not async
        public override async Task<string> StartSutAsync () {

            //TODO: check this again
            int ephemeralPort = GetEphemeralTcpPort ();

            var task = Task.Run (() => {

                RestApis.HelloWorld.Program.Main (new string[] { ephemeralPort.ToString () });
            });

            await WaitUntilSutIsRunningAsync (ephemeralPort);

            sutPort = ephemeralPort;

            isSutRunning = true;

            return $"http://localhost:{ephemeralPort}";
        }

        public override void StopSut () {

            RestApis.HelloWorld.Program.Shutdown ();

            isSutRunning = false;
        }

        protected int GetSutPort () => sutPort;
    }
}