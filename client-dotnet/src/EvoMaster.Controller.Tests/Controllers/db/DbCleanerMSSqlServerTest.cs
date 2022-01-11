using System.Collections.Generic;
using System.Data.Common;
using System.Data.SqlClient;
using System.Threading.Tasks;
using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using DotNet.Testcontainers.Containers.Modules.Databases;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Controllers.db;
using Npgsql;
using Xunit;

namespace EvoMaster.Controller.Tests.Controllers.db {
    public class DbCleanerMsSqlServerTest : DbCleanTestBase, IAsyncLifetime {
        private static ITestcontainersBuilder<MsSqlTestcontainer> msSQLBuilder =
            new TestcontainersBuilder<MsSqlTestcontainer>()
                .WithDatabase(new MsSqlTestcontainerConfiguration("mcr.microsoft.com/mssql/server:2017-CU14-ubuntu") {
                    Password = "A_Str0ng_Required_Password"
                });

        private DbConnection _connection;
        private MsSqlTestcontainer _msSql;

        protected override DbConnection GetConnection() {
            return _connection;
        }

        protected override DatabaseType GetDbType() {
            return DatabaseType.MS_SQL_SERVER;
        }

        public async Task InitializeAsync() {
            _msSql = msSQLBuilder.Build();
            await _msSql.StartAsync();
            _connection = new SqlConnection(_msSql.ConnectionString);
            await _connection.OpenAsync();
        }

        public async Task DisposeAsync() {
            DbCleaner.ClearDatabase(_connection, null, GetDbType(), "");

            await _connection.CloseAsync();
            await _msSql.StopAsync();
        }


        protected override void CleanDb(List<string> tablesToSkip) {
            DbCleaner.ClearDatabase(_connection, tablesToSkip, GetDbType(), "");
        }
    }
    public class PostgresFixture : IAsyncLifetime
    {

        private DbConnection _connection;
        private PostgreSqlTestcontainer _postgres;

        public async Task InitializeAsync()
        {
            //for the moment, use this testcontainer for dotnet https://github.com/HofmeisterAn/dotnet-testcontainers
            ITestcontainersBuilder<PostgreSqlTestcontainer> postgresBuilder =
                new TestcontainersBuilder<PostgreSqlTestcontainer>()
                    .WithDatabase(new PostgreSqlTestcontainerConfiguration("postgres:11.5")
                    {
                        Database = "db",
                        Username = "postgres",
                        Password = "postgres",
                    })
                    .WithExposedPort(5432);
            
            _postgres = postgresBuilder.Build();
            await _postgres.StartAsync();
            _connection = new NpgsqlConnection(_postgres.ConnectionString);
            await _connection.OpenAsync();
        }

        public async Task DisposeAsync()
        {
            await _connection.CloseAsync();
            await _postgres.StopAsync();
        }

        public DbConnection GetConnection()
        {
            return _connection;
        }
    }
}