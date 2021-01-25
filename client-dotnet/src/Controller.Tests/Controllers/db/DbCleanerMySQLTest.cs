// This is created on 01-21-2021 by Man Zhang

using System;
using System.Data.Common;
using System.Threading.Tasks;
using Xunit;

using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Modules.Databases;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using MySql.Data.MySqlClient;
using Controller.Controllers.db;

namespace Controller.Tests.Controllers.db
{
   

    public class DbCleanerMySQLTest 
    {
        //for the moment, use this testcontainer for dotnet https://github.com/HofmeisterAn/dotnet-testcontainers
        private static ITestcontainersBuilder<MySqlTestcontainer> mySqlBuilder =
            new TestcontainersBuilder<MySqlTestcontainer>()
                .WithDatabase(new MySqlTestcontainerConfiguration
                {
                    Database = "db",
                    Username = "mysql",
                    Password = "mysql"
                });
        private static DbConnection connection;
        private static MySqlTestcontainer mySql;
        
        [Fact]
        public async Task testClean()
        {
            await using (mySql = mySqlBuilder.Build())
            {
                await mySql.StartAsync();
                await using (connection = new MySqlConnection(mySql.ConnectionString))
                {
                    connection.Open();
                    var command = connection.CreateCommand();
                    
                    SeededTestData.seedFKData(connection, SupportedDatabaseType.MySQL);

                    command.CommandText = "SELECT * FROM Foo;";
                    DbDataReader reader = command.ExecuteReader();
                    Assert.Equal(reader.HasRows, true);
                    reader.Close();
                    
                    DbCleaner.ClearDatabase(connection, null, SupportedDatabaseType.MySQL);

                    command.CommandText = "SELECT * FROM Foo;";
                    reader = command.ExecuteReader();
                    Assert.Equal( false, reader.HasRows);
                    reader.Close();

                    await connection.CloseAsync();
                    await mySql.StopAsync();
                }
            }
        }
    }
}