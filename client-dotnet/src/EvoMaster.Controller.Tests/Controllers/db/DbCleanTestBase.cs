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
            seedFKData(GetConnection(), GetDbType());

            var reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+getBarTable(GetDbType())+";");
            Assert.True(reader.HasRows);
            reader.Close();

            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+getFooTable(GetDbType())+";");
            Assert.True(reader.HasRows);
            reader.Close();

            //clean all
            CleanDb(null);
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+getFooTable(GetDbType())+";");
            Assert.False(reader.HasRows);
            reader.Close();

            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+getBarTable(GetDbType())+";");
            Assert.False(reader.HasRows);
            reader.Close();
        }

        [Fact]
        public void TestCleanWithSkip() {
            seedFKData(GetConnection(), GetDbType());

            var reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+getBarTable(GetDbType())+";");
            Assert.True(reader.HasRows);
            reader.Close();

            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+getFooTable(GetDbType())+";");
            Assert.True(reader.HasRows);
            reader.Close();

            //clean all except Foo
            CleanDb(new List<string>() { "Foo" });
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+getFooTable(GetDbType())+";");
            Assert.True(reader.HasRows);
            reader.Close();

            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM "+getBarTable(GetDbType())+";");
            Assert.False(reader.HasRows);
            reader.Close();
        }

        [Fact]
        public void TestCleanException() {
            seedFKData(GetConnection(), GetDbType());

            // throws exception with incorrect skip table
            Assert.Throws<SystemException>(() => CleanDb(new List<string>() { "zoo" }));
        }


        public static void seedFKData(DbConnection connection, DatabaseType type = DatabaseType.H2) {
            if (type == DatabaseType.MS_SQL_SERVER){
                SqlScriptRunner.ExecCommand(connection, "CREATE SCHEMA Foo AUTHORIZATION dbo;");
            }
            SqlScriptRunner.ExecCommand(connection, "CREATE TABLE "+getFooTable(type)+"(x int, primary key (x));");
            SqlScriptRunner.ExecCommand(connection, "CREATE TABLE "+getBarTable(type)+"(y int, primary key (y));");

            switch (type) {
                case DatabaseType.MS_SQL_SERVER:
                case DatabaseType.POSTGRES:
                    SqlScriptRunner.ExecCommand(connection,
                        "alter table "+getBarTable(type)+" add constraint FK foreign key (y) references "+getFooTable(type)+";");
                    break;
                case DatabaseType.MYSQL:
                    SqlScriptRunner.ExecCommand(connection, "alter table "+getBarTable(type)+" add foreign key (y) references "+getFooTable(type)+"(x);");
                    break;
                default:
                    throw new InvalidOperationException("NOT SUPPORT");
            }

            SqlScriptRunner.ExecCommand(connection, "INSERT INTO "+getFooTable(type)+" (x) VALUES (42)");
            SqlScriptRunner.ExecCommand(connection, "INSERT INTO "+getBarTable(type)+" (y) VALUES (42)");
        }

        private static string getFooTable(DatabaseType type){
            switch (type){
                case  DatabaseType.MS_SQL_SERVER: return "Foo.Foo";
                default: return "Foo";
            }
        }
        
        private static string getBarTable(DatabaseType type){
            switch (type){
                case  DatabaseType.MS_SQL_SERVER: return "Foo.Bar";
                default: return "Bar";
            }
        }
    }
}