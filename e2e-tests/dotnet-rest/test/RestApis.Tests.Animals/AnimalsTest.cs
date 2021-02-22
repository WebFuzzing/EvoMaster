using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Controller;
using Controller.Controllers.db;
using Microsoft.Extensions.Configuration;
using Newtonsoft.Json;
using Npgsql;
using RestApis.Animals;
using RestApis.Animals.Entities;
using Xunit;

namespace RestApis.Tests.Animals
{
    public class AnimalsTest : IClassFixture<ControllerFixture>
    {
        private static readonly HttpClient Client = new HttpClient();
        private ControllerFixture _fixture;
        public AnimalsTest(ControllerFixture fixture)
        {
            _fixture = fixture;
            _fixture.Controller.ResetStateOfSut();
        }
        [Fact]
        public async Task Test_200()
        {
            HttpResponseMessage response = await Client.GetAsync($"{_fixture.BaseUrlOfSut}/mammals");
            string responseBody = await response.Content.ReadAsStringAsync();

            //just to show how deserialization can be done
            var body = (JsonConvert.DeserializeObject<IEnumerable<Animal>>(responseBody) ?? Array.Empty<Animal>())
                .ToList();

            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.ContentType.ToString());
            Assert.Contains("Giraffe", responseBody);
        }

        [Fact]
        public async Task Test_Create()
        {
            var response = await Client.GetAsync($"{_fixture.BaseUrlOfSut}/birds");
            var responseBody = await response.Content.ReadAsStringAsync();
            
            Assert.DoesNotContain("Eagle", responseBody);
            
            var dto = new Bird {Name = "Eagle"};
            var httpContent = new StringContent(JsonConvert.SerializeObject(dto), Encoding.UTF8, "application/json");
            response = await Client.PostAsync($"{_fixture.BaseUrlOfSut}/birds", httpContent);

            Assert.Equal(204, (int) response.StatusCode);
        }

        [Fact]
        public async Task Test_404()
        {
            HttpResponseMessage response = await Client.GetAsync($"{_fixture.BaseUrlOfSut}/fishes");
            string responseBody = await response.Content.ReadAsStringAsync();
            var body = (JsonConvert.DeserializeObject<IEnumerable<Animal>>(responseBody) ?? Array.Empty<Animal>())
                .ToList();

            Assert.Equal(404, (int) response.StatusCode);
            Assert.Null(response.Content.Headers.ContentType);
        }
    }
    
    public class ControllerFixture : IDisposable {
    
        public ISutHandler Controller { get; private set; }
        public string BaseUrlOfSut { get; private set; }
    
        public ControllerFixture() {
        
            Controller = new RestApis.Tests.Animals.Controller.EmbeddedEvoMasterController();
            Controller.SetupForGeneratedTest();
            BaseUrlOfSut = Controller.StartSut ();
            Assert.NotNull(BaseUrlOfSut);
        }
    
        public void Dispose() {
            Controller.StopSut ();
        }
    }
}