using System;
using System.Linq;
using Xunit;
using System.Net.Http;
using EvoMaster.Controller;
using Newtonsoft.Json;

namespace RestApis.Tests.ForAssertions {


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
    
    public class ForAssertionsManualTest : IClassFixture<EMFixture> {
    
        private static readonly HttpClient Client = new HttpClient();

        private  EMFixture fixture;
        
        public ForAssertionsManualTest(EMFixture fixture) {
            this.fixture = fixture;
            fixture.controller.ResetStateOfSut();
        }


        [Fact]
        public async void TestGetData() {

            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/data");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());

            Assert.True(body.a == 42);
            Assert.True(body.b == "hello");
            Assert.True(body.c.Count == 3);
            Assert.True(body.c[0] == 1000);
            Assert.True(body.c[1] == 2000);
            Assert.True(body.c[2] == 3000);
            Assert.True(body.d.e == 66);
            Assert.True(body.d.f == "bar");
            Assert.True(body.d.g.h.Count == 2);
            Assert.True(body.d.g.h[0] == "xvalue");
            Assert.True(body.d.g.h[1] == "yvalue");
            Assert.True(body.i == true);
            Assert.True(body.l == false);
        }

        [Fact]
        public async void TestPostData() {
            
            
            var response = await Client.PostAsync($"{fixture.baseUrlOfSut}/data", null);
            
            Assert.Equal(201, (int) response.StatusCode);
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());

            Assert.True(body == null);
        }
        
        
        [Fact]
        public async void TestSimpleNumber() {
            
            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/simpleNumber");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());
            
            Assert.Equal(42, body);
        }

        [Fact]
        public async void TestSimpleString() {
            
            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/simpleString");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());
            
            Assert.True(body == "simple-string");
        }
        
        [Fact]
        public async void TestSimpleText() {
            
            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/simpleText");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("text/plain", response.Content.Headers.GetValues("Content-Type").First());
            
            var body = await response.Content.ReadAsStringAsync();
            
            Assert.True(body == "simple-text");
        }
        
        [Fact]
        public async void TestArray() {
            
            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/array");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());
            
            Assert.True(body.Count == 2);
            Assert.True(body[0] == 123);
            Assert.True(body[1] == 456);
        }
        
        [Fact]
        public async void TestArrayObject() {
            
            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/arrayObject");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());
            
            Assert.True(body.Count == 2);
            Assert.True(body[0].x == 777);
            Assert.True(body[1].x == 888);
        }
        
        [Fact]
        public async void TestArrayEmpty() {
            
            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/arrayEmpty");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());
            
            Assert.True(body.Count == 0);
        }
        
        [Fact]
        public async void TestObjectEmpty() {
            
            var response = await Client.GetAsync($"{fixture.baseUrlOfSut}/objectEmpty");
            
            Assert.Equal(200, (int) response.StatusCode);
            Assert.Contains("application/json", response.Content.Headers.GetValues("Content-Type").First());
            
            dynamic body = JsonConvert.DeserializeObject(await response.Content.ReadAsStringAsync());
            
            Assert.True(body.ToString() == "{}");
        }
    }
}