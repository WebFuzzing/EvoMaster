# Windows and Networking

If you are running _EvoMaster_ on Windows for long periods of time, or if you are running
more than one instance in parallel, you might get error messages regarding running out
of _ephemeral TCP ports_.

This might happen because Windows has much more restrictive default settings compared to 
Linux/Mac when dealing with TCP ports.
If you end up seeing these issues, you might want to use `regedit` to modify 
`HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\TCPIP\Parameters` registry subkeys.
In particular, add new `REG_DWORD` values for:

* `TcpTimedWaitDelay `: can use something like `60`.
* `MaxUserPort`: can use something like `32768`.

As this would impact your whole OS, we recommend doing such changes if and only if you start to see
problems with _EvoMaster_ related to ephemeral ports. 
 