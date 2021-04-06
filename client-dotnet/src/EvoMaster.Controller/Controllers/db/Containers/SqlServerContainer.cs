using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Docker.DotNet.Models;
using Microsoft.Data.SqlClient;

namespace EvoMaster.Controller.Controllers.db
{
    internal class SqlServerContainer : DockerContainerBase
    {
        private const string SaPassword = "password123";
        private readonly int _port;

        public SqlServerContainer(int port)
            : base("mcr.microsoft.com/mssql/server:2017-latest",
                $"{ContainerPrefix}{Guid.NewGuid().ToString()}")
        {
            this._port = port;
        }

        public string GetConnectionString(string database = "master")
        {
            return $"Server=127.0.0.1,{_port};Database={database};User Id=sa;Password={SaPassword};Timeout=5";
        }

        protected override async Task<bool> IsReadyAsync()
        {
            try
            {
                await using var connection = new SqlConnection(GetConnectionString());
                await connection.OpenAsync();

                return true;
            }
            catch (Exception)
            {
                return false;
            }
        }

        protected override HostConfig ToHostConfig()
        {
            return new HostConfig
            {
                PortBindings = new Dictionary<string, IList<PortBinding>>
                {
                    {
                        "1433/tcp",
                        new List<PortBinding>
                        {
                            new PortBinding
                            {
                                HostPort = _port.ToString(),
                                HostIP = "127.0.0.1"
                            }
                        }
                    }
                }
            };
        }

        protected override Config ToConfig()
        {
            return new Config
            {
                Env = new List<string> {"ACCEPT_EULA=Y", $"SA_PASSWORD={SaPassword}", "MSSQL_PID=Developer"}
            };
        }
    }
}