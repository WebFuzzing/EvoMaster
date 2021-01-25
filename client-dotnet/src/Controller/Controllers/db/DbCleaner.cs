using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Linq;
using System.Threading;

using Client.Util;

namespace Controller.Controllers.db
{
    
    public static class DbCleaner
    {

        public static void ClearDatabase(DbConnection connection) {
            ClearDatabase(connection, null);
        }
        
        public static void ClearDatabase(DbConnection connection, List<string> tablesToSkip, SupportedDatabaseType type=SupportedDatabaseType.H2) {
            ClearDatabase(GetDefaultRetries(type), connection, GetSchema(type), tablesToSkip,type);
        }

        public static void ClearDatabase_Postgres(DbConnection connection, List<string> tablesToSkip=null)
        {
            ClearDatabase(GetDefaultRetries(SupportedDatabaseType.POSTGRES), connection, GetSchema(SupportedDatabaseType.POSTGRES) ,tablesToSkip, SupportedDatabaseType.POSTGRES);
        }

        private static void ConnectionStateCheck(DbConnection connection)
        {
            // Man: need to check whether to throw an exception here, the connection should open in startSut.
            if (connection.State == ConnectionState.Closed)
            {
                connection.Open();
            }
        }

        // Man: restructure clearDatabase_Postgres here if Andrea agrees with.
        // for more information about dbcommand and dbconnection
        // https://docs.microsoft.com/en-us/dotnet/framework/data/adonet/dbconnection-dbcommand-and-dbexception
        public static void ClearDatabase(int retries, DbConnection connection, string schemaName, List<string> tablesToSkip, SupportedDatabaseType type)
        {
            // Check for valid DbConnection.
            if (connection != null)
            {
                try
                {
                    ConnectionStateCheck(connection);
                        
                    // Create the command.
                    DbCommand command = connection.CreateCommand();
                    
                    //disable referential integrity constraint for cleaning the data in tables
                    DisableReferentialIntegrity(command, type);
                    
                    TruncateTables(tablesToSkip, command, schemaName, GetDefaultTrunctionSingleCommand(type));
                    
                    ResetSequences(command, type);
                    
                    //enable referential integrity constraints after the clean
                    EnableReferentialIntegrity(command,type);
                }
                catch (Exception ex) //catch DBException, InvalidOperationException or others
                {
                    /*
                        note from DbCleaner.java
                        this could happen if there is a current transaction with a lock on any table.
                        We could check the content of INFORMATION_SCHEMA.LOCKS, or simply look at error message
                     */
                    string msg = ex.Message;
                    if(msg.ToLower().Contains("timeout")){
                        if(retries > 0) {
                            SimpleLogger.Warn("Timeout issue with cleaning DB. Trying again.");
                            //let's just wait a bit, and retry
                            Thread.Sleep(2000);
                            retries--;
                            ClearDatabase(retries, connection, schemaName, tablesToSkip, type);
                        } else {
                            SimpleLogger.Error("Giving up cleaning the DB. There are still timeouts.");
                        }
                    }
                    // with java: throw new RuntimeException(e);
                    throw new SystemException();
                }
            }
        }
        
        private static void TruncateTables(List<string> tablesToSkip, DbCommand command, SupportedDatabaseType type)
        {
            TruncateTables(tablesToSkip, command, GetSchema(type), GetDefaultTrunctionSingleCommand(type));
        }

        private static void TruncateTables(List<string> tablesToSkip, DbCommand command, String schema, bool singleCommand) 
        {
            // Retrieve all tables
            command.CommandText = GetAllTableCommand(schema);
            command.CommandType = CommandType.Text;
            var reader = command.ExecuteReader();

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
                    var exist = tables.ToList().FindAll(t => s.Equals(t, StringComparison.InvariantCultureIgnoreCase));
                    if (exist.Count == 0)
                    {
                        var msg = "Asked to skip tables '" + s+ "', but it does not exist.";
                        msg += " Existing tables in schema '"+schema+"': [" +
                               string.Join(",", tables)+ "]";
                        throw new InvalidOperationException(msg);
                    }
                }
            }

            var tablesToClear = tables.ToList().FindAll(t =>
                tablesToSkip == null || tablesToSkip.Count == 0 ||
                !tablesToSkip.Any(s => t.Equals(s, StringComparison.InvariantCultureIgnoreCase)));

            if (singleCommand)
            {
                var all = string.Join(",", tablesToClear);
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

        private static void ResetSequences(DbCommand command, SupportedDatabaseType type)
        {
            ISet<string> sequences = new HashSet<string>();
            command.CommandText = GetAllSequenceCommand(type);
            command.CommandType = CommandType.Text;
            var reader = command.ExecuteReader();
            while (reader.Read())
            {
                sequences.Add(reader.GetString(0));
            }
            reader.Close();
            foreach (var sequence in sequences)
            {
                command.CommandText = ResetSequenceCommand(sequence, type);
                command.ExecuteNonQuery();
            }
        }

        private static string GetSchema(SupportedDatabaseType type)
        {
            return type switch
            {
                SupportedDatabaseType.H2 => "PUBLIC",
                SupportedDatabaseType.MySQL => "db",
                SupportedDatabaseType.POSTGRES => "public",
                _ => throw new InvalidProgramException("NOT SUPPORT")
            };
        }

        private static string GetAllTableCommand(SupportedDatabaseType type)
        {
            return GetAllTableCommand(GetSchema(type));
        }

        private static string GetAllTableCommand(string schema)
        {
            return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='" + schema + "' AND (TABLE_TYPE='TABLE' OR TABLE_TYPE='BASE TABLE')";
        }

        private static string GetAllSequenceCommand(SupportedDatabaseType type)
        {
            return type switch
            {
                // there is no INFORMATION_SCHEMA.SEQUENCES in MySQL
                SupportedDatabaseType.MySQL => GetAllTableCommand(GetSchema(type)),
                _ => GetAllSequenceCommand(GetSchema(type))
            };
        }


        private static string GetAllSequenceCommand(string schema)
        {
            return "SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA='" + schema + "'";
        }

        private static string ResetSequenceCommand(string sequence, SupportedDatabaseType type)
        {
            return type switch
            {
                SupportedDatabaseType.MySQL => "ALTER TABLE " + sequence + " AUTO_INCREMENT=1;",
                _ => "ALTER SEQUENCE " + sequence + " RESTART WITH 1"
            };
        }

        private static int GetDefaultRetries(SupportedDatabaseType type)
        {
            switch (type)
            {
                case SupportedDatabaseType.POSTGRES: return 0;
                default:
                    return 3;
            }
        }

        private static bool GetDefaultTrunctionSingleCommand(SupportedDatabaseType type)
        {
            return type switch
            {
                SupportedDatabaseType.POSTGRES => true,
                _ => false
            };
        }
        
        private static void DisableReferentialIntegrity(DbCommand command, SupportedDatabaseType type)
        {
            switch (type)
            {
                case SupportedDatabaseType.POSTGRES: break;
                case SupportedDatabaseType.H2: 
                    SqlScriptRunner.ExecCommand(command, "SET REFERENTIAL_INTEGRITY FALSE");
                    break;
                case SupportedDatabaseType.MySQL:
                    SqlScriptRunner.ExecCommand(command, "SET @@foreign_key_checks = 0;");
                    break;
                case SupportedDatabaseType.OTHERS:
                    throw new InvalidOperationException("NOT SUPPORT");
                default:
                    throw new ArgumentOutOfRangeException(nameof(type), type, null);
            }
        }
        
        private static void EnableReferentialIntegrity(DbCommand command, SupportedDatabaseType type)
        {
            switch (type)
            {
                case SupportedDatabaseType.POSTGRES: break;
                case SupportedDatabaseType.H2: 
                    SqlScriptRunner.ExecCommand(command, "SET REFERENTIAL_INTEGRITY TRUE");
                    break;
                case SupportedDatabaseType.MySQL:
                    SqlScriptRunner.ExecCommand(command, "SET @@foreign_key_checks = 1;");
                    break;
                case SupportedDatabaseType.OTHERS:
                    throw new InvalidOperationException("NOT SUPPORT");
                default:
                    throw new ArgumentOutOfRangeException(nameof(type), type, null);
            }
        }
    }
}