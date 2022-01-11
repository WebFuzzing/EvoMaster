using System;

namespace EvoMaster.Client.Util {
    //Implementation of this class is a bit different with its java counterpart
    public class SimpleLogger {
        private static readonly object ConsoleLock = new object();

        public static void Debug(string message) {
            PrintMessage(Level.DEBUG, message);
        }

        public static void Info(string message) {
            PrintMessage(Level.INFO, message);
        }

        public static void Warn(string message, Exception e = null) {
            PrintMessage(Level.WARN, message, e);
        }

        public static void Error(string message, Exception e = null) {
            PrintMessage(Level.ERROR, message, e);
        }

        private static void PrintMessage(Level level, string message, Exception e = null) {
            lock (ConsoleLock) {
                if (level.Equals(Level.ERROR)) {
                    Console.ForegroundColor = ConsoleColor.DarkRed;
                }
                else if (level.Equals(Level.WARN)) {
                    Console.ForegroundColor = ConsoleColor.Yellow;
                }

                System.Console.WriteLine($"{level.ToString()}: {message}");

                if (e != null)
                    System.Console.WriteLine(e.StackTrace);

                Console.ResetColor();
            }
        }

        private enum Level {
            DEBUG,
            INFO,
            WARN,
            ERROR,
            OFF
        }
    }
}