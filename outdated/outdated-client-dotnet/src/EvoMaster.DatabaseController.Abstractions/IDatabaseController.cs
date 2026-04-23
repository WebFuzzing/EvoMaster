using System;
using System.Data.Common;
using System.Threading.Tasks;

namespace EvoMaster.DatabaseController.Abstractions {
    public interface IDatabaseController {
        public string DatabaseName { get; }
        public int Port { get; }
        public int Timeout { get; }
        public string Password { get; }
        Task<(string, DbConnection)> StartAsync();
        Task StopAsync();
        void Stop();

        protected static string GetDockerUri() {
            return IsRunningOnWindows()
                ? "npipe://./pipe/docker_engine"
                : "unix:///var/run/docker.sock";

            static bool IsRunningOnWindows() {
                return Environment.OSVersion.Platform == PlatformID.Win32NT;
            }
        }
    }
}