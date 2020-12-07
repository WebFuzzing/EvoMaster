using System;
using System.Net.Sockets;
using System.Threading.Tasks;

namespace Controller {
  public abstract class EmbeddedSutController : SutController {

    protected async Task WaitUntilSutIsRunningAsync (int port) {

      using (TcpClient tcpClient = new TcpClient ()) {

        while (true) {
          try {
            tcpClient.Connect ("127.0.0.1", port);
            break;
          } catch (Exception) {
            await Task.Delay (50);
            continue;
          }
        }
      }
    }
  }
}