using System;
using System.Data.Common;
using System.Threading.Tasks;
using Docker.DotNet;
using EvoMaster.Client.Util;
using EvoMaster.DatabaseController.Abstractions;
using EvoMaster.DatabaseController.Containers;
using Microsoft.Data.SqlClient;

namespace EvoMaster.DatabaseController {
    public class SqlServerDatabaseController : IDatabaseController {
        public string DatabaseName { get; }
        public int Port { get; }
        public int Timeout { get; }
        public string Password { get; }

        public string ImageName{ get; }

        private static SqlServerContainer _sqlServerContainer;
        private static DockerClient _dockerClient;

        public SqlServerDatabaseController(string databaseName, int port, string password, int timeout = 60, string imageName = "mcr.microsoft.com/mssql/server:2017-latest") {
            DatabaseName = databaseName;
            Port = port;
            Timeout = timeout;
            Password = password;
            ImageName = imageName;
        }

        public async Task<(string, DbConnection)> StartAsync() {
            var dockerUri = IDatabaseController.GetDockerUri();

            _dockerClient = new DockerClientConfiguration(
                    new Uri(dockerUri))
                .CreateClient();

            //DockerContainerBase.CleanupOrphanedContainersAsync(_dockerClient).Wait(Timeout * 500);

            _sqlServerContainer = new SqlServerContainer(Port, Password, ImageName);

            await _sqlServerContainer.StartAsync(_dockerClient, Timeout);

            var connectionString = _sqlServerContainer.GetConnectionString(DatabaseName);

            //TODO: use logger
            SimpleLogger.Info($"*** SQL Server database started and the connection string is: \"{connectionString}\"");

            var connection = new SqlConnection(connectionString);

            return (connectionString, connection);
        }

        public Task StopAsync() {
            return _sqlServerContainer.RemoveAsync(_dockerClient);
        }

        public void Stop() {
            _sqlServerContainer.RemoveAsync(_dockerClient).GetAwaiter().GetResult();
        }
    }
}