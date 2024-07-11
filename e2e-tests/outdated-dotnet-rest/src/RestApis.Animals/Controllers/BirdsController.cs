using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using RestApis.Animals.Dtos;
using RestApis.Animals.Entities;

namespace RestApis.Animals.Controllers {
    [ApiController]
    [Route("[controller]")]
    public class BirdsController : ControllerBase {
        private readonly AnimalsDbContext _context;

        public BirdsController(AnimalsDbContext context) {
            _context = context;
        }

        [HttpGet]
        public async Task<IActionResult> GetAsync() {
            var birds = await _context.Birds.ToListAsync();

            return Ok(birds);
        }

        [HttpPost]
        public async Task<IActionResult> CreateAsync([FromBody] CreateAnimalDto dto) {
            await _context.Birds.AddAsync(new Bird { Name = dto.Name });

            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}