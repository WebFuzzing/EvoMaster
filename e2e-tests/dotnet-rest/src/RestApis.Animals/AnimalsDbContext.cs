using Microsoft.EntityFrameworkCore;
using RestApis.Animals.Entities;

namespace RestApis.Animals {
    public class AnimalsDbContext : DbContext {
        public AnimalsDbContext(DbContextOptions<AnimalsDbContext> getRequiredService) : base(getRequiredService) { }

        protected override void OnModelCreating(ModelBuilder modelBuilder) {
            modelBuilder.Entity<Mammal>().HasIndex(x => x.Id);
            modelBuilder.Entity<Mammal>().Property(x => x.Name).HasMaxLength(50);

            modelBuilder.Entity<Bird>().HasIndex(x => x.Id);
            modelBuilder.Entity<Bird>().Property(x => x.Name).HasMaxLength(50);
        }

        public DbSet<Mammal> Mammals { get; set; }
        public DbSet<Bird> Birds { get; set; }
    }
}