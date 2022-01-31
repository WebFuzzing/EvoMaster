using System;
using Microsoft.AspNetCore.Mvc;

namespace RestApis.StringEquals.Controllers {
    [ApiController]
    [Route("[controller]")]
    public class EqualsController : ControllerBase {
        [HttpGet("getConstant/{value}")]
        public IActionResult GetConstant(string value) => Ok(value);
    }
}