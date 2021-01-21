using System;
using System.Data.Common;
using System.Threading.Tasks;
using Xunit;

// for testcontainer
using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Modules.Databases;
using Npgsql;
using Controller.Controllers.db;
using DotNet.Testcontainers.Containers.Configurations.Databases;

namespace Controller.Test
{
    public class DBCleanerPostgresTest
    {
        //for the moment, use this testcontainer for dotnet https://github.com/HofmeisterAn/dotnet-testcontainers
        private static ITestcontainersBuilder<PostgreSqlTestcontainer> postgresBuilder =
            new TestcontainersBuilder<PostgreSqlTestcontainer>()
                .WithDatabase(new PostgreSqlTestcontainerConfiguration
                {
                    Database = "db",
                    Username = "postgres",
                    Password = "postgres",
                })
                .WithExposedPort(5432);

        private static DbConnection connection;
        private static PostgreSqlTestcontainer postgres;
        

        [Fact]
        public async Task testClean()
        {
            await using (postgres = postgresBuilder.Build())
            {
                await postgres.StartAsync();
                await using (connection = new NpgsqlConnection(postgres.ConnectionString))
                {
                    connection.Open();
                    var command = connection.CreateCommand();
                    
                    SeededTestData.seedFKData(connection);

                    command.CommandText = "SELECT * FROM Foo;";
                    DbDataReader reader = command.ExecuteReader();
                    Assert.Equal(true, reader.HasRows);
                    reader.Close();

                    DbCleaner.clearDatabase_Postgres(connection);

                    command.CommandText = "SELECT * FROM Foo;";
                    reader = command.ExecuteReader();
                    Assert.Equal(false, reader.HasRows);
                    reader.Close();

                    await connection.CloseAsync();
                    await postgres.StopAsync();
                }
            }
        }
        
    }
}