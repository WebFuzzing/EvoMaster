using System;
using System.Linq;
using System.Net.Http;
using System.Text;
using EvoMaster.Controller;
using Newtonsoft.Json;
using Xunit;

namespace RestApis.Tests.Crud {
    public class EMFixture : IDisposable {

        public ISutHandler controller { get; private set; }
        public string baseUrlOfSut { get; private set; }

        public EMFixture() {
            controller = new EmbeddedEvoMasterController();
            controller.SetupForGeneratedTest();
            baseUrlOfSut = controller.StartSut();
            Assert.NotNull(baseUrlOfSut);
        }

        public void Dispose() {
            controller?.StopSut();
        }

    }

    public class CrudManualTest : IClassFixture<EMFixture> {

        private static readonly HttpClient Client = new HttpClient();

        private EMFixture fixture;

        public CrudManualTest(EMFixture fixture) {
            this.fixture = fixture;
            fixture.controller.ResetStateOfSut();
        }


        [Fact]
        public async void TestGetData() {
            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/data");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());

            Assert.True(body[0] == "FOO");
        }

        [Fact]
        public async void TestPostData() {

            var payload = new StringContent("{\"x\":0}", Encoding.UTF8, "application/json");
            
            var response = await Client.PostAsync($"{fixture.baseUrlOfSut}/data", payload);
            
            Assert.Equal(201, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());

            Assert.True(body == "CREATED");
        }
        
        [Fact]
        public async void TestPutData() {

            var payload = new StringContent("{\"x\":0}", Encoding.UTF8, "application/json");
            
            var response = await Client.PutAsync($"{fixture.baseUrlOfSut}/data/42", payload);
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());

            Assert.True(body == "UPDATED");
        }
        
        [Fact]
        public async void TestPatchData() {

            var payload = new StringContent("{\"x\":0}", Encoding.UTF8, "application/json");
            
            var response = await Client.PatchAsync($"{fixture.baseUrlOfSut}/data/42", payload);
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());

            Assert.True(body == "PATCHED");
        }

        [Fact]
        public async void TestDeleteData() {
            
            var response = await Client.DeleteAsync($"{fixture.baseUrlOfSut}/data/42");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());

            Assert.True(body == "DELETED");
        }

        
    }
}