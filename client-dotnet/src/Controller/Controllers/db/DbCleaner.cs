using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Linq;
using System.Threading;

using Client.Util;

namespace Controller.Controllers.db
{
    
    public class DbCleaner
    {

        public static void clearDatabase_H2(DbConnection connection) {
            clearDatabase_H2(connection, null);
        }

        public static void clearDatabase_H2(DbConnection connection, List<String> tablesToSkip) {
            clearDatabase_H2(connection, "PUBLIC", tablesToSkip, DatabaseType.NOT_SPECIFIED);
        }
        
        public static void clearDatabase_H2(DbConnection connection, String schemaName, List<String> tablesToSkip, DatabaseType type) {
            clearDatabase_H2(3, connection, schemaName, tablesToSkip,type);
        }
        
        private static void connectionStateCheck(DbConnection connection)
        {
            // Man: need to check whether to throw an exception here, the connection should open in startSut.
            if (connection.State == ConnectionState.Closed)
            {
                connection.Open();
            }
        }

        // Man: restructure clearDatabase_Postgres here if Andrea agrees with.
        public static void clearDatabase_H2(int retries, DbConnection connection, string schemaName, List<string> tablesToSkip, DatabaseType type)
        {
            // Check for valid DbConnection.
            if (connection != null)
            {
                try
                {
                    connectionStateCheck(connection);
                        
                    // Create the command.
                    DbCommand command = connection.CreateCommand();
                    
                    //handling referential integrity constraint
                    switch (type)
                    {
                        case DatabaseType.NOT_SPECIFIED:
                            command.CommandText = "SET REFERENTIAL_INTEGRITY FALSE";
                            command.ExecuteNonQuery();
                            break;
                            
                        case DatabaseType.MySQL:
                            command.CommandText = "SET @@foreign_key_checks = 0;";
                            command.ExecuteNonQuery();
                            break;
                    }
                    
                    truncateTables(tablesToSkip, command, schemaName,false);
                    
                    switch (type)
                    {
                        case DatabaseType.NOT_SPECIFIED:
                            command.CommandText = "SET REFERENTIAL_INTEGRITY TRUE";
                            command.ExecuteNonQuery();
                            
                            resetSequences(command, schemaName);
                            break;
                            
                        case DatabaseType.MySQL:
                            command.CommandText = "SET @@foreign_key_checks = 1;";
                            command.ExecuteNonQuery();
                            break;
                    }
                }
                catch (Exception ex) //catch DBException, InvalidOperationException or others
                {
                    /*
                        note from DbCleaner.java
                        this could happen if there is a current transaction with a lock on any table.
                        We could check the content of INFORMATION_SCHEMA.LOCKS, or simply look at error message
                     */
                    String msg = ex.Message;
                    if(msg.ToLower().Contains("timeout")){
                        if(retries > 0) {
                            SimpleLogger.Warn("Timeout issue with cleaning DB. Trying again.");
                            //let's just wait a bit, and retry
                            Thread.Sleep(2000);
                            retries--;
                            clearDatabase_H2(retries, connection, schemaName, tablesToSkip, type);
                        } else {
                            SimpleLogger.Error("Giving up cleaning the DB. There are still timeouts.");
                        }
                    }
                    // with java: throw new RuntimeException(e);
                    throw new SystemException();
                }
            }
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
                    connectionStateCheck(connection);
                        
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