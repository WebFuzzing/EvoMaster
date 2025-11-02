using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Threading.Tasks;
using EvoMaster.Controller.Api;
using Xunit;
using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Modules.Databases;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using MySql.Data.MySqlClient;
using EvoMaster.Controller.Controllers.db;

namespace EvoMaster.Controller.Tests.Controllers.db{
    public class DbCleanerMySqlTest : DbCleanTestBase, IAsyncLifetime{
        private static ITestcontainersBuilder<MySqlTestcontainer> mySqlBuilder =
            new TestcontainersBuilder<MySqlTestcontainer>()
                .WithDatabase(new MySqlTestcontainerConfiguration("mysql:8.0.18"){
                    Database = "db",
                    Username = "mysql",
                    Password = "mysql"
                });

        private static DbConnection _connection;
        private static MySqlTestcontainer mySql;

        protected override DbConnection GetConnection(){
            return _connection;
        }

        protected override DatabaseType GetDbType(){
            return DatabaseType.MYSQL;
        }

        public async Task InitializeAsync(){
            mySql = mySqlBuilder.Build();
            await mySql.StartAsync();
            _connection = new MySqlConnection(mySql.ConnectionString);
            await _connection.OpenAsync();
        }

        public async Task DisposeAsync(){
            DbCleaner.ClearDatabase(_connection, null, DatabaseType.MYSQL, "db");

            await _connection.CloseAsync();
            await mySql.StopAsync();
        }


        protected override void CleanDb(List<string> tablesToSkip){
            DbCleaner.ClearDatabase(_connection, tablesToSkip, GetDbType(), "db");
        }
    }
}