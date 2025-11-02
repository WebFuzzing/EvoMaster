using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using EvoMaster.Controller.Api;
using EvoMaster.Instrumentation;
using EvoMaster.Instrumentation.StaticState;
using Microsoft.Extensions.Configuration;
using Action = EvoMaster.Instrumentation.Action;

namespace EvoMaster.Controller {
    public abstract class EmbeddedSutController : SutController {
        public sealed override UnitsInfoDto GetUnitsInfoDto() {
            // return GetUnitsInfoDto(UnitsInfoRecorder.GetInstance());
            return GetUnitsInfoDto(UnitsInfoRecorder.GetInstance());
        }

        public sealed override bool IsInstrumentationActivated() => false;

        public sealed override void NewActionSpecificHandler(ActionDto dto) {
            ExecutionTracer.SetAction(new Action(dto.Index, dto.InputVariables));
        }

        public override IList<TargetInfo> GetTargetInfos(IEnumerable<int> ids) {
            return InstrumentationController.GetTargetInfos(ids);
        }

        public override IList<AdditionalInfo> GetAdditionalInfoList() {
            return InstrumentationController.GetAdditionalInfoList();
        }

        public sealed override void NewSearch() {
            InstrumentationController.ResetForNewSearch();
        }

        public sealed override void NewTestSpecificHandler() {
            InstrumentationController.ResetForNewTest();
        }

        public override void SetKillSwitch(bool b) => ExecutionTracer.SetKillSwitch(b);

        /// <summary>
        /// This method checks whether the SUT is listening on the port number
        /// </summary>
        /// <param name="port">The port number on the localhost</param>
        /// <param name="timeout">The amount of time in seconds the driver should give up if the SUT did not start </param>
        protected static void WaitUntilSutIsRunning(int port, int timeout = 30) {
            var task = Task.Run(() => {
                using var tcpClient = new TcpClient();
                while (true) {
                    try {
                        tcpClient.Connect("127.0.0.1", port);
                        break;
                    }
                    catch (Exception) {
                        Thread.Sleep(50);
                    }
                }
            });

            if (task.Wait(TimeSpan.FromSeconds(timeout)))
                return;

            throw new TimeoutException($"The SUT didn't start within {timeout} seconds on port {port}.");
        }

        protected static IConfiguration GetConfiguration(string settingFileName) {
            var config = new ConfigurationBuilder()
                .AddJsonFile(settingFileName)
                .Build();
            return config;
        }
    }
}