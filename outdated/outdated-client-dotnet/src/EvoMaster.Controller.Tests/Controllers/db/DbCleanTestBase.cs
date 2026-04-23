using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Diagnostics;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Controllers.db;
using Xunit;

namespace EvoMaster.Controller.Tests.Controllers.db {
    public abstract class DbCleanTestBase {
        protected abstract DbConnection GetConnection();
        protected abstract DatabaseType GetDbType();

        protected abstract void CleanDb(List<string> tablesToSkip);

        [Fact]
        public void TestAllClean() {
            SeedFKData(GetConnection());

            var reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+GetBarTable()+";");
            Assert.True(reader.HasRows);
            reader.Close();

            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+GetFooTable()+";");
            Assert.True(reader.HasRows);
            reader.Close();

            //clean all
            CleanDb(null);
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+GetFooTable()+";");
            Assert.False(reader.HasRows);
            reader.Close();

            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+GetBarTable()+";");
            Assert.False(reader.HasRows);
            reader.Close();
        }

        [Fact]
        public void TestCleanWithSkip() {
            SeedFKData(GetConnection());

            var reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+GetBarTable()+";");
            Assert.True(reader.HasRows);
            reader.Close();

            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+GetFooTable()+";");
            Assert.True(reader.HasRows);
            reader.Close();

            //clean all except Foo
            CleanDb(new List<string>() { "Foo" });
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+GetFooTable()+";");
            Assert.True(reader.HasRows);
            reader.Close();

            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+GetBarTable()+";");
            Assert.False(reader.HasRows);
            reader.Close();
        }

        [Fact]
        public void TestCleanException() {
            SeedFKData(GetConnection());

            // throws exception with incorrect skip table
            Assert.Throws<SystemException>(() => CleanDb(new List<string>() { "zoo" }));
        }


        public virtual void SeedFKData(DbConnection connection) {
            
            SqlScriptRunner.ExecCommand(connection, "CREATE TABLE "+GetFooTable()+"(x int, primary key (x));");
            SqlScriptRunner.ExecCommand(connection, "CREATE TABLE "+GetBarTable()+"(y int, primary key (y));");

            switch (GetDbType()) {
                case DatabaseType.MS_SQL_SERVER:
                case DatabaseType.POSTGRES:
                    SqlScriptRunner.ExecCommand(connection,
                        "alter table "+GetBarTable()+" add constraint FK foreign key (y) references "+GetFooTable()+";");
                    break;
                case DatabaseType.MYSQL:
                    SqlScriptRunner.ExecCommand(connection, "alter table "+GetBarTable()+" add foreign key (y) references "+GetFooTable()+"(x);");
                    break;
                default:
                    throw new InvalidOperationException("NOT SUPPORT");
            }

            SqlScriptRunner.ExecCommand(connection, "INSERT INTO "+GetFooTable()+" (x) VALUES (42)");
            SqlScriptRunner.ExecCommand(connection, "INSERT INTO "+GetBarTable()+" (y) VALUES (42)");
        }

        /**
         * Foo table is used for seeding test data for db cleaner
         */
        public virtual string GetFooTable() => "Foo";
        
        
        /**
         * Bar table is used for seeding test data for db cleaner
         */
        public virtual string GetBarTable() => "Bar";
    }
}