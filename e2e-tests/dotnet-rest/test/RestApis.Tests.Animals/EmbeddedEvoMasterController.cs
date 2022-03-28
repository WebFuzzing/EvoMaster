using System;
using System.Collections.Generic;
using System.ServiceModel;
using System.Threading.Tasks;
using EvoMaster.Controller;
using EvoMaster.Controller.Api;
using EvoMaster.Controller.Controllers.db;
using EvoMaster.Controller.Problem;
using DotNet.Testcontainers.Containers.Builders;
using DotNet.Testcontainers.Containers.Configurations.Databases;
using DotNet.Testcontainers.Containers.Modules.Abstractions;
using DotNet.Testcontainers.Containers.Modules.Databases;
using EvoMaster.Client.Util;
using Npgsql;

namespace RestApis.Tests.Animals.Controller {
    public class EmbeddedEvoMasterController : EmbeddedSutController {
        private bool _isSutRunning;
        private int _sutPort;
        private TestcontainerDatabase _database;
        private NpgsqlConnection _connection;

        public static void Main(string[] args) {
            
            var embeddedEvoMasterController = new EmbeddedEvoMasterController();

            if (args.Length > 0) {
                var controllerPort = int.Parse(args[0]);
                embeddedEvoMasterController.SetControllerPort(controllerPort);
            }

            var instrumentedSutStarter = new InstrumentedSutStarter(embeddedEvoMasterController);
            
            SimpleLogger.Info("Driver is starting...\n");

            instrumentedSutStarter.Start();
        }

        public override string GetDatabaseDriverName() => null;

        public override List<AuthenticationDto> GetInfoForAuthentication() => null;

        public override string GetPackagePrefixesToCover() => "RestApis.Animals";

        public override OutputFormat GetPreferredOutputFormat() => OutputFormat.CSHARP_XUNIT;

        //TODO: check again
        public override IProblemInfo GetProblemInfo() =>
            GetSutPort() != 0
                ? new RestProblem("http://localhost:" + GetSutPort() + "/swagger/v1/swagger.json", null)
                : new RestProblem(null, null);

        public override bool IsSutRunning() => _isSutRunning;

        public override void ResetStateOfSut() {
            DbCleaner.ClearDatabase_Postgres(_connection, null,
                new List<string> { "Mammals" });
        }

        public override string StartSut() {
            //TODO: check this again
            var ephemeralPort = GetEphemeralTcpPort();

            Task.Run(async () => {
                var connectionString = await StartContainerAsync();
                RestApis.Animals.Program.Main(new[] { ephemeralPort.ToString(), connectionString });
            });

            WaitUntilSutIsRunning(ephemeralPort);

            _sutPort = ephemeralPort;

            _isSutRunning = true;

            return $"http://localhost:{ephemeralPort}";
        }

        public override void StopSut() {
            RestApis.Animals.Program.Shutdown();

            //TODO
            _connection.Close();

            _database.StopAsync().GetAwaiter().GetResult();

            _isSutRunning = false;
        }

        protected int GetSutPort() => _sutPort;

        private async Task<string> StartContainerAsync() {
            var postgresBuilder = new TestcontainersBuilder<PostgreSqlTestcontainer>()
                .WithDatabase(new PostgreSqlTestcontainerConfiguration {
                    Database = "AnimalsDatabase",
                    Username = "user",
                    Password = "password123"
                })
                .WithExposedPort(5432);

            _database = postgresBuilder.Build();
            await _database.StartAsync();

            _connection = new NpgsqlConnection(_database.ConnectionString);
            await _connection.OpenAsync();

            //No idea why the password is missing in the connection string
            return $"{_connection.ConnectionString};Password={_database.Password}";
        }
    }
}