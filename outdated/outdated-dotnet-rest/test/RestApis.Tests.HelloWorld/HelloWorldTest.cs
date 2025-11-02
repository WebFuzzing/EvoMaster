using System.Net.Http;
using System.Threading.Tasks;
using Xunit;

namespace RestApis.Tests.HelloWorld {
    public class HelloWorldTest {
        private static readonly HttpClient Client = new HttpClient();


        [Theory]
        [InlineData("helloworld", 200)]
        [InlineData("swagger", 200)]
        [InlineData("wrongUri", 404)]
        public async Task StartApi_RetrunExpectedStatusCode(string uri, int expectedStatusCode) {
            var evoMasterController = new EmbeddedEvoMasterController();

            var baseUrl = evoMasterController.StartSut();

            var response = await Client.GetAsync($"{baseUrl}/{uri}");

            evoMasterController.StopSut();

            Assert.Equal(expectedStatusCode, (int)response.StatusCode);
        }

        [Fact]
        public async Task CallApiWhenStopped_Fail() {
            var evoMasterController = new EmbeddedEvoMasterController();

            var baseUrl = evoMasterController.StartSut();

            evoMasterController.StopSut();

            await Assert.ThrowsAsync<HttpRequestException>(async () => await Client.GetAsync($"{baseUrl}/helloworld"));
        }

        [Fact]
        public void StartApi_IsSutRunningShouldReturnTrue() {
            var evoMasterController = new EmbeddedEvoMasterController();

            evoMasterController.StartSut();

            Assert.True(evoMasterController.IsSutRunning());

            evoMasterController.StopSut();
        }

        [Fact]
        public void StartAndStopApi_IsSutRunningShouldReturnFalse() {
            var evoMasterController = new EmbeddedEvoMasterController();

            evoMasterController.StartSut();

            evoMasterController.StopSut();

            Assert.False(evoMasterController.IsSutRunning());
        }
    }
}