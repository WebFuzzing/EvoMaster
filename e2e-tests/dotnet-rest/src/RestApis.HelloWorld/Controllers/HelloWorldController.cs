using Microsoft.AspNetCore.Mvc;

namespace RestApis.HelloWorld.Controllers {
    [ApiController]
    [Route("[controller]")]
    public class HelloWorldController : ControllerBase {
        [HttpGet]
        public IActionResult Get() => Ok("Hello World");
    }
}