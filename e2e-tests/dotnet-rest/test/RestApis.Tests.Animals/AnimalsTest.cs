using System;
using System.Collections.Generic;
using System.Configuration;
using System.Linq;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Controller.Controllers.db;
using Microsoft.Extensions.Configuration;
using Newtonsoft.Json;
using Npgsql;
using RestApis.Animals;
using RestApis.Animals.Entities;
using Xunit;

namespace RestApis.Tests.Animals
{
    public class AnimalsTest
    {
        private static readonly HttpClient Client = new HttpClient();

        public AnimalsTest()
        {
            //TODO
            ResetDatabase();
        }
        [Fact]
        public async Task Test_200()
        {
            var baseUrlOfSut = "http://localhost:5000";
            HttpResponseMessage response = await Client.GetAsync($"{baseUrlOfSut}/mammals");
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
            var baseUrlOfSut = "http://localhost:5000";

            var response = await Client.GetAsync($"{baseUrlOfSut}/birds");
            var responseBody = await response.Content.ReadAsStringAsync();
            
            Assert.DoesNotContain("Eagle", responseBody);
            
            var dto = new Bird {Name = "Eagle"};
            var httpContent = new StringContent(JsonConvert.SerializeObject(dto), Encoding.UTF8, "application/json");
            response = await Client.PostAsync($"{baseUrlOfSut}/birds", httpContent);

            Assert.Equal(204, (int) response.StatusCode);
        }

        [Fact]
        public async Task Test_404()
        {
            var baseUrlOfSut = "http://localhost:5000";
            HttpResponseMessage response = await Client.GetAsync($"{baseUrlOfSut}/fishes");
            string responseBody = await response.Content.ReadAsStringAsync();
            var body = (JsonConvert.DeserializeObject<IEnumerable<Animal>>(responseBody) ?? Array.Empty<Animal>())
                .ToList();

            Assert.Equal(404, (int) response.StatusCode);
            Assert.Null(response.Content.Headers.ContentType);
        }
        
        //TODO
        private void ResetDatabase()
        {
            DbCleaner.ClearDatabase_Postgres(
                new NpgsqlConnection("Host=localhost;Database=AnimalsDb;Username=user;Password=password123"),
                new List<string> {"Mammals"});
        }
    }
}