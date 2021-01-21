using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Linq;

namespace Controller.Controllers.db
{
    
    public class DbCleaner
    {

        public static void clearDatabase_H2(int retries, DbConnection connection, string schemaName, List<string> tablesToSkip)
        {
            
        }

        public static void clearDatabase_Postgres(DbConnection connection)
        {
            clearDatabase_Postgres(connection, "public", null);
        }

        //https://docs.microsoft.com/en-us/dotnet/framework/data/adonet/dbconnection-dbcommand-and-dbexception
        public static void clearDatabase_Postgres(DbConnection connection,  string schemaName, List<string> tablesToSkip)
        {
            // Check for valid DbConnection.
            if (connection != null)
            {
                try
                {
                    // Man: need to check whether to throw an exception here, the connection should open in startSut.
                    if (connection.State == ConnectionState.Closed)
                    {
                        connection.Open();
                    }
                        
                    // Create the command.
                    DbCommand command = connection.CreateCommand();
                        
                    truncateTables(tablesToSkip, command, schemaName,true);
                    
                    resetSequences(command, schemaName);
                }
                catch (Exception ex) //catch DBException, InvalidOperationException or others
                {
                    Console.WriteLine("Exception.Message: {0}", ex.Message);
                    // with java: throw new RuntimeException(e);
                    throw new SystemException();
                }
            }
        }

        private static void truncateTables(List<string> tablesToSkip, DbCommand command, String schema, bool singleCommand) 
        {
            command.CommandText = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='" + schema + "' AND (TABLE_TYPE='TABLE' OR TABLE_TYPE='BASE TABLE')";
            command.CommandType = CommandType.Text;
            
            // Retrieve all tables
            DbDataReader reader = command.ExecuteReader();

            ISet<string> tables = new HashSet<string>();
            while (reader.Read())
            {
                tables.Add(reader.GetString(0));
            }
            reader.Close();

            if (tables.Count == 0)
            {
                throw new InvalidOperationException("Could not find any table");
            }
            
            //check tablesToSkip
            if (tablesToSkip != null)
            {
                foreach (var s in tablesToSkip)
                {
                    var notcontain = tables.ToList().FindAll(t => s.Equals(t, StringComparison.InvariantCultureIgnoreCase));
                    if (notcontain.Count > 0)
                    {
                        string msg = "Asked to skip tables '" + string.Join(",", notcontain)+ "', but it does not exist.";
                        msg += " Existing tables in schema '"+schema+"': [" +
                               string.Join(",", tables)+ "]";
                        throw new InvalidOperationException(msg);
                    }
                }
            }

            List<string> tablesToClear = tables.ToList().FindAll(t =>
                tablesToSkip == null || tablesToSkip.Count == 0 ||
                !tablesToSkip.Any(s => t.Equals(s, StringComparison.InvariantCultureIgnoreCase)));

            if (singleCommand)
            {
                string all = string.Join(",", tablesToClear);
                command.CommandText = "TRUNCATE TABLE " + all;
                command.ExecuteNonQuery();
            }else
            {
                //note from DbCleaner.java: if one at a time, need to make sure to first disable FK checks
                foreach (var t in tablesToClear)
                {
                    command.CommandText = "TRUNCATE TABLE " + t;
                    command.ExecuteNonQuery();
                }
            }
        }

        private static void resetSequences(DbCommand command, String schema)
        {
            ISet<string> sequences = new HashSet<string>();
            command.CommandText="SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA='" + schema + "'";
            command.CommandType = CommandType.Text;
            DbDataReader reader = command.ExecuteReader();
            while (reader.Read())
            {
                sequences.Add(reader.GetString(0));
            }
            reader.Close();
            foreach (var sequence in sequences)
            {
                command.CommandText = "ALTER SEQUENCE " + sequence + " RESTART WITH 1";
                command.ExecuteNonQuery();
            }
        }
    }
}