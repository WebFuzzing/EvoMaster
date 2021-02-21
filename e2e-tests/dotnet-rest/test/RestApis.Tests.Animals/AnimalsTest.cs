using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;
using Newtonsoft.Json;
using RestApis.Animals;
using Xunit;

namespace RestApis.Tests.Animals
{
    public class AnimalsTest
    {
        private static readonly HttpClient Client = new HttpClient();
        
        [Fact]
        public async Task Test_200()
        {
            var baseUrlOfSut = "http://localhost:5000";
            HttpResponseMessage response = await Client.GetAsync($"{baseUrlOfSut}/animals");
            string responseBody = await response.Content.ReadAsStringAsync();
            
            //just to show how deserialization can be done
            var body = (JsonConvert.DeserializeObject<IEnumerable<Animal>>(responseBody) ?? Array.Empty<Animal>()).ToList();
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json",response.Content.Headers.ContentType.ToString());
            Assert.Contains("Giraffe", responseBody);
        }
        
        [Fact]
        public async Task Test_404()
        {
            var baseUrlOfSut = "http://localhost:5000";
            HttpResponseMessage response = await Client.GetAsync($"{baseUrlOfSut}/mammals");
            string responseBody = await response.Content.ReadAsStringAsync();
            var body = (JsonConvert.DeserializeObject<IEnumerable<Animal>>(responseBody) ?? Array.Empty<Animal>()).ToList();
            
            Assert.Equal(404, (int) response.StatusCode);
            Assert.Null(response.Content.Headers.ContentType);
        }
    }
}
