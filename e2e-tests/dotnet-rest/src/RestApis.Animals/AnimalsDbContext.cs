using Microsoft.EntityFrameworkCore;

namespace RestApis.Animals
{
    public class AnimalsDbContext : DbContext
    {
        public AnimalsDbContext(DbContextOptions<AnimalsDbContext> getRequiredService) : base(getRequiredService)
        {
        }

        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            modelBuilder.Entity<Animal>().HasIndex(x => x.Id);
            modelBuilder.Entity<Animal>().Property(x => x.Name).HasMaxLength(50);
        }

        public DbSet<Animal> Animals { get; set; }
    }
}