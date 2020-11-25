using System.Diagnostics;

namespace RestApis.Tests.HelloWorld.Controller {

    //TODO: Move this class from here
    public static class ShellHelper {
        public static Process Bash (this string cmd) {
            var escapedArgs = cmd.Replace ("\"", "\\\"");

            var process = new Process () {
                StartInfo = new ProcessStartInfo {
                FileName = "/bin/bash",
                Arguments = $"-c \"{escapedArgs}\"",
                RedirectStandardOutput = true,
                UseShellExecute = false,
                CreateNoWindow = true,
                }
            };
            process.Start ();

            return process;
        }
    }
}