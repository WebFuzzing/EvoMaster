using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Docker.DotNet.Models;
using Microsoft.Data.SqlClient;

namespace EvoMaster.DatabaseController.Containers {
    internal class SqlServerContainer : DockerContainerBase {
        private readonly string _saPassword;
        private readonly int _port;

        public SqlServerContainer(int port, string saPassword, string imageName)
            : base(imageName,
                $"{ContainerPrefix}{Guid.NewGuid().ToString()}") {
            this._port = port;
            _saPassword = saPassword;
        }

        public string GetConnectionString(string database = "master") {
            return $"Server=127.0.0.1,{_port};Database={database};User Id=sa;Password={_saPassword};Timeout=5";
        }

        protected override async Task<bool> IsReadyAsync() {
            try {
                await using var connection = new SqlConnection(GetConnectionString());
                await connection.OpenAsync();

                return true;
            }
            catch (Exception) {
                return false;
            }
        }

        protected override HostConfig ToHostConfig() {
            return new HostConfig {
                PortBindings = new Dictionary<string, IList<PortBinding>> {
                    {
                        "1433/tcp",
                        new List<PortBinding> {
                            new PortBinding {
                                HostPort = _port.ToString(),
                                HostIP = "127.0.0.1"
                            }
                        }
                    }
                }
            };
        }

        protected override Config ToConfig() {
            return new Config {
                Env = new List<string> { "ACCEPT_EULA=Y", $"SA_PASSWORD={_saPassword}", "MSSQL_PID=Developer" }
            };
        }
    }
}