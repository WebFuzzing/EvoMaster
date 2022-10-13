package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.evomaster.client.java.utils.SimpleLogger;

import java.io.IOException;
import java.net.*;

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
    public static void connect(Socket caller, SocketAddress endpoint, int timeout) throws IOException {
        if (endpoint instanceof InetSocketAddress){
            InetSocketAddress socketAddress = (InetSocketAddress) endpoint;

            if (socketAddress.getAddress() instanceof Inet4Address){

                ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(ExternalServiceSharedUtils.DEFAULT_SOCKET_CONNECT_PROTOCOL, socketAddress.getHostName(), socketAddress.getPort());
                ExecutionTracer.addExternalServiceHost(remoteHostInfo);

                String signature = remoteHostInfo.signature();
                int port = socketAddress.getPort();
                if (!ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())) {
                    ExecutionTracer.addEmployedDefaultWMHost(remoteHostInfo);
                    signature = ExternalServiceSharedUtils.getWMDefaultSignature(remoteHostInfo.getProtocol(), socketAddress.getPort());
                    port = ExternalServiceSharedUtils.getDefaultWMPort(signature);
                }
                String ip  = ExecutionTracer.getExternalMapping(signature);

                InetSocketAddress replaced = new InetSocketAddress(InetAddress.getByName(ip), port);
                caller.connect(replaced, timeout);
                return;
            }
        }
        SimpleLogger.warn("not handle the type of endpoint yet:" + endpoint.getClass().getName());
        caller.connect(endpoint, timeout);
    }
}
