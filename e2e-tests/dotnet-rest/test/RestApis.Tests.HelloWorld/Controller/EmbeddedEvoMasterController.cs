using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using Controller;
using Controller.Api;
using Controller.Problem;

namespace RestApis.Tests.HelloWorld.Controller {
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

        //This method in java client is neither async, nor returning Process => String StartSut();
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