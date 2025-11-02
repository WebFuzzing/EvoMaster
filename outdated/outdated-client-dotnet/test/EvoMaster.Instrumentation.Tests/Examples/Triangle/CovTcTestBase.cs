using System;
using System.IO;
using System.Reflection;
using EvoMaster.Instrumentation.Examples.Triangle;
using EvoMaster.Instrumentation_Shared;
using Xunit;

namespace EvoMaster.Instrumentation.Tests.Examples.Triangle {
    public abstract class CovTcTestBase {
        protected RegisteredTargets GetRegisteredTargets() {
            var bin = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);

            if (bin == null) throw new Exception("Executing directory not found");

            var json = File.ReadAllText(Path.Combine(bin, "Targets.json"));

            return Newtonsoft.Json.JsonConvert.DeserializeObject<RegisteredTargets>(json);
        }
    }
}