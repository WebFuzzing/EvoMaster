using System;
using System.Collections.Generic;
using System.Data;
using System.Data.Common;
using System.Linq;
using System.Threading;
using EvoMaster.Controller.Api;
using EvoMaster.Client.Util;

namespace EvoMaster.Controller.Controllers.db {
    public static class DbCleaner {
        public static void DropDatabaseTables(DbConnection connection, string schemaName, List<string> tablesToSkip,
            DatabaseType type) {
            if (type != DatabaseType.MYSQL && type != DatabaseType.MARIADB)
                throw new InvalidOperationException("Dropping tables are not supported by " + type);
            ClearDatabase(GetDefaultRetries(type), connection, schemaName, tablesToSkip, type, true);
        }

        public static void ClearDatabase_Postgres(DbConnection connection, string schema = null,
            List<string> tablesToSkip = null) {
            ClearDatabase(connection, tablesToSkip, DatabaseType.POSTGRES, schema);
        }

        public static void ClearDatabase(DbConnection connection, List<string> tablesToSkip, DatabaseType type,
            string schema = null) {
            ClearDatabase(GetDefaultRetries(type), connection, schema ?? GetSchema(type), tablesToSkip, type);
        }

        private static void ConnectionStateCheck(DbConnection connection) {
            // Man: need to check whether to throw an exception here, the connection should open in startSut.
            if (connection.State == ConnectionState.Closed) {
                connection.Open();
            }
        }

        // Man: restructure clearDatabase_Postgres here if Andrea agrees with.
        // for more information about dbcommand and dbconnection
        // https://docs.microsoft.com/en-us/dotnet/framework/data/adonet/dbconnection-dbcommand-and-dbexception
        public static void ClearDatabase(int retries, DbConnection connection, string schemaName,
            List<string> tablesToSkip, DatabaseType type, bool doDropTable = false) {
            // Check for valid DbConnection.
            if (connection != null) {
                try {
                    ConnectionStateCheck(connection);

                    // Create the command.
                    var command = connection.CreateCommand();

                    //disable referential integrity constraint for cleaning the data in tables
                    DisableReferentialIntegrity(command, type);

                    CleanDataInTables(tablesToSkip, command, type, schemaName, GetDefaultTrunctionSingleCommand(type),
                        doDropTable);

                    ResetSequences(command, type, schemaName);

                    //enable referential integrity constraints after the clean
                    EnableReferentialIntegrity(command, type);
                }
                catch (Exception ex) //catch DBException, InvalidOperationException or others
                {
                    /*
                        note from DbCleaner.java
                        this could happen if there is a current transaction with a lock on any table.
                        We could check the content of INFORMATION_SCHEMA.LOCKS, or simply look at error message
                     */
                    var msg = ex.Message;
                    if (msg.ToLower().Contains("timeout")) {
                        if (retries > 0) {
                            SimpleLogger.Warn("Timeout issue with cleaning DB. Trying again.");
                            //let's just wait a bit, and retry
                            Thread.Sleep(2000);
                            retries--;
                            ClearDatabase(retries, connection, schemaName, tablesToSkip, type, doDropTable);
                        }
                        else {
                            SimpleLogger.Error("Giving up cleaning the DB. There are still timeouts.");
                        }
                    }

                    // with java: throw new RuntimeException(e);
                    throw new SystemException(msg, ex);
                }
            }
        }

        private static void CleanDataInTables(List<string> tablesToSkip, DbCommand command,
            DatabaseType type,
            string schema,
            bool singleCommand, bool doDropTable) {
            // Retrieve all tables
            command.CommandText = GetAllTableCommand(type, schema);
            command.CommandType = CommandType.Text;
            var reader = command.ExecuteReader();

            ISet<string> tables = new HashSet<string>();
            while (reader.Read()) {
                tables.Add(reader.GetString(0));
            }

            reader.Close();

            if (tables.Count == 0) {
                throw new InvalidOperationException("Could not find any table");
            }

            //check tablesToSkip
            if (tablesToSkip != null) {
                foreach (var s in tablesToSkip) {
                    var exist = tables.ToList().FindAll(t => s.Equals(t, StringComparison.InvariantCultureIgnoreCase));
                    if (exist.Count == 0) {
                        var msg = "Asked to skip tables '" + s + "', but it does not exist.";
                        msg += " Existing tables in schema '" + schema + "': [" +
                               string.Join(",", tables) + "]";
                        throw new InvalidOperationException(msg);
                    }
                }
            }

            ISet<string> tablesHaveIdentifies = new HashSet<string>();
            if (type == DatabaseType.MS_SQL_SERVER) {
                command.CommandText = GetAllTableHasIdentify(type, schema);
                command.CommandType = CommandType.Text;
                var readerHasIdentify = command.ExecuteReader();
                while (readerHasIdentify.Read()) {
                    tablesHaveIdentifies.Add(readerHasIdentify.GetString(0));
                }

                readerHasIdentify.Close();
            }


            var tablesToClear = tables.ToList().FindAll(t =>
                tablesToSkip == null || tablesToSkip.Count == 0 ||
                !tablesToSkip.Any(s => t.Equals(s, StringComparison.InvariantCultureIgnoreCase)));

            if (singleCommand) {
                if (!type.Equals(DatabaseType.POSTGRES)) {
                    throw new InvalidOperationException(
                        "do not support for cleaning all data by one single command for " + type);
                }

                tablesToClear = tablesToClear.Select(x => $"\"{x}\"").ToList();
                var all = string.Join(",", tablesToClear);

                //TODO: Man need to check with Amid, this command seems different from java
                TruncateTablesBySingleCommand(command, all);
            }
            else {
                //note from DbCleaner.java: if one at a time, need to make sure to first disable FK checks
                foreach (var t in tablesToClear) {
                    if (doDropTable) DropTables(command, t);
                    else {
                        if (type == DatabaseType.MS_SQL_SERVER) DeleteTables(command, t, schema,tablesHaveIdentifies);
                        else TruncateTables(command, t);
                    }
                }
            }
        }

        private static string GetAllTableHasIdentify(DatabaseType type, string schema) {
            if (type != DatabaseType.MS_SQL_SERVER)
                throw new InvalidOperationException("getAllTableHasIdentify only supports for MS_SQL_SERVER, not for " +
                                                    type);
            return GetAllTableCommand(type, schema) +
                   " AND OBJECTPROPERTY(OBJECT_ID(TABLE_NAME), 'TableHasIdentity') = 1";
        }

        private static void DropTables(DbCommand command, string table) {
            command.CommandText = "DROP TABLE IF EXISTS " + table;
            command.ExecuteNonQuery();
        }

        private static void DeleteTables(DbCommand command, string table, string schmea, ISet<string> tableHasIdentify){
            var tableWithSchema = table;
            /*
             * for MS SQL, the delete command should consider its schema,
             * but such schema info is not returned when retrieving table name with select command, see [GetAllTableCommand]
             * then here, we need to reformat the table name with schema
             */
            if (schmea.Length > 0 && !schmea.Equals(GetSchema(DatabaseType.MS_SQL_SERVER))){
                tableWithSchema = schmea + "." + table;
            }
            command.CommandText = "DELETE FROM " + tableWithSchema;
            command.ExecuteNonQuery();
            if (tableHasIdentify.Contains(table)) {
                command.CommandText = "DBCC CHECKIDENT ('" + tableWithSchema + "', RESEED, 0)";
                command.ExecuteNonQuery();
            }
        }

        private static void TruncateTables(DbCommand command, string table) {
            command.CommandText = "TRUNCATE TABLE " + table;
            command.ExecuteNonQuery();
        }

        private static void TruncateTablesBySingleCommand(DbCommand command, string tables) {
            command.CommandText = "TRUNCATE " + tables;
            command.ExecuteNonQuery();
        }

        private static void ResetSequences(DbCommand command, DatabaseType type, string schemaName) {
            ISet<string> sequences = new HashSet<string>();
            command.CommandText = GetAllSequenceCommand(type, schemaName);
            command.CommandType = CommandType.Text;
            var reader = command.ExecuteReader();
            while (reader.Read()) {
                sequences.Add(reader.GetString(0));
            }

            reader.Close();
            foreach (var sequence in sequences) {
                command.CommandText = ResetSequenceCommand(sequence, type);
                command.ExecuteNonQuery();
            }
            /*
                from java:
                Note: we reset all sequences from 1. But the original database might
                have used a different value.
                In most cases (99.99%), this should not be a problem.
                We could allow using different values in this API... but, maybe just easier
                for the user to reset it manually if really needed?
            */
        }

        private static string GetSchema(DatabaseType type) {
            return type switch {
                DatabaseType.MS_SQL_SERVER => "dbo",
                DatabaseType.H2 => "PUBLIC",
                DatabaseType.POSTGRES => "public",
                _ => throw new DbUnsupportedException(type)
            };
        }


        private static string GetAllTableCommand(DatabaseType type, string schema) {
            var command =
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where (TABLE_TYPE='TABLE' OR TABLE_TYPE='BASE TABLE')";
            switch (type) {
                case DatabaseType.MS_SQL_SERVER:
                // for mysql, schema is dbname
                case DatabaseType.MYSQL:
                case DatabaseType.MARIADB:
                case DatabaseType.POSTGRES:
                    if (schema.Length == 0) return command;
                    else
                        return command + " AND TABLE_SCHEMA='" + schema + "'";
            }

            throw new DbUnsupportedException(type);
        }

        private static string GetAllSequenceCommand(DatabaseType type, string schema) {
            var command = "SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES";
            switch (type) {
                case DatabaseType.MYSQL:
                case DatabaseType.MARIADB:
                    return GetAllTableCommand(type, schema);
                case DatabaseType.POSTGRES:
                case DatabaseType.MS_SQL_SERVER:
                    if (schema.Length == 0) return command;
                    else return command + " WHERE SEQUENCE_SCHEMA='" + schema + "'";
            }

            throw new DbUnsupportedException(type);
        }

        private static string ResetSequenceCommand(string sequence, DatabaseType type) {
            switch (type) {
                case DatabaseType.MARIADB:
                case DatabaseType.MYSQL:
                    return "ALTER TABLE " + sequence + " AUTO_INCREMENT=1;";
                case DatabaseType.MS_SQL_SERVER:
                case DatabaseType.POSTGRES:
                    return "ALTER SEQUENCE " + sequence + " RESTART WITH 1";
            }

            throw new DbUnsupportedException(type);
        }

        private static int GetDefaultRetries(DatabaseType type) {
            switch (type) {
                case DatabaseType.MS_SQL_SERVER:
                case DatabaseType.POSTGRES: return 0;
                default:
                    return 3;
            }
        }

        private static bool GetDefaultTrunctionSingleCommand(DatabaseType type) {
            return type switch {
                DatabaseType.POSTGRES => true,
                _ => false
            };
        }

        private static void DisableReferentialIntegrity(DbCommand command, DatabaseType type) {
            switch (type) {
                case DatabaseType.POSTGRES: break;
                case DatabaseType.MS_SQL_SERVER:
                    SqlScriptRunner.ExecCommand(command,
                        "EXEC sp_MSForEachTable \"ALTER TABLE ? NOCHECK CONSTRAINT all\"");
                    break;
                case DatabaseType.H2:
                    SqlScriptRunner.ExecCommand(command, "SET REFERENTIAL_INTEGRITY FALSE");
                    break;
                case DatabaseType.MYSQL:
                    SqlScriptRunner.ExecCommand(command, "SET @@foreign_key_checks = 0;");
                    break;
                case DatabaseType.OTHER:
                    throw new DbUnsupportedException(type);
                default:
                    throw new ArgumentOutOfRangeException(nameof(type), type, null);
            }
        }

        private static void EnableReferentialIntegrity(DbCommand command, DatabaseType type) {
            switch (type) {
                case DatabaseType.POSTGRES: break;
                case DatabaseType.MS_SQL_SERVER:
                    SqlScriptRunner.ExecCommand(command,
                        "exec sp_MSForEachTable \"ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all\"");
                    break;
                case DatabaseType.H2:
                    SqlScriptRunner.ExecCommand(command, "SET REFERENTIAL_INTEGRITY TRUE");
                    break;
                case DatabaseType.MARIADB:
                case DatabaseType.MYSQL:
                    SqlScriptRunner.ExecCommand(command, "SET @@foreign_key_checks = 1;");
                    break;
                case DatabaseType.OTHER:
                    throw new DbUnsupportedException(type);
                default:
                    throw new ArgumentOutOfRangeException(nameof(type), type, null);
            }
        }
    }
}