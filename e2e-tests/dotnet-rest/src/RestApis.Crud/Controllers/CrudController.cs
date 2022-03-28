using Microsoft.AspNetCore.Mvc;

namespace RestApis.Crud.Controllers {
    
    [ApiController]
    [Produces("application/json")]
    [Route("/data")]
    public class CrudController : ControllerBase  {
        
        
        [HttpGet]
        public IActionResult GetData() {
            return Ok(new string[]{"FOO"});
        }
        
        [HttpPost]
        [Consumes("application/json")]
        public IActionResult PostData([FromBody] Dto dto) {
            return Created("/data/42","CREATED");
        }

        [HttpPut]
        [Consumes("application/json")]
        [Route("{id:int}")]
        public IActionResult PutData([FromBody] Dto dto, [FromRoute] int id) {
            return StatusCode(200, "UPDATED");
        }

        [HttpPatch]
        [Consumes("application/json")]
        [Route("{id:int}")]
        public IActionResult PatchData([FromBody] Dto dto, [FromRoute] int id) {
            return StatusCode(200, "PATCHED");
        }

        [HttpDelete]
        [Route("{id:int}")]
        public IActionResult DeleteData([FromRoute] int id) {
            return StatusCode(200, "DELETED");
        }
  
    }
}