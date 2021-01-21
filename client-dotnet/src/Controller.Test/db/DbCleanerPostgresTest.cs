using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Threading.Tasks;
using NUnit.Framework;

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
        

        [Test]
        public async Task testClean()
        {
            await using (postgres = postgresBuilder.Build())
            {
                await postgres.StartAsync();
                await using (connection = new NpgsqlConnection(postgres.ConnectionString))
                {
                    connection.Open();
                    var command = connection.CreateCommand();
                    command.CommandText = "CREATE TABLE Foo(x int, primary key (x));";
                    command.ExecuteNonQuery();

                    command.CommandText = "INSERT INTO Foo (x) VALUES (42)";
                    command.ExecuteNonQuery();

                    command.CommandText = "SELECT * FROM Foo;";
                    DbDataReader reader = command.ExecuteReader();
                    Assert.AreEqual(true, reader.HasRows);
                    reader.Close();

                    DbCleaner.clearDatabase_Postgres(connection);

                    command.CommandText = "SELECT * FROM Foo;";
                    reader = command.ExecuteReader();
                    Assert.AreEqual(false, reader.HasRows);
                    reader.Close();

                    await connection.CloseAsync();
                    await postgres.StopAsync();
                }
            }
        }
        
    }
}