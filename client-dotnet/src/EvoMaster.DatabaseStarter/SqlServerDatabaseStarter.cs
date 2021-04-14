using System;
using System.Data.Common;
using System.Threading.Tasks;
using Docker.DotNet;
using EvoMaster.DatabaseStarter.Abstractions;
using EvoMaster.DatabaseStarter.Containers;
using Microsoft.Data.SqlClient;

namespace EvoMaster.DatabaseStarter
{
    public class SqlServerDatabaseStarter : IDatabaseStarter
    {
        private const string SaPassword = "password123";
        private static SqlServerContainer _sqlServerContainer;
        private static DockerClient _dockerClient;

        public async Task<(string, DbConnection)> StartAsync(string databaseName, int port, int timeout)
        {
            var dockerUri = IDatabaseStarter.GetDockerUri();

            _dockerClient = new DockerClientConfiguration(
                    new Uri(dockerUri))
                .CreateClient();

            DockerContainerBase.CleanupOrphanedContainersAsync(_dockerClient).Wait(timeout * 500);

            _sqlServerContainer = new SqlServerContainer(port, SaPassword);

            await _sqlServerContainer.StartAsync(_dockerClient, timeout);

            var connectionString = _sqlServerContainer.GetConnectionString(databaseName);

            //TODO: use logger
            Console.WriteLine($"*** SQL Server database started and the connection string is: \"{connectionString}\"");

            var connection = new SqlConnection(connectionString);

            return (connectionString, connection);
        }
    }
}