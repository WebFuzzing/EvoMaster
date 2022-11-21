package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.net.*;

public class InetSocketAddressReplacement extends MethodClassReplacement {

    private static ThreadLocal<InetSocketAddress> instance = new ThreadLocal<>();

    public static InetSocketAddress consumeInstance() {

        InetSocketAddress address = instance.get();
        if (address == null) {
            //this should never happen, unless bug in instrumentation, or edge case we didn't think of
            throw new IllegalStateException("No instance to consume");
        }
        instance.set(null);
        return address;
    }

    private static void addInstance(InetSocketAddress x) {
        InetSocketAddress address = instance.get();
        if (address != null) {
            //this should never happen, unless bug in instrumentation, or edge case we didn't think of
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Override
    public Class<?> getTargetClass() {
        return InetSocketAddress.class;
    }

    @Replacement(
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.NET,
            replacingConstructor = true
    )
    public static void InetSocketAddress(String hostname, int port) {
        ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo("TCP", hostname, port);

        InetSocketAddress address = null;

        if (ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())) {
            String ip = ExecutionTracer.getExternalMapping(remoteHostInfo.signature());

            try {
                address = new InetSocketAddress(InetAddress.getByName(ip), port);
            } catch (Exception e) {
                address = new InetSocketAddress(hostname, port);
            }
        } else {
            ExecutionTracer.addExternalServiceHost(remoteHostInfo);
        }

        addInstance(address);

    }

}
