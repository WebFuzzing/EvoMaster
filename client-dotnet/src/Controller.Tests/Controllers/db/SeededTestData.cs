// This is created on 01-21-2021 by Man Zhang

using System.Data.Common;
using Controller.Controllers.db;

namespace Controller.Test
{
    public static class SeededTestData
    {
        public static void seedFKData(DbConnection connection, DatabaseType type = DatabaseType.NOT_SPECIFIED, bool doCreate=true)
        {

            if (doCreate)
            {
                SqlScriptRunner.execCommand(connection, "CREATE TABLE Foo(x int, primary key (x));");
                SqlScriptRunner.execCommand(connection, "CREATE TABLE Bar(y int, primary key (y));");
                
                switch (type)
                {
                    case DatabaseType.NOT_SPECIFIED:
                        SqlScriptRunner.execCommand(connection,  "alter table Bar add constraint FK foreign key (y) references Foo;");
                        break;
                    case DatabaseType.MySQL:
                        SqlScriptRunner.execCommand(connection,  "alter table Bar add foreign key (y) references Foo(x);");
                        break;
                }
            }

            SqlScriptRunner.execCommand(connection,  "INSERT INTO Foo (x) VALUES (42)");
            SqlScriptRunner.execCommand(connection,  "INSERT INTO Bar (y) VALUES (42)");

        }
    }
}