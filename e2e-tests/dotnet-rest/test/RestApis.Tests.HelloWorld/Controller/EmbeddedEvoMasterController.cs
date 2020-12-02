using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using Controller;
using Controller.Api;
using Controller.Problem;

namespace RestApis.Tests.HelloWorld.Controller {

    //TODO: It is not actually embedded as I run the dll to start
    public class EmbeddedEvoMasterController : EmbeddedSutController {
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

        public override IProblemInfo GetProblemInfo () {
            throw new System.NotImplementedException ();
        }

        public override UnitsInfoDto GetUnitsInfoDto () {
            throw new System.NotImplementedException ();
        }

        public override bool IsInstrumentationActivated () {
            throw new System.NotImplementedException ();
        }

        public override bool IsSutRunning () {
            throw new System.NotImplementedException ();
        }

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
        public override async Task<string> StartSutAsync (string port) {

            //TODO: Remove hardcoded path
            // var process = "dotnet ../../../../../src/RestApis.HelloWorld/bin/Debug/netcoreapp3.1/RestApis.HelloWorld.dll".Bash ();

            var task = Task.Run (() => {

                RestApis.HelloWorld.Program.Main (new string[] { port });
            });

            await Task.Delay (1000);

            return port;
        }

        public override void StopSut (string port) {

            // process.Kill (true);

            RestApis.HelloWorld.Program.Shutdown (port);
        }
    }
}