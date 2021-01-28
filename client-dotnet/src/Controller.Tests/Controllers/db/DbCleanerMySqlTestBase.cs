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
    public class DbCleanerMySqlTestBase : DbCleanTestBase, IAsyncLifetime
    {
        
        private static ITestcontainersBuilder<MySqlTestcontainer> mySqlBuilder =
            new TestcontainersBuilder<MySqlTestcontainer>()
                .WithDatabase(new MySqlTestcontainerConfiguration
                {
                    Database = "db",
                    Username = "mysql",
                    Password = "mysql"
                });
        
        private static DbConnection _connection;
        private static MySqlTestcontainer mySql;
        
        protected override DbConnection GetConnection()
        {
            return _connection;
        }
        
        protected override SupportedDatabaseType GetDbType()
        {
            return SupportedDatabaseType.MySQL;
        }
        
        

        public async Task InitializeAsync()
        {
            mySql = mySqlBuilder.Build();
            await mySql.StartAsync();
            _connection = new MySqlConnection(mySql.ConnectionString);
            await _connection.OpenAsync();
        }

        public async Task DisposeAsync()
        {
            DbCleaner.ClearDatabase(_connection, null, SupportedDatabaseType.MySQL);
            
            SqlScriptRunner.ExecCommand(_connection, "DROP DATABASE db;");
            SqlScriptRunner.ExecCommand(_connection, "CREATE DATABASE db;");
            
            await _connection.CloseAsync();
            await mySql.StopAsync();
        }
    }
}