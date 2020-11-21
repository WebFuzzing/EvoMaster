using System.Collections.Generic;
using Controller.Api;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Hosting;

namespace Controller
{
  public abstract class SutController {
        private int controllerPort = ControllerConstants.DEFAULT_CONTROLLER_PORT;
        private string controllerHost = ControllerConstants.DEFAULT_CONTROLLER_HOST;

        public abstract void ExecInsertionsIntoDatabase (IList<InsertionDto> insertions);

        public abstract void ResetStateOfSut ();

        public abstract string StartSut ();

        public abstract void StopSut ();

        public bool StartTheControllerServer () {

            try {
                CreateHostBuilder ().Build ().Run ();
            } catch (System.Exception e) {
                System.Console.WriteLine ($"Failed to start web server. Check this error out:\n{e}");

                return false;
            }

            return true;
        }

        private IHostBuilder CreateHostBuilder () =>
            Host.CreateDefaultBuilder ()
            .ConfigureWebHostDefaults (webBuilder => {
                webBuilder.UseStartup<Startup> ();
            });
    }

    public class ConcreteController : SutController {
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