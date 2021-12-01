using System;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;

namespace EvoMaster.Instrumentation {
    public static class Program {
        public static void Main(string[] args) {
            
            var services = new ServiceCollection();
            ConfigureServices(services);
            ServiceProvider serviceProvider = services.BuildServiceProvider();
            
            var instrumentator = serviceProvider.GetService<Instrumentator>();
            
            if (instrumentator == null) throw new NullReferenceException("Instrumentator is null");
            
            instrumentator.Instrument(args[0], args[1]);
        }

        private static void ConfigureServices(ServiceCollection services) {
            services.AddLogging(configure => configure.AddConsole())
                .AddSingleton<Instrumentator>();
        }
    }
}