using System.Net.Http;
using System.Threading.Tasks;
using RestApis.Tests.HelloWorld.Controller;
using Xunit;

namespace RestApis.Tests.HelloWorld
{
  public class HelloWorldTest {
      //TODO: ports shouldn't be hardcoded, they should be allocated by the OS dynamically
        static readonly HttpClient client = new HttpClient ();

        [Fact]
        public async Task StartApi_RetrunSuccess () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var port = await evoMasterController.StartSutAsync ();

            var response = await client.GetAsync ($"http://localhost:{port}/helloworld");

            evoMasterController.StopSut (port);

            Assert.Equal (200, (int) response.StatusCode);
        }

        [Fact]
        public async Task StartApiWithWrongUri_RetrunNotFound () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var port = await evoMasterController.StartSutAsync ();

            var response = await client.GetAsync ($"http://localhost:{port}/wrongUri");

            evoMasterController.StopSut (port);

            Assert.Equal (404, (int) response.StatusCode);
        }

        [Fact]
        public async Task CallApiWhenStopped_Fail () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var port = await evoMasterController.StartSutAsync ();

            evoMasterController.StopSut (port);

            await Assert.ThrowsAsync<HttpRequestException> (async () => await client.GetAsync ($"http://localhost:{port}/helloworld"));
        }
    }
}