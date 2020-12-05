using System.Net.Http;
using System.Threading.Tasks;
using RestApis.Tests.HelloWorld.Controller;
using Xunit;

namespace RestApis.Tests.HelloWorld {
    public class HelloWorldTest {
        static readonly HttpClient client = new HttpClient ();

        [Fact]
        public async Task StartApi_RetrunSuccess () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var baseUrl = await evoMasterController.StartSutAsync ();

            var response = await client.GetAsync ($"{baseUrl}/helloworld");

            evoMasterController.StopSut ();

            Assert.Equal (200, (int) response.StatusCode);
        }

        [Fact]
        public async Task StartApiWithWrongUri_RetrunNotFound () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var baseUrl = await evoMasterController.StartSutAsync ();

            var response = await client.GetAsync ($"{baseUrl}/wrongUri");

            evoMasterController.StopSut ();

            Assert.Equal (404, (int) response.StatusCode);
        }

        [Fact]
        public async Task CallApiWhenStopped_Fail () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var baseUrl = await evoMasterController.StartSutAsync ();

            evoMasterController.StopSut ();

            await Assert.ThrowsAsync<HttpRequestException> (async () => await client.GetAsync ($"{baseUrl}/helloworld"));
        }

        [Fact]
        public async Task StartApi_IsSutRunningShouldReturnTrue () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            await evoMasterController.StartSutAsync ();

            Assert.True (evoMasterController.IsSutRunning ());

            evoMasterController.StopSut ();
        }

        [Fact]
        public async Task StartAndStopApi_IsSutRunningShouldReturnFalse () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            await evoMasterController.StartSutAsync ();

            evoMasterController.StopSut ();
            
            Assert.False (evoMasterController.IsSutRunning ());
        }
        [Fact]
        public async Task CheckSwaggerWhenStarted_ReturnSuccess () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var baseUrl = await evoMasterController.StartSutAsync ();
            
            var response = await client.GetAsync ($"{baseUrl}/swagger");

            evoMasterController.StopSut ();
            
            Assert.Equal (200, (int) response.StatusCode);
        }
    }
}