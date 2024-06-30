package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.*;
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.instrumentation.staticstate.MethodReplacementPreserveSemantics;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.IOException;
import java.net.*;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceUtils.collectExternalServiceInfo;

public class SocketClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return Socket.class;
    }


    @Replacement(
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.NET,
            replacingStatic = false,
            usageFilter = UsageFilter.ANY
    )
    public static void connect(Socket caller, SocketAddress endpoint) throws IOException {
        connect(caller, endpoint, 0);
    }


    @Replacement(
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.NET,
            replacingStatic = false,
            usageFilter = UsageFilter.ANY
    )
    public static void connect(Socket caller, SocketAddress endpoint, int timeout) throws IOException {
        if (MethodReplacementPreserveSemantics.shouldPreserveSemantics) {
            SimpleLogger.warn("Preserving semantics: java.net.socket");
            caller.connect(endpoint, timeout);
            return;
        }

        if (!(endpoint instanceof InetSocketAddress)) {
            SimpleLogger.warn("not handle the type of socket address yet: " + endpoint.getClass().getName());
            caller.connect(endpoint, timeout);
            return;
        }

        InetSocketAddress socketAddress = (InetSocketAddress) endpoint;

            /*
                We MUST NOT call getHostName() anywhere in EM.
                On Windows, it can take more than 4 seconds when dealing with a fake hostname.
                This latter is common case when dealing with microservices where connections are done on localhost,
                and we still want to mock them, so we give them a fake hostname.
                A concrete example in EMB is CWA.
             */

        if (ExternalServiceUtils.skipHostnameOrIp(socketAddress.getHostString())
                || ExecutionTracer.skipHostnameAndPort(socketAddress.getHostString(), socketAddress.getPort())
        ) {
            caller.connect(endpoint, timeout);
            return;
        }

        ExternalServiceUtils.analyzeDnsResolution(socketAddress.getHostString());

        //reverse lookup... socketAddress could be a fake local address
        String hostname = ExecutionTracer.getRemoteHostname(socketAddress.getHostString());
        ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(
                ExternalServiceSharedUtils.DEFAULT_SOCKET_CONNECT_PROTOCOL,
                hostname,
                socketAddress.getPort()
        );
        String[] ipAndPort = collectExternalServiceInfo(remoteHostInfo, socketAddress.getPort());
        String localIp = ipAndPort[0];
        int port = Integer.parseInt(ipAndPort[1]);

                /*
                    Socket information will be replaced if there is a mapping available for the given address.
                    Inet replacement will pass down the local IP address to Socket instead of the remote host name.
                    Which will create another entry to mock. This will work, but created test case will have
                    local IP address as the DNS record. Actual remote hostname will be lost since Socket will keep
                    passing the replaced IP information to core with port. To avoid this, a reverse lookup is done
                    and if there is a mapping available then Socket will use that value to connect. Otherwise,
                    nothing will happen.
                 */

        InetSocketAddress replaced = new InetSocketAddress(InetAddress.getByName(localIp), port);
        caller.connect(replaced, timeout);

        if (!ExecutionTracer.hasMappingForLocalAddress(socketAddress.getHostString())) {
            assert localIp.equals(ExecutionTracer.getDefaultSinkholeAddress());
        }
    }
}
