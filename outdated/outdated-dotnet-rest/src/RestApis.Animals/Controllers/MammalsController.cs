using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using RestApis.Animals.Dtos;
using RestApis.Animals.Entities;

namespace RestApis.Animals.Controllers {
    [ApiController]
    [Route("[controller]")]
    public class MammalsController : ControllerBase {
        private readonly AnimalsDbContext _context;

        public MammalsController(AnimalsDbContext context) {
            _context = context;
        }

        [HttpGet]
        public async Task<IActionResult> GetAsync() {
            var mammals = await _context.Mammals.ToListAsync();

            return Ok(mammals);
        }

        [HttpPost]
        public async Task<IActionResult> CreateAsync([FromBody] CreateAnimalDto dto) {
            await _context.Mammals.AddAsync(new Mammal { Name = dto.Name });

            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}