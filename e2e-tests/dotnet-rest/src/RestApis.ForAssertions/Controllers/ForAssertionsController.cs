using System;
using Microsoft.AspNetCore.Mvc;

namespace RestApis.ForAssertions.Controllers {
    
    [ApiController]
    public class ForAssertionsController : ControllerBase {

        [HttpGet]
        [Route("/data")]
        [Produces("application/json")]
        public IActionResult GetData() {

            var obj = new {
                a = 42,
                b = "hello",
                c = new int[]{1000, 2000, 3000},
                d = new {
                    e = 66,
                    f = "bar",
                    g = new {
                        h =  new string[]{"xvalue", "yvalue"}
                    }
                },
                i = true,
                l =  false
            };
            
            return Ok(obj);
        }

        [HttpPost]
        [Route("/data")]
        public IActionResult PostData() {
            return StatusCode(201);
        }
        
        
        [HttpGet]
        [Route("/simpleNumber")]
        [Produces("application/json")]
        public IActionResult GetSimpleNumber() {
            return Ok(42);
        }

        [HttpGet]
        [Route("/simpleString")]
        [Produces("application/json")]
        public IActionResult GetSimpleString() {
            return Ok("simple-string");
        }

        [HttpGet]
        [Route("/simpleText")]
        [Produces("text/plain")]
        public IActionResult GetSimpleText() {
            return Ok("simple-text");
        }

        
        [HttpGet]
        [Route("/array")]
        [Produces("application/json")]
        public IActionResult GetArray() {
            return Ok(new int[]{123, 456});
        }
        
        [HttpGet]
        [Route("/arrayObject")]
        [Produces("application/json")]
        public IActionResult GetArrayObject() {
            return Ok(new object[]{new {x=777}, new {x=888}});
        }
        
        [HttpGet]
        [Route("/arrayEmpty")]
        [Produces("application/json")]
        public IActionResult GetArrayEmpty() {
            return Ok(new int[0]);
        }
        
        [HttpGet]
        [Route("/objectEmpty")]
        [Produces("application/json")]
        public IActionResult GetObjectEmpty() {
            return Ok(new {});
        }
    }
}