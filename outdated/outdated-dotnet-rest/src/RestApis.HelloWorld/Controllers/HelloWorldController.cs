using System;
using Microsoft.AspNetCore.Mvc;

namespace RestApis.HelloWorld.Controllers {
    [ApiController]
    [Route("[controller]")]
    public class HelloWorldController : ControllerBase {
        [HttpGet]
        public IActionResult Get() => Ok("HelloWorld");

        [HttpGet]
        [Route("{value}")]
        public IActionResult GetError([FromRoute] string value){
            var result = Int32.Parse(value);
            return Ok("int value");
        }
    }
}