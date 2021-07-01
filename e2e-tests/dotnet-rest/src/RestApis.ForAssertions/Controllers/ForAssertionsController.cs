using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;

namespace RestApis.ForAssertions.Controllers {
    
    [ApiController]
    public class ForAssertionsController : ControllerBase {

        [HttpGet]
        [Route("/simpleNumber")]
        [Produces("application/json")]
        public IActionResult GetSimpleNumber() {
            return Ok(42);
        } 
    }
}