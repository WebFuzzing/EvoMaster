using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;
using Docker.DotNet;
using Docker.DotNet.Models;
using Microsoft.Data.SqlClient;

namespace EvoMaster.Controller.Controllers.db
{
    internal static class MsSqlDockerStarter
    {
        public const string SQLSERVER_SA_PASSWORD = "password123";
        public const string SQLSERVER_IMAGE = "mcr.microsoft.com/mssql/server";

        public const string SQLSERVER_IMAGE_TAG = "2017-CU14-ubuntu";

        // public const string SQLSERVER_IMAGE_TAG = "2017-latest";
        public const string SQLSERVER_CONTAINER_NAME_PREFIX = "EvoMasterTestsSql-";

        public static async Task<(string connectionString, SqlConnection connection)> StartDatabaseAsync(
            string databaseName, int timeout)
        {
            await CleanupRunningContainers();
            var dockerClient = GetDockerClient();
            var freePort = GetFreePort();

            // This call ensures that the latest SQL Server Docker image is pulled
            await dockerClient.Images.CreateImageAsync(new ImagesCreateParameters
            {
                FromImage = $"{SQLSERVER_IMAGE}:{SQLSERVER_IMAGE_TAG}"
            }, null, new Progress<JSONMessage>());

            var sqlContainer = await dockerClient
                .Containers
                .CreateContainerAsync(new CreateContainerParameters
                {
                    Name = SQLSERVER_CONTAINER_NAME_PREFIX + Guid.NewGuid(),
                    Image = $"{SQLSERVER_IMAGE}:{SQLSERVER_IMAGE_TAG}",
                    Env = new List<string>
                    {
                        "ACCEPT_EULA=Y",
                        $"SA_PASSWORD={SQLSERVER_SA_PASSWORD}"
                    },
                    HostConfig = new HostConfig
                    {
                        PortBindings = new Dictionary<string, IList<PortBinding>>
                        {
                            {
                                "1433/tcp",
                                new PortBinding[]
                                {
                                    new PortBinding
                                    {
                                        HostPort = freePort
                                    }
                                }
                            }
                        }
                    }
                });

            await dockerClient
                .Containers
                .StartContainerAsync(sqlContainer.ID, new ContainerStartParameters());

            var connection = await WaitUntilDatabaseAvailableAsync(freePort, timeout);

            var connectionString = GetSqlConnectionString(freePort, databaseName);

            return (connectionString, connection);
        }

        private static string GetSqlConnectionString(string port, string databaseName)
        {
            return $"Data Source=localhost,{port};" +
                   $"Initial Catalog={databaseName};" +
                   "Integrated Security=False;" +
                   "User ID=SA;" +
                   $"Password={SQLSERVER_SA_PASSWORD}";
        }

        public static async Task EnsureDockerStoppedAndRemovedAsync(string dockerContainerId)
        {
            var dockerClient = GetDockerClient();
            await dockerClient.Containers
                .StopContainerAsync(dockerContainerId, new ContainerStopParameters());
            await dockerClient.Containers
                .RemoveContainerAsync(dockerContainerId, new ContainerRemoveParameters());
        }

        private static DockerClient GetDockerClient()
        {
            var dockerUri = IsRunningOnWindows()
                ? "npipe://./pipe/docker_engine"
                : "unix:///var/run/docker.sock";
            return new DockerClientConfiguration(new Uri(dockerUri))
                .CreateClient();
        }

        private static async Task CleanupRunningContainers()
        {
            var dockerClient = GetDockerClient();

            var runningContainers = await dockerClient.Containers
                .ListContainersAsync(new ContainersListParameters());

            foreach (var runningContainer in runningContainers.Where(cont =>
                cont.Names.Any(n => n.Contains(SQLSERVER_CONTAINER_NAME_PREFIX))))
            {
                // Stopping all test containers that are older than one hour, they likely failed to cleanup
                if (runningContainer.Created < DateTime.UtcNow.AddHours(-1))
                {
                    try
                    {
                        await EnsureDockerStoppedAndRemovedAsync(runningContainer.ID);
                    }
                    catch
                    {
                        // Ignoring failures to stop running containers
                    }
                }
            }
        }

        private static async Task<SqlConnection> WaitUntilDatabaseAvailableAsync(string databasePort,
            int maxWaitTimeSeconds)
        {
            var start = DateTime.UtcNow;
            var connectionEstablised = false;
            while (!connectionEstablised && start.AddSeconds(maxWaitTimeSeconds) > DateTime.UtcNow)
            {
                try
                {
                    var sqlConnectionString =
                        $"Data Source=localhost,{databasePort};Integrated Security=False;User ID=SA;Password={SQLSERVER_SA_PASSWORD}";
                    using var sqlConnection = new SqlConnection(sqlConnectionString);
                    await sqlConnection.OpenAsync();
                    connectionEstablised = true;

                    return sqlConnection;
                }
                catch (Exception ex)
                {
                    // If opening the SQL connection fails, SQL Server is not ready yet
                    await Task.Delay(500);
                }
            }

            if (!connectionEstablised)
            {
                throw new Exception(
                    $"Connection to the SQL docker database could not be established within {maxWaitTimeSeconds} seconds.");
            }

            //TODO
            return null;
        }
        
        //TODO: use the method in SutController instead
        private static string GetFreePort()
        {
            // Taken from https://stackoverflow.com/a/150974/4190785
            var tcpListener = new TcpListener(IPAddress.Loopback, 0);
            tcpListener.Start();
            var port = ((IPEndPoint) tcpListener.LocalEndpoint).Port;
            tcpListener.Stop();
            return port.ToString();
        }

        private static bool IsRunningOnWindows()
        {
            return Environment.OSVersion.Platform == PlatformID.Win32NT;
        }
    }
}