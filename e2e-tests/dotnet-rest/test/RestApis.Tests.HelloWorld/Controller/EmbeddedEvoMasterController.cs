using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using Controller;
using Controller.Api;
using Controller.Problem;

namespace RestApis.Tests.HelloWorld.Controller {

    //TODO: It is not actually embedded as I run the dll to start
    public class EmbeddedEvoMasterController : EmbeddedSutController {

        private bool isSutRunning;
        private int sutPort;

        public static void Main (string[] args) {

            System.Console.WriteLine ("Driver is starting...\n");

            EmbeddedEvoMasterController embeddedEvoMasterController = new EmbeddedEvoMasterController ();

            InstrumentedSutStarter instrumentedSutStarter = new InstrumentedSutStarter (embeddedEvoMasterController);

            instrumentedSutStarter.Start ();
        }

        public override string GetDatabaseDriverName () {
            throw new System.NotImplementedException ();
        }

        public override List<AuthenticationDto> GetInfoForAuthentication () {
            throw new System.NotImplementedException ();
        }

        public override string GetPackagePrefixesToCover () {
            throw new System.NotImplementedException ();
        }

        public override OutputFormat GetPreferredOutputFormat () {
            throw new System.NotImplementedException ();
        }
        //TODO: check again
        public override IProblemInfo GetProblemInfo () {
           return new RestProblem("http://localhost:" + GetSutPort() + "/swagger", null);
        }

        public override UnitsInfoDto GetUnitsInfoDto () {
            throw new System.NotImplementedException ();
        }

        public override bool IsInstrumentationActivated () {
            throw new System.NotImplementedException ();
        }

        public override bool IsSutRunning () => isSutRunning;

        public override void NewActionSpecificHandler (ActionDto dto) {
            throw new System.NotImplementedException ();
        }

        public override void NewSearch () {
            throw new System.NotImplementedException ();
        }

        public override void NewTestSpecificHandler () {
            throw new System.NotImplementedException ();
        }

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

            await Task.Delay (300);

            sutPort = ephemeralPort;

            isSutRunning = true;

            return $"http://localhost:{ephemeralPort}";
        }

        public override void StopSut () {

            RestApis.HelloWorld.Program.Shutdown ();

            isSutRunning = false;
        }

        //TODO: we can remove this
        public override void StopSut (int port) {

            RestApis.HelloWorld.Program.Shutdown (port);

            isSutRunning = false;
        }

        protected int GetSutPort () => sutPort;
    }
}