using System.Net.Http;
using System.Threading.Tasks;
using RestApis.Tests.HelloWorld.Controller;
using Xunit;

namespace RestApis.Tests.HelloWorld {
    public class HelloWorldTest {
        static readonly HttpClient client = new HttpClient ();

        [Theory]
        [InlineData ("helloworld", 200)]
        [InlineData ("swagger", 200)]
        [InlineData ("wrongUri", 404)]
        public async Task StartApi_RetrunExpectedStatusCodeAsync (string uri, int expectedStatusCode) {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var baseUrl = await evoMasterController.StartSutAsync ();

            var response = await client.GetAsync ($"{baseUrl}/{uri}");

            evoMasterController.StopSut ();

            Assert.Equal (expectedStatusCode, (int) response.StatusCode);
        }


        [Fact]
        public async Task CallApiWhenStopped_FailAsync () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var baseUrl = await evoMasterController.StartSutAsync ();

            evoMasterController.StopSut ();

            await Assert.ThrowsAsync<HttpRequestException> (async () => await client.GetAsync ($"{baseUrl}/helloworld"));
        }

        [Fact]
        public async Task StartApi_IsSutRunningShouldReturnTrueAsync () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            await evoMasterController.StartSutAsync ();

            Assert.True (evoMasterController.IsSutRunning ());

            evoMasterController.StopSut ();
        }

        [Fact]
        public async Task StartAndStopApi_IsSutRunningShouldReturnFalseAsync () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            await evoMasterController.StartSutAsync ();

            evoMasterController.StopSut ();

            Assert.False (evoMasterController.IsSutRunning ());
        }
    }
}