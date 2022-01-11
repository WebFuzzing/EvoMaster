using System;
using System.Collections.Generic;
using System.Data.Common;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Controllers.db;
using Xunit;

namespace EvoMaster.Controller.Tests.Controllers.db {
    public class DbCleanerPostgresTest : DbCleanTestBase, IClassFixture<PostgresFixture>, IDisposable
    {
        
        private readonly PostgresFixture _fixture;
        private readonly DbConnection _connection;
        
        public DbCleanerPostgresTest(PostgresFixture fixture)
        {
            _fixture = fixture;
            _connection = _fixture.GetConnection();
        }

        public void Dispose()
        {
            SqlScriptRunner.ExecCommand(_connection, "DROP SCHEMA public CASCADE;");
            SqlScriptRunner.ExecCommand(_connection, "CREATE SCHEMA public;");
            SqlScriptRunner.ExecCommand(_connection, "GRANT ALL ON SCHEMA public TO postgres;");
            SqlScriptRunner.ExecCommand(_connection, "GRANT ALL ON SCHEMA public TO public;");
        }

        protected override DbConnection GetConnection()
        {
            return _connection;
        }

        protected override DatabaseType GetDbType()
        {
            return DatabaseType.POSTGRES;
        }

        protected override void CleanDb(List<string> tablesToSkip)
        {
            DbCleaner.ClearDatabase(_connection, tablesToSkip, GetDbType());
        }
    }
}