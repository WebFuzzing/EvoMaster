using System;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.OpenApi.Models;

namespace RestApis.Animals {
    public class Startup {
        public Startup(IConfiguration configuration) {
            Configuration = configuration;
        }

        public IConfiguration Configuration { get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        public void ConfigureServices(IServiceCollection services) {
            services.AddControllers();

            services.AddSwaggerGen(
                c => { c.SwaggerDoc("v1", new OpenApiInfo { Title = "Animals API", Version = "v1" }); });

            var connectionString = Configuration.GetValue<string>("ConnectionString") ??
                                   Configuration.GetConnectionString("LocalDb");

            services.AddDbContext<AnimalsDbContext>(options =>
                options.UseNpgsql(connectionString));
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IWebHostEnvironment env) {
            CreateDatabase(app);

            if (env.IsDevelopment()) {
                app.UseDeveloperExceptionPage();
            }

            //app.UseHttpsRedirection();

            app.UseRouting();

            app.UseAuthorization();

            app.UseSwagger();
            app.UseSwaggerUI(c => { c.SwaggerEndpoint("/swagger/v1/swagger.json", "HelloWorld API"); });

            app.UseEndpoints(endpoints => { endpoints.MapControllers(); });
        }

        private static void CreateDatabase(IApplicationBuilder app, bool seed = true) {
            using var serviceScope = app.ApplicationServices.GetService<IServiceScopeFactory>()?.CreateScope();

            if (serviceScope == null) return;

            var context = serviceScope.ServiceProvider.GetRequiredService<AnimalsDbContext>();

            context.Database.EnsureCreated();

            var serviceProvider = serviceScope.ServiceProvider;

            if (!seed) return;

            try {
                SeedData.Initialize(serviceProvider);
            }
            catch (Exception ex) {
                Console.ForegroundColor = ConsoleColor.Red;
                Console.WriteLine(ex);
                Console.ResetColor();
            }
        }
    }
}