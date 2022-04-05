# Multiple loopback interface for tests

WireMock tests require different loopback address other than 127.0.0.1.

By default, in macOS, this address is not available to enable it run 
the following command.

`sudo ifconfig lo0 alias 127.0.0.2`

>> This is not persistent, each restart this will be disabled. So each 
> time have to enable it before executing tests related to wiremock 
> module. By creating a launch daemon in macOS it can be made persistent.

_For other operating systems, steps will be added soon once tested._