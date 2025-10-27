using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.AspNetCore;

namespace RestApis.StringEquals
{
    public class Program {
        private static ConcurrentDictionary<int, CancellationTokenSource> tokens =
            new ConcurrentDictionary<int, CancellationTokenSource>();

        public static void Main(string[] args) {
            if (args.Length > 0) {
                var port = Convert.ToInt32(args[0]);

                tokens.TryAdd(port, new CancellationTokenSource());

                var host = CreateWebHostBuilder(args).Build();

                host.RunAsync(tokens[port].Token).GetAwaiter().GetResult();
            }
            else {
                var host = CreateWebHostBuilder(args).Build();

                host.RunAsync().GetAwaiter().GetResult();
            }
        }

        public static IWebHostBuilder CreateWebHostBuilder(string[] args) {
            var webHostBuilder = WebHost.CreateDefaultBuilder(args)
                .UseStartup<Startup>();

            return args.Length > 0 ? webHostBuilder.UseUrls($"http://*:{args[0]}") : webHostBuilder;
        }

        public static void Shutdown() {
            foreach (var pair in tokens) {
                pair.Value.Cancel();
            }

            tokens.Clear();
        }
    }
}
