using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using EvoMaster.Controller;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Problem;

namespace RestApis.Tests.ForAssertions {

    public class EmbeddedEvoMasterController : EmbeddedSutController {

        private bool isSutRunning;
        private int sutPort;

        public static void Main (string[] args) {

            var controllerPort = 40100;
            if (args.Length > 0) {
                controllerPort = Int32.Parse(args[0]);
            }
            
            var embeddedEvoMasterController = new EmbeddedEvoMasterController ();
            embeddedEvoMasterController.SetControllerPort(controllerPort);
            
            var instrumentedSutStarter = new InstrumentedSutStarter (embeddedEvoMasterController);

            System.Console.WriteLine ($"Driver is starting on port {controllerPort}...\n");

            instrumentedSutStarter.Start ();
        }

        public override string GetDatabaseDriverName () => null;

        public override List<AuthenticationDto> GetInfoForAuthentication () => null;

        public override string GetPackagePrefixesToCover () => "RestApis.ForAssertions";
        
        public override OutputFormat GetPreferredOutputFormat () => OutputFormat.CSHARP_XUNIT;

        //TODO: check again
        public override IProblemInfo GetProblemInfo () =>
            GetSutPort () != 0 ? new RestProblem ("http://localhost:" + GetSutPort () + "/swagger/v1/swagger.json", null) : new RestProblem (null, null);

        public override bool IsSutRunning () => isSutRunning;

        public override void ResetStateOfSut () { }

        public override string StartSut () {

            //TODO: check this again
            var ephemeralPort = GetEphemeralTcpPort ();

            var task = Task.Run (() => {

                RestApis.ForAssertions.Program.Main (new string[] { ephemeralPort.ToString () });
            });

            WaitUntilSutIsRunning (ephemeralPort);

            sutPort = ephemeralPort;

            isSutRunning = true;

            return $"http://localhost:{ephemeralPort}";
        }

        public override void StopSut () {

            RestApis.ForAssertions.Program.Shutdown ();

            isSutRunning = false;
        }

        protected int GetSutPort () => sutPort;
    }
}