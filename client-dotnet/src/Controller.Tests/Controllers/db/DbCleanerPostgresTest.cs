using System;
using System.Collections;
using System.Collections.Generic;
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

                    DbDataReader reader = SqlScriptRunner.execQueryCommand(connection, "SELECT * FROM Bar;");
                    Assert.Equal(true, reader.HasRows);
                    reader.Close();
                    
                    reader = SqlScriptRunner.execQueryCommand(connection, "SELECT * FROM Foo;");
                    Assert.Equal(true, reader.HasRows);
                    reader.Close();
                    
                    //clean all except Foo
                    DbCleaner.clearDatabase_Postgres(connection, "public", new List<string>() { "Foo"});
                    reader = SqlScriptRunner.execQueryCommand(connection, "SELECT * FROM Foo;");
                    Assert.Equal(true, reader.HasRows);
                    reader.Close();
                    
                    reader = SqlScriptRunner.execQueryCommand(connection, "SELECT * FROM Bar;");
                    Assert.Equal(false, reader.HasRows);
                    reader.Close();
                    
                    //clean all
                    DbCleaner.clearDatabase_Postgres(connection);
                    reader = SqlScriptRunner.execQueryCommand(connection, "SELECT * FROM Foo;");
                    Assert.Equal(false, reader.HasRows);
                    reader.Close();
                    
                    SeededTestData.seedFKData(connection, DatabaseType.NOT_SPECIFIED, false);
                    // throws exception with incorrect skip table
                    Assert.Throws<SystemException>(()=>DbCleaner.clearDatabase_Postgres(connection, "public", new List<string>() { "zoo"}));
                    
                    //clean all except Bar, but it should throw exception 
                    Assert.Throws<SystemException>(()=>DbCleaner.clearDatabase_Postgres(connection, "public", new List<string>() { "Bar"}));

                    await connection.CloseAsync();
                    await postgres.StopAsync();
                }
            }
        }
        
    }
}