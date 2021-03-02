using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using RestApis.Animals.Entities;

namespace RestApis.Animals.Controllers
{
    [ApiController]
    [Route("[controller]")]
    public class MammalsController : ControllerBase
    {
        private readonly AnimalsDbContext _context;

        public MammalsController(AnimalsDbContext context)
        {
            _context = context;
        }
        [HttpGet]
        public async Task<IActionResult> GetAsync()
        {
            var mammals = await _context.Mammals.ToListAsync();
            
            return Ok(mammals);
        }
        
        [HttpPost]
        public async Task<IActionResult> CreateAsync([FromBody] Mammal mammal)
        {
            await _context.Mammals.AddAsync(mammal);

            await _context.SaveChangesAsync();

            return NoContent();
        }
    }
}
