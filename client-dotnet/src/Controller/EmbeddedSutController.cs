using System;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using Controller.Api;

namespace Controller {
  public abstract class EmbeddedSutController : SutController {

    public override sealed UnitsInfoDto GetUnitsInfoDto () {
      throw new System.NotImplementedException ();
    }

    public override sealed bool IsInstrumentationActivated () => false;

    public override sealed void NewActionSpecificHandler (ActionDto dto) {
      throw new System.NotImplementedException ();
    }

    public override sealed void NewSearch () {
      throw new System.NotImplementedException ();
    }

    public override sealed void NewTestSpecificHandler () {
      throw new System.NotImplementedException ();
    }

    protected void WaitUntilSutIsRunning (int port) {

      using (TcpClient tcpClient = new TcpClient ()) {

        while (true) {
          try {
            tcpClient.Connect ("127.0.0.1", port);
            break;
          } catch (Exception) {
            Thread.Sleep(50);
            continue;
          }
        }
      }
    }
  }
}