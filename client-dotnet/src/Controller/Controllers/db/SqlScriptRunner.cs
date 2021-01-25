using System;
using System.Data.Common;

namespace Controller.Controllers.db
{
    public static class SqlScriptRunner
    {

        public static void ExecCommand(DbConnection connection,string command)
        {
            DbCommand cmd = connection.CreateCommand();
            ExecCommand(cmd, command);
        }

        //https://docs.microsoft.com/en-us/dotnet/framework/data/adonet/executing-a-command
        //https://docs.microsoft.com/en-us/dotnet/api/system.data.common.dbcommand?view=net-5.0
        public static void ExecCommand(DbCommand command, string commandText)
        {
            command.CommandText = commandText;
            command.ExecuteNonQuery();
        }

        public static DbDataReader ExecCommandWithDataReader(DbCommand command, string commandText)
        {
            command.CommandText = commandText;
            return command.ExecuteReader();
        }


        public static DbDataReader ExecCommandWithDataReader(DbConnection connection,string command)
        {
            DbCommand cmd = connection.CreateCommand();
            return ExecCommandWithDataReader(cmd, command);
        }
    }
}