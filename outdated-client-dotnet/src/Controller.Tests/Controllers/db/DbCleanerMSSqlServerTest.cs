
using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Data.SqlClient;
using System.Threading.Tasks;
using Controller.Api;
using Xunit;

using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Modules.Databases;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using Controller.Controllers.db;

namespace Controller.Tests.Controllers.db
{
    public class DbCleanerMSSqlServerTest : DbCleanTestBase, IAsyncLifetime
    {
        
        private static ITestcontainersBuilder<MsSqlTestcontainer> msSQLBuilder =
            new TestcontainersBuilder<MsSqlTestcontainer>()
                .WithDatabase(new MsSqlTestcontainerConfiguration("mcr.microsoft.com/mssql/server:2017-CU14-ubuntu")
                {
                    Password = "A_Str0ng_Required_Password"
                });
        
        private DbConnection _connection;
        private MsSqlTestcontainer _msSql;
        
        protected override DbConnection GetConnection()
        {
            return _connection;
        }
        
        protected override DatabaseType GetDbType()
        {
            return DatabaseType.MS_SQL_SERVER;
        }
        
        public async Task InitializeAsync()
        {
            _msSql = msSQLBuilder.Build();
            await _msSql.StartAsync();
            _connection = new SqlConnection(_msSql.ConnectionString);
            await _connection.OpenAsync();
        }

        public async Task DisposeAsync()
        {
            DbCleaner.ClearDatabase(_connection, null, GetDbType(), "");
            
            await _connection.CloseAsync();
            await _msSql.StopAsync();
        }
        
        
        protected override void CleanDb(List<string> tablesToSkip)
        {
            DbCleaner.ClearDatabase(_connection, tablesToSkip, GetDbType(), "");
        }
    }
}