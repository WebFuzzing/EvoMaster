using System;
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

namespace Controller.Tests.Controllers.db
{
    public class PostgresFixture : IAsyncLifetime
    {

        private DbConnection _connection;
        private PostgreSqlTestcontainer _postgres;

        public async Task InitializeAsync()
        {
            //for the moment, use this testcontainer for dotnet https://github.com/HofmeisterAn/dotnet-testcontainers
            ITestcontainersBuilder<PostgreSqlTestcontainer> postgresBuilder =
                new TestcontainersBuilder<PostgreSqlTestcontainer>()
                    .WithDatabase(new PostgreSqlTestcontainerConfiguration
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
    
    public class DBCleanerPostgresTest : IClassFixture<PostgresFixture>, IDisposable
    {
        
        private readonly PostgresFixture _fixture;
        private readonly DbConnection _connection;
        
        public DBCleanerPostgresTest(PostgresFixture fixture)
        {
            _fixture = fixture;
            _connection = _fixture.GetConnection();
        }
        
        [Fact]
        public async Task TestAllClean()
        {
            SeededTestData.seedFKData(_connection);
            
            DbDataReader reader = SqlScriptRunner.ExecCommandWithDataReader(_connection, "SELECT * FROM Bar;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            reader = SqlScriptRunner.ExecCommandWithDataReader(_connection, "SELECT * FROM Foo;");
            Assert.True(reader.HasRows);
            reader.Close();
            
            //clean all
            DbCleaner.ClearDatabase_Postgres(_fixture.GetConnection());
            reader = SqlScriptRunner.ExecCommandWithDataReader(_connection, "SELECT * FROM Foo;");
            Assert.False(reader.HasRows);
            reader.Close();
            
            reader = SqlScriptRunner.ExecCommandWithDataReader(_connection, "SELECT * FROM Bar;");
            Assert.False(reader.HasRows);
            reader.Close();

        }

        [Fact]
        public async Task TestCleanWithSkip()
        {
            SeededTestData.seedFKData(_connection);
            
            DbDataReader reader = SqlScriptRunner.ExecCommandWithDataReader(_connection, "SELECT * FROM Bar;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            reader = SqlScriptRunner.ExecCommandWithDataReader(_connection, "SELECT * FROM Foo;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            //clean all except Foo
            DbCleaner.ClearDatabase_Postgres(_fixture.GetConnection(), new List<string>() { "Foo"});
            reader = SqlScriptRunner.ExecCommandWithDataReader(_connection, "SELECT * FROM Foo;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            reader = SqlScriptRunner.ExecCommandWithDataReader(_connection, "SELECT * FROM Bar;");
            Assert.False(reader.HasRows);
            reader.Close();
        }

        [Fact]
        public async Task TestCleanException()
        {
            SeededTestData.seedFKData(_fixture.GetConnection(), SupportedDatabaseType.POSTGRES);
            
            // throws exception with incorrect skip table
            Assert.Throws<SystemException>(()=>DbCleaner.ClearDatabase_Postgres(_connection,  new List<string>() { "zoo"}));
                    
            //clean all except Bar, and it should throw exception since Bar depends on other table, i.e., Foo
            Assert.Throws<SystemException>(()=>DbCleaner.ClearDatabase_Postgres(_connection, new List<string>() { "Bar"}));
        }

        public void Dispose()
        {
            SqlScriptRunner.ExecCommand(_connection, "DROP SCHEMA public CASCADE;");
            SqlScriptRunner.ExecCommand(_connection, "CREATE SCHEMA public;");
            SqlScriptRunner.ExecCommand(_connection, "GRANT ALL ON SCHEMA public TO postgres;");
            SqlScriptRunner.ExecCommand(_connection, "GRANT ALL ON SCHEMA public TO public;");
        }
    }
}