using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Threading.Tasks;
using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using DotNet.Testcontainers.Containers.Modules.Databases;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Controllers.db;
using Npgsql;
using Xunit;

namespace EvoMaster.Controller.Tests.Controllers.db{
    public class DbCleanerPostgresTest : DbCleanTestBase, IClassFixture<PostgresFixture>, IDisposable{
        private readonly PostgresFixture _fixture;
        private readonly DbConnection _connection;

        public DbCleanerPostgresTest(PostgresFixture fixture){
            _fixture = fixture;
            _connection = _fixture.GetConnection();
        }

        public void Dispose(){
            SqlScriptRunner.ExecCommand(_connection, "DROP SCHEMA public CASCADE;");
            SqlScriptRunner.ExecCommand(_connection, "CREATE SCHEMA public;");
            SqlScriptRunner.ExecCommand(_connection, "GRANT ALL ON SCHEMA public TO postgres;");
            SqlScriptRunner.ExecCommand(_connection, "GRANT ALL ON SCHEMA public TO public;");
        }

        protected override DbConnection GetConnection(){
            return _connection;
        }

        protected override DatabaseType GetDbType(){
            return DatabaseType.POSTGRES;
        }

        protected override void CleanDb(List<string> tablesToSkip){
            DbCleaner.ClearDatabase(_connection, tablesToSkip, GetDbType());
        }
    }

    public class PostgresFixture : IAsyncLifetime{
        private DbConnection _connection;
        private PostgreSqlTestcontainer _postgres;

        public async Task InitializeAsync(){
            //for the moment, use this testcontainer for dotnet https://github.com/HofmeisterAn/dotnet-testcontainers
            ITestcontainersBuilder<PostgreSqlTestcontainer> postgresBuilder =
                new TestcontainersBuilder<PostgreSqlTestcontainer>()
                    .WithDatabase(new PostgreSqlTestcontainerConfiguration("postgres:11.5"){
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

        public async Task DisposeAsync(){
            await _connection.CloseAsync();
            await _postgres.StopAsync();
        }

        public DbConnection GetConnection(){
            return _connection;
        }
    }
}