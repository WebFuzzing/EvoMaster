// This is created on 01-27-2021 by Man Zhang

using System;
using System.Collections.Generic;
using System.Data.Common;
using System.Threading.Tasks;
using Controller.Controllers.db;
using Xunit;

namespace Controller.Tests.Controllers.db
{
    public abstract class DbCleanTestBase
    {

        protected abstract DbConnection GetConnection();
        protected abstract SupportedDatabaseType GetDbType();
        
        [Fact]
        public void TestAllClean()
        {
            SeededTestData.seedFKData(GetConnection(), GetDbType());
            
            DbDataReader reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Bar;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Foo;");
            Assert.True(reader.HasRows);
            reader.Close();
            
            //clean all
            DbCleaner.ClearDatabase(GetConnection(), null, GetDbType());
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
            SeededTestData.seedFKData(GetConnection(), GetDbType());
            
            DbDataReader reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Bar;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            reader = SqlScriptRunner.ExecCommandWithDataReader(GetConnection(), "SELECT * FROM Foo;");
            Assert.True(reader.HasRows);
            reader.Close();
                    
            //clean all except Foo
            DbCleaner.ClearDatabase(GetConnection(), new List<string>() { "Foo"}, GetDbType());
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
            SeededTestData.seedFKData(GetConnection(), GetDbType());
            
            // throws exception with incorrect skip table
            Assert.Throws<SystemException>(()=>DbCleaner.ClearDatabase(GetConnection(),  new List<string>() { "zoo"}, GetDbType()));
        }
    }
}