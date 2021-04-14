using System;
using System.Data.Common;
using System.Threading.Tasks;

namespace EvoMaster.DatabaseStarter.Abstractions
{
    public interface IDatabaseStarter
    {
        Task<(string, DbConnection)> StartAsync(string databaseName, int port, int timeout = 60);

        protected static string GetDockerUri()
        {
            return IsRunningOnWindows()
                ? "npipe://./pipe/docker_engine"
                : "unix:///var/run/docker.sock";

            static bool IsRunningOnWindows()
            {
                return Environment.OSVersion.Platform == PlatformID.Win32NT;
            }
        }
    }
}