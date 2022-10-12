package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

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

                ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo("TCP", socketAddress.getHostName(), socketAddress.getPort());
                ExecutionTracer.addExternalServiceHost(remoteHostInfo);


                if (ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())) {
                    String ip  = ExecutionTracer.getExternalMapping(remoteHostInfo.signature());
                    InetSocketAddress replaced = new InetSocketAddress(InetAddress.getByName(ip), socketAddress.getPort());
                    caller.connect(replaced, timeout);
                    return;
                }
            }
        }

        caller.connect(endpoint, timeout);
    }
}
