
using System;
using System.Collections.Generic;
using System.Data.Common;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Controllers.db;
using Xunit;

namespace EvoMaster.Controller.Tests.Controllers.db
{
    public abstract class DbCleanTestBase
    {

        protected abstract DbConnection GetConnection();
        protected abstract DatabaseType GetDbType();

        protected abstract void CleanDb(List<string> tablesToSkip);
        
        [Fact]
        public void TestAllClean()
        {
            seedFKData(GetConnection(), GetDbType());
            
            DbDataReader reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Bar;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Foo;");
            Assert.True(reader.HasRows);
            reader.Close();
            
            //clean all
            CleanDb(null);
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Foo;");
            Assert.False(reader.HasRows);
            reader.Close();
            
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Bar;");
            Assert.False(reader.HasRows);
            reader.Close();

        }

        [Fact]
        public void TestCleanWithSkip()
        {
            seedFKData(GetConnection(), GetDbType());
            
            DbDataReader reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Bar;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Foo;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            //clean all except Foo
            CleanDb(new List<string>() { "Foo"});
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Foo;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Bar;");
            Assert.False(reader.HasRows);
            reader.Close();
        }

        [Fact]
        public void TestCleanException()
        {
            seedFKData(GetConnection(), GetDbType());
            
            // throws exception with incorrect skip table
            Assert.Throws<SystemException>(()=> CleanDb(new List<string>() { "zoo"}));
        }
        
        
        public static void seedFKData(DbConnection connection, DatabaseType type = DatabaseType.H2)
        {

            SqlScriptRunner.ExecCommand(connection, "CREATE TABLE Foo(x int, primary key (x));");
            SqlScriptRunner.ExecCommand(connection, "CREATE TABLE Bar(y int, primary key (y));");
                
            switch (type)
            {
                case DatabaseType.MS_SQL_SERVER:
                case DatabaseType.POSTGRES:
                    SqlScriptRunner.ExecCommand(connection,  "alter table Bar add constraint FK foreign key (y) references Foo;");
                    break;
                case DatabaseType.MYSQL:
                    SqlScriptRunner.ExecCommand(connection,  "alter table Bar add foreign key (y) references Foo(x);");
                    break;
                default:
                    throw new InvalidOperationException("NOT SUPPORT");
            }

            SqlScriptRunner.ExecCommand(connection,  "INSERT INTO Foo (x) VALUES (42)");
            SqlScriptRunner.ExecCommand(connection,  "INSERT INTO Bar (y) VALUES (42)");

        }
    }
}