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
        public async Task StartApi_RetrunExpectedStatusCode (string uri, int expectedStatusCode) {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var baseUrl = evoMasterController.StartSut ();

            var response = await client.GetAsync ($"{baseUrl}/{uri}");

            evoMasterController.StopSut ();

            Assert.Equal (expectedStatusCode, (int) response.StatusCode);
        }

        [Fact]
        public async Task CallApiWhenStopped_Fail () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            var baseUrl = evoMasterController.StartSut ();

            evoMasterController.StopSut ();

            await Assert.ThrowsAsync<HttpRequestException> (async () => await client.GetAsync ($"{baseUrl}/helloworld"));
        }

        [Fact]
        public void StartApi_IsSutRunningShouldReturnTrue () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            evoMasterController.StartSut ();

            Assert.True (evoMasterController.IsSutRunning ());

            evoMasterController.StopSut ();
        }

        [Fact]
        public void StartAndStopApi_IsSutRunningShouldReturnFalse () {

            EmbeddedEvoMasterController evoMasterController = new EmbeddedEvoMasterController ();

            evoMasterController.StartSut ();

            evoMasterController.StopSut ();

            Assert.False (evoMasterController.IsSutRunning ());
        }
    }
}