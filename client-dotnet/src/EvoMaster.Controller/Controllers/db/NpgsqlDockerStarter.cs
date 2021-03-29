using System.Data.Common;
using System.Threading.Tasks;
using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using DotNet.Testcontainers.Containers.Modules.Abstractions;
using DotNet.Testcontainers.Containers.Modules.Databases;
using Microsoft.Data.SqlClient;
using Npgsql;

namespace EvoMaster.Controller.Controllers.db
{
    internal static class NpgsqlDockerStarter
    {
        private static TestcontainerDatabase _database;
        private static NpgsqlConnection _connection;

        public static async Task<(string, DbConnection)> StartDatabaseAsync(string databaseName)
        {
            var postgresBuilder = new TestcontainersBuilder<PostgreSqlTestcontainer>()
                .WithDatabase(new PostgreSqlTestcontainerConfiguration
                {
                    Database = databaseName,
                    Username = "user",
                    Password = "password123"
                })
                .WithExposedPort(5432);

            _database = postgresBuilder.Build();
            await _database.StartAsync();

            _connection = new NpgsqlConnection(_database.ConnectionString);
            await _connection.OpenAsync();

            //No idea why the password is missing in the connection string
            var connectionString = $"{_connection.ConnectionString};Password={_database.Password}";

            return (connectionString, _connection);
        }
    }
}