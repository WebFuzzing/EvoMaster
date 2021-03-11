using System;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;
using EvoMaster.Controller.Api;
using Microsoft.Extensions.Configuration;

namespace EvoMaster.Controller
{
    public abstract class EmbeddedSutController : SutController
    {
        public sealed override UnitsInfoDto GetUnitsInfoDto()
        {
            //TODO
            return new UnitsInfoDto();
        }

        public sealed override bool IsInstrumentationActivated() => false;

        public sealed override void NewActionSpecificHandler(ActionDto dto)
        {
            throw new System.NotImplementedException();
        }

        public sealed override void NewSearch()
        {
            //TODO: Implement this method
        }

        public sealed override void NewTestSpecificHandler()
        {
            throw new System.NotImplementedException();
        }

        /// <summary>
        /// This method checks whether the SUT is listening on the port number
        /// </summary>
        /// <param name="port">The port number on the localhost</param>
        /// <param name="timeout">The amount of time in seconds the driver should give up if the SUT did not start </param>
        protected static void WaitUntilSutIsRunning(int port, int timeout = 60)
        {
            var task = Task.Run(() =>
            {
                using var tcpClient = new TcpClient();
                while (true)
                {
                    try
                    {
                        tcpClient.Connect("127.0.0.1", port);
                        break;
                    }
                    catch (Exception)
                    {
                        Thread.Sleep(50);
                    }
                }
            });
            
            if (task.Wait(TimeSpan.FromSeconds(timeout)))
                return;
            
            throw new TimeoutException($"The SUT didn't start within {timeout} seconds on port {port}.");
        }
        protected static IConfiguration GetConfiguration(string settingFileName)
        {
            var config = new ConfigurationBuilder()
                .AddJsonFile(settingFileName)
                .Build();
            return config;
        }
    }
}