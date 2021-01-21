// This is created on 01-21-2021 by Man Zhang

using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Threading.Tasks;
using NUnit.Framework;

using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Modules.Databases;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using MySql.Data.MySqlClient;
using Controller.Controllers.db;

namespace Controller.Test
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
        
        [Test]
        public async Task testClean()
        {
            await using (mySql = mySqlBuilder.Build())
            {
                await mySql.StartAsync();
                await using (connection = new MySqlConnection(mySql.ConnectionString))
                {
                    connection.Open();
                    var command = connection.CreateCommand();
                    command.CommandText = "CREATE TABLE Foo(x int, primary key (x));";
                    command.ExecuteNonQuery();

                    command.CommandText = "INSERT INTO Foo (x) VALUES (42)";
                    command.ExecuteNonQuery();

                    command.CommandText = "SELECT * FROM Foo;";
                    DbDataReader reader = command.ExecuteReader();
                    Assert.AreEqual(reader.HasRows, true);
                    reader.Close();
                    
                    DbCleaner.clearDatabase_H2(connection, "db", null, DatabaseType.MySQL);

                    command.CommandText = "SELECT * FROM Foo;";
                    reader = command.ExecuteReader();
                    Assert.AreEqual( false, reader.HasRows);
                    reader.Close();

                    await connection.CloseAsync();
                    await mySql.StopAsync();
                }
            }
        }
    }
}