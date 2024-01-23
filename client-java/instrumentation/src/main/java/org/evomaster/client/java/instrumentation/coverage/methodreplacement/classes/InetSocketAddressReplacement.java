package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceInfoUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceInfoUtils.collectExternalServiceInfo;

public class InetSocketAddressReplacement implements MethodReplacementClass {

    private static ThreadLocal<InetSocketAddress> instance = new ThreadLocal<>();

    @Override
    public Class<?> getTargetClass() {
        return InetSocketAddress.class;
    }

    private static void addInstance(InetSocketAddress addr) {
        InetSocketAddress inetSocketAddress = instance.get();

        if (inetSocketAddress != null) {
            throw new IllegalStateException("No instance to consume");
        }

        instance.set(addr);
    }

    public static InetSocketAddress consumeInstance() {
        InetSocketAddress inetSocketAddress = instance.get();

        if (inetSocketAddress == null) {
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return inetSocketAddress;
    }

    @Replacement(
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.EXT_0,
            replacingConstructor = true
    )
    public static void InetSocketAddress(InetAddress addr, int port) {
        InetSocketAddress inetSocketAddress;

        if (addr == null) {
            inetSocketAddress = new java.net.InetSocketAddress(addr, port);
        } else {
            if ((ExternalServiceInfoUtils.skipHostnameOrIp(addr.getHostName())
                    || ExecutionTracer.skipHostnameAndPort(addr.getHostName(), port))) {
                inetSocketAddress = new java.net.InetSocketAddress(addr, port);
            } else {
                ExternalServiceInfoUtils.analyzeDnsResolution(addr.getHostName());

                if (ExecutionTracer.hasMappingForLocalAddress(addr.getHostName())) {
                    String newHostname = ExecutionTracer.getRemoteHostname(addr.getHostName());
                    ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo(
                            ExternalServiceSharedUtils.DEFAULT_SOCKET_CONNECT_PROTOCOL,
                            newHostname,
                            port
                    );
                    String[] ipAndPort = collectExternalServiceInfo(remoteHostInfo, port);

                    inetSocketAddress = new InetSocketAddress(ipAndPort[0], Integer.parseInt(ipAndPort[1]));
                } else {
                    inetSocketAddress = new java.net.InetSocketAddress(addr, port);
                }
            }
        }

        addInstance(inetSocketAddress);
    }
}
