# Running EvoMaster in Docker

_EvoMaster_ is also released via Docker, under `webfuzzing/evomaster`.

It can be run with:

> docker run webfuzzing/evomaster  \<options\>

Where `<options>` would be the [command-line options](options.md).


## Accessing the generated test files

By default, _EvoMaster_ generates tests under the `/generated_tests` folder.
To access the generated tests after Docker run is completed, you need to mount a folder or a volume pointing to that folder.
An example is mounting the local `./generated_tests`, using for example:

> docker run -v "/$(pwd)/generated_tests":/generated_tests webfuzzing/evomaster  \<options\>

There can be other ways to access the `/generated_tests` folder inside the Docker image (e.g., using volumes), but, for that, you will need to check the Docker documentation.  

## Issues with "localhost" 

An important fact to keep in mind is that references to `localhost` will point to the Docker virtual network, and not your host machine.
This latter can be accessed at `host.docker.internal`.


For example, in __black-box__ testing for REST APIs, if the tested API is running on your machine, an option like

> --bbSwaggerUrl http://localhost:8080/v3/api-docs

would need to be replaced with

> --bbSwaggerUrl http://host.docker.internal:8080/v3/api-docs

Note that here the port `8080` and the the path `/v3/api-docs` are just examples.

An equivalent solution is to use the option

> --dockerLocalhost true

i.e.,

> --bbSwaggerUrl http://localhost:8080/v3/api-docs --dockerLocalhost true

This can be useful if you do not remember or do not want to copy&paste the hostname `host.docker.internal`.

For __white-box__ testing, by default _EvoMaster_ core tool process will try to connect to a driver, listening on `localhost`. 
This will not directly work in Docker.
Such default configuration can be changed with the option:

> --sutControllerHost  host.docker.internal

Still, this might not be enough. If in the driver there are references to `localhost`, either you need to change the code of the driver, or override some parameters via command-line options (e.g. using `--overrideOpenAPIUrl`).
A problem here is that, if you are using _ephemeral_ ports in the driver (as you are recommended to do), then it will not be easy to set up the correct port numbers beforehand on the command line.
To deal with this issue, you can use the option:

> --dockerLocalhost true

This will change the references to `localhost` (if any) in the driver into `host.docker.internal`. 
It will also change the default of `--sutControllerHost`, unless you modify it explicitly.
This way, you can run __white-box__ _EvoMaster_ by using for example:

> docker run -v "/$(pwd)/generated_tests":/generated_tests  webfuzzing/evomaster --dockerLocalhost true


## Handling "em.yaml" configuration file

TODO
