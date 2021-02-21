using System;
using System.Linq;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace RestApis.Animals
{
    public static class SeedData
    {
        public static void Initialize(IServiceProvider serviceProvider)
        {
            using var context = new AnimalsDbContext(
                serviceProvider.GetRequiredService<
                    DbContextOptions<AnimalsDbContext>>());
            
            if (context.Animals.Any())
            {
                return; // DB has been seeded
            }

            context.Animals.AddRange(
                new Animal("Giraffe"),
                new Animal("Horse")
            );
            
            context.SaveChanges();
        }
    }
}