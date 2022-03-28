using System;
using System.Linq;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using RestApis.Animals.Entities;

namespace RestApis.Animals {
    public static class SeedData {
        public static void Initialize(IServiceProvider serviceProvider) {
            using var context = new AnimalsDbContext(
                serviceProvider.GetRequiredService<
                    DbContextOptions<AnimalsDbContext>>());
            if (context.Mammals.Any()) {
                return; // DB has been seeded
            }

            context.Mammals.AddRange(
                new Mammal { Name = "Giraffe" },
                new Mammal { Name = "Horse" },
                new Mammal { Name = "Human" },
                new Mammal { Name = "Cat" }
            );

            context.SaveChanges();
        }
    }
}