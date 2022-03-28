using System;
using Microsoft.AspNetCore.Mvc;

namespace RestApis.StringEquals.Controllers {
    [ApiController]
    [Route("[controller]")]
    public class EqualsController : ControllerBase {
        [HttpGet("getConstant/{value}")]
        public IActionResult GetConstant(string value){
            if (value.Equals("Hello world!!! Even if this is a long string, it will be trivial to cover with taint analysis")){
                return StatusCode(200, "CONSTANT_OK");;
            }
            return StatusCode(400, "CONSTANT_FAIL");
        }
    }
}