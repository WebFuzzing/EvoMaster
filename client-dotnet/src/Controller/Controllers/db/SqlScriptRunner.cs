// This is created on 01-21-2021 by Man Zhang

using System;
using System.Data.Common;

namespace Controller.Controllers.db
{
    public class SqlScriptRunner
    {

        public static void execCommand(DbConnection connection,string command)
        {
            DbCommand cmd = connection.CreateCommand();
            cmd.CommandText = command;
            cmd.ExecuteNonQuery();
        }
        
        
        public static DbDataReader execQueryCommand(DbConnection connection,string command)
        {
            //Man: need to check.
            if (!command.StartsWith("SELECT"))
            {
                throw new InvalidOperationException("it is not query command.");
            }
            DbCommand cmd = connection.CreateCommand();
            cmd.CommandText = command;
            return cmd.ExecuteReader();
        }
    }
}