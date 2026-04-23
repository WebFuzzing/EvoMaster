using System;
using System.Collections.Generic;
using System.Data.Common;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;

namespace EvoMaster.Controller.Controllers.db {
    
        
    //Man: I just enable a method to execute the command, and this needs to be further extended based on java version.
    public class SqlScriptRunner {
        
        /*
        Class adapted from ScriptRunner
        https://github.com/BenoitDuffez/ScriptRunner/blob/master/ScriptRunner.java

        released under Apache 2.0 license
        */

        private const string DEFAULT_DELIMITER = ";";
            
        /**
         * regex to detect delimiter.
         * ignores spaces, allows delimiter in comment, allows an equals-sign
         */
        //public static final Pattern delimP = Pattern.compile("^\\s*(--)?\\s*delimiter\\s*=?\\s*([^\\s]+)+\\s*.*$", Pattern.CASE_INSENSITIVE);
        public const string delimPatternStr = "^\\s*(--)?\\s*delimiter\\s*=?\\s*([^\\s]+)+\\s*.*$";
        
        // private const string SINGLE_APOSTROPHE = "'";

        // private const string DOUBLE_APOSTROPHE = "''";

        private string delimiter = DEFAULT_DELIMITER;
        private bool fullLineDelimiter = false;
        
        /**
         * Default constructor
         */
        public SqlScriptRunner() {
        }

        public void setDelimiter(String delimiter, bool fullLineDelimiter) {
            this.delimiter = delimiter;
            this.fullLineDelimiter = fullLineDelimiter;
        }
        
        public static void ExecCommand(DbConnection connection, string command) {
            var cmd = connection.CreateCommand();
            ExecCommand(cmd, command);
        }

        //https://docs.microsoft.com/en-us/dotnet/framework/data/adonet/executing-a-command
        //https://docs.microsoft.com/en-us/dotnet/api/system.data.common.dbcommand
        public static void ExecCommand(DbCommand command, string commandText) {
            command.CommandText = commandText;
            command.ExecuteNonQuery();
        }

        public static DbDataReader ExecCommandWithDataReader(DbCommand command, string commandText) {
            command.CommandText = commandText;
            return command.ExecuteReader();
        }


        public static DbDataReader ExecCommandWithDataReader(DbConnection connection, string command) {
            var cmd = connection.CreateCommand();
            return ExecCommandWithDataReader(cmd, command);
        }
        public static void RunCommands(DbConnection connection, List<string> commands) {
            try {
                foreach  (var command in commands) {
                    ExecCommand(connection, command);
                }
            } catch (Exception e) {
                throw new SystemException("Error running script.  Cause: " + e, e);
            }
        }
        public List<string> ReadCommands(StreamReader reader) {

            List<string> list = new List<string>();

            StringBuilder command = null;
            try {
                string line;

                while ((line = reader.ReadLine()) != null) {
                    if (command == null) {
                        command = new StringBuilder();
                    }

                    string trimmedLine = line.Trim();
                    Match delimMatch = Regex.Match(trimmedLine, delimPatternStr, RegexOptions.IgnoreCase); 
                    if (trimmedLine.Length == 0
                        || trimmedLine.StartsWith("//")
                        || trimmedLine.StartsWith("--")) {
                        // Do nothing
                    } else if (delimMatch.Success) {
                        setDelimiter(delimMatch.Groups[2].Value, false);
                    } else if (!fullLineDelimiter
                               && trimmedLine.EndsWith(delimiter)
                               || fullLineDelimiter
                               && trimmedLine.Equals(delimiter)) {

                        command.Append(line, 0, line.LastIndexOf(delimiter));
                        command.Append(" ");

                        list.Add(command.ToString());
                        command = null;

                    } else {
                        command.Append(line);
                        command.Append("\n");
                    }
                }

                if (command != null && command.Length > 0) {
                    list.Add(command.ToString());
                }

            } catch (IOException e) {
                throw new SystemException(e.Message,e);
            }

            return list;
        }
    }
}