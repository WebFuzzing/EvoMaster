using System;
using System.Linq;
using System.Threading.Tasks;
using Docker.DotNet;
using Docker.DotNet.Models;

namespace EvoMaster.Controller.Controllers.db
{
    // credits to https://www.meziantou.net/2018/10/08/integration-testing-using-a-docker-container
    internal abstract class DockerContainerBase
    {
        protected const string ContainerPrefix = "EvoMaster-DbContainer-";

        protected DockerContainerBase(string imageName, string containerName)
        {
            ImageName = imageName;
            ContainerName = containerName;
        }

        private string ImageName { get; }

        private string ContainerName { get; }

        private ContainerStartAction StartAction { get; set; } = ContainerStartAction.NONE;

        public async Task StartAsync(IDockerClient client, int timeout)
        {
            if (StartAction != ContainerStartAction.NONE) return;

            var images =
                await client.Images.ListImagesAsync(new ImagesListParameters {MatchName = ImageName});

            if (images.Count == 0)
            {
                await client.Images.CreateImageAsync(
                    new ImagesCreateParameters {FromImage = ImageName, Tag = "latest"}, null,
                    new Progress<JSONMessage>());
            }

            var list = await client.Containers.ListContainersAsync(new ContainersListParameters
            {
                All = true
            });

            var container = list.FirstOrDefault(x => x.Names.Contains("/" + ContainerName));

            if (container == null)
            {
                await CreateContainerAsync(client);
            }
            else
            {
                if (container.State == "running")
                {
                    StartAction = ContainerStartAction.EXTERNAL;
                    return;
                }
            }


            var started = await client.Containers.StartContainerAsync(ContainerName, new ContainerStartParameters());
            if (!started) throw new InvalidOperationException($"Container '{ContainerName}' did not start!!!!");

            var i = 0;
            var eachDelay = (timeout * 1000) / 20 < 5000 ? 5000 : (timeout * 1000) / 20;

            while (!await IsReadyAsync())
            {
                i++;

                if (i > 20)
                    throw new TimeoutException(
                        $"Container {ContainerName} does not seem to be responding in a timely manner");

                await Task.Delay(eachDelay);
            }

            StartAction = ContainerStartAction.STARTED;
        }

        private async Task CreateContainerAsync(IDockerClient client)
        {
            var hostConfig = ToHostConfig();
            var config = ToConfig();

            await client.Containers.CreateContainerAsync(new CreateContainerParameters(config)
            {
                Image = ImageName,
                Name = ContainerName,
                Tty = true,
                HostConfig = hostConfig
            });
        }

        public Task RemoveAsync(IDockerClient client)
        {
            return client.Containers.RemoveContainerAsync(ContainerName,
                new ContainerRemoveParameters {Force = true, RemoveVolumes = true});
        }

        protected abstract Task<bool> IsReadyAsync();

        protected abstract HostConfig ToHostConfig();

        protected abstract Config ToConfig();

        public override string ToString()
        {
            return $"{nameof(ImageName)}: {ImageName}, {nameof(ContainerName)}: {ContainerName}";
        }

        public static async Task CleanupOrphanedContainersAsync(DockerClient dockerClient)
        {
            var containers = await dockerClient.Containers.ListContainersAsync(new ContainersListParameters
            {
                All = true
            });

            var orphanedContainers = containers.Where(_ => _.Names.Any(__ => __.Contains(ContainerPrefix)));

            foreach (var container in orphanedContainers)
            {
                await dockerClient.Containers.RemoveContainerAsync(container.ID,
                    new ContainerRemoveParameters {Force = true, RemoveVolumes = true});
            }
        }

        private enum ContainerStartAction
        {
            NONE,
            STARTED,
            EXTERNAL
        }
    }
}