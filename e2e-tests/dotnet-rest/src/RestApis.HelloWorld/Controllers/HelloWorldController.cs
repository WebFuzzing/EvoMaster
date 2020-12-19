using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;

namespace RestApis.HelloWorld.Controllers {

    [ApiController]
    [Route ("[controller]")]
    public class HelloWorldController : ControllerBase {

        [HttpGet]
        public IActionResult Get () => Ok ("Hello World");
    }
}