using System.Collections.Generic;
using System.Threading.Tasks;
using Controller;
using Controller.Api;
using Controller.Controllers.db;
using Controller.Problem;
using RestApis.Animals;

namespace RestApis.Tests.Animals.Controller
{
    public class EmbeddedEvoMasterController : EmbeddedSutController {

        private bool isSutRunning;
        private int sutPort;

        public static void Main (string[] args) {

            var embeddedEvoMasterController = new EmbeddedEvoMasterController ();

            var instrumentedSutStarter = new InstrumentedSutStarter (embeddedEvoMasterController);

            System.Console.WriteLine ("Driver is starting...\n");

            instrumentedSutStarter.Start ();
        }

        public override string GetDatabaseDriverName () => null;

        public override List<AuthenticationDto> GetInfoForAuthentication () => null;

        public override string GetPackagePrefixesToCover () => "RestApis.Animals";
        
        public override OutputFormat GetPreferredOutputFormat () => OutputFormat.CSHARP_XUNIT;

        //TODO: check again
        public override IProblemInfo GetProblemInfo () =>
            GetSutPort () != 0 ? new RestProblem ("http://localhost:" + GetSutPort () + "/swagger/v1/swagger.json", null) : new RestProblem (null, null);

        public override bool IsSutRunning () => isSutRunning;

        public override void ResetStateOfSut()
        {
           // DbCleaner.ClearDatabase_Postgres();
        }

        public override string StartSut () {

            //TODO: check this again
            var ephemeralPort = GetEphemeralTcpPort ();

            var task = Task.Run (() => {

                RestApis.Animals.Program.Main (new string[] { ephemeralPort.ToString () });
            });

            WaitUntilSutIsRunning (ephemeralPort);

            sutPort = ephemeralPort;

            isSutRunning = true;

            return $"http://localhost:{ephemeralPort}";
        }

        public override void StopSut () {

            RestApis.Animals.Program.Shutdown ();

            isSutRunning = false;
        }

        protected int GetSutPort () => sutPort;
    }
}