using System;
using System.Data.Common;
using System.Threading.Tasks;
using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using DotNet.Testcontainers.Containers.Modules.Abstractions;
using DotNet.Testcontainers.Containers.Modules.Databases;
using EvoMaster.DatabaseController.Abstractions;
using Npgsql;

namespace EvoMaster.DatabaseController {
    public class PostgresDatabaseController : IDatabaseController {
        private static TestcontainerDatabase _database;
        private static NpgsqlConnection _connection;

        public PostgresDatabaseController(string databaseName, int port, string password, int timeout = 60) {
            DatabaseName = databaseName;
            Port = port;
            Timeout = timeout;
            Password = password;
        }

        public string DatabaseName { get; }
        public int Port { get; }
        public int Timeout { get; }
        public string Password { get; }

        public async Task<(string, DbConnection)> StartAsync() {
            var postgresBuilder = new TestcontainersBuilder<PostgreSqlTestcontainer>()
                .WithName($"EvoMaster-DB-Postgres-{Guid.NewGuid()}")
                .WithDatabase(new PostgreSqlTestcontainerConfiguration {
                    Database = DatabaseName,
                    Username = "user",
                    Password = Password
                })
                .WithExposedPort(Port).WithCleanUp(true);

            _database = postgresBuilder.Build();
            await _database.StartAsync();

            _connection = new NpgsqlConnection(_database.ConnectionString);
            await _connection.OpenAsync();

            //No idea why the password is missing in the connection string
            var connectionString = $"{_connection.ConnectionString};Password={_database.Password}";

            return (connectionString, _connection);
        }

        public async Task StopAsync() {
            await _connection.CloseAsync();
            await _database.StopAsync();
        }

        public void Stop() {
            _connection.Close();
            _database.StopAsync().GetAwaiter().GetResult();
        }
    }
}