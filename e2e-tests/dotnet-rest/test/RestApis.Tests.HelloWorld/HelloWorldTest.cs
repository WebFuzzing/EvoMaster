using System.Net.Http;
using System.Threading.Tasks;
using RestApis.Tests.HelloWorld.Controller;
using Xunit;

namespace RestApis.Tests.HelloWorld
{
  public class HelloWorldTest {
        static readonly HttpClient client = new HttpClient ();

        [Fact]
        public async Task StartApi_RetrunSuccess () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var process = await evoMasterController.StartSutAsync ();

            var response = await client.GetAsync ("http://localhost:5000/helloworld");

            evoMasterController.StopSut (process);

            Assert.Equal (200, (int) response.StatusCode);
        }

        [Fact]
        public async Task StartApiWithWrongUri_RetrunNotFound () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var process = await evoMasterController.StartSutAsync ();

            var response = await client.GetAsync ("http://localhost:5000/wrongUri");

            evoMasterController.StopSut (process);

            Assert.Equal (404, (int) response.StatusCode);
        }

        [Fact]
        public async Task CallApiWhenFailed_Fail () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var process = await evoMasterController.StartSutAsync ();

            evoMasterController.StopSut (process);

            await Assert.ThrowsAsync<HttpRequestException> (async () => await client.GetAsync ("http://localhost:5000/helloworld"));
        }
    }
}