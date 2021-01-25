// This is created on 01-21-2021 by Man Zhang

using System;
using System.Data.Common;
using Controller.Controllers.db;

namespace Controller.Tests.Controllers.db
{
    public static class SeededTestData
    {
        public static void seedFKData(DbConnection connection, SupportedDatabaseType type = SupportedDatabaseType.H2)
        {

            SqlScriptRunner.ExecCommand(connection, "CREATE TABLE Foo(x int, primary key (x));");
            SqlScriptRunner.ExecCommand(connection, "CREATE TABLE Bar(y int, primary key (y));");
                
            switch (type)
            {
                case SupportedDatabaseType.H2:
                case SupportedDatabaseType.POSTGRES:
                    SqlScriptRunner.ExecCommand(connection,  "alter table Bar add constraint FK foreign key (y) references Foo;");
                    break;
                case SupportedDatabaseType.MySQL:
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