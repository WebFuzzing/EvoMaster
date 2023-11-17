package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.HostnameResolutionInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceInfoUtils;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.net.*;

/**
 * with socket connection, it will first do host lookup with java.net.InetAddress#getByName
 * if it does not exist, UnknownHostException will be thrown,
 * then the `socket.connect` cannot be reached.
 * in order to make it connected, we could do replacement for
 * 1) collecting host info
 * 2) providing an ip address
 * note that it is not used now, ie, not register it into ReplacementList
 */
public class InetAddressClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return InetAddress.class;
    }

    @Replacement(
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.NET,
            replacingStatic = true,
            usageFilter = UsageFilter.ANY
    )
    public static InetAddress getByName(String host) throws UnknownHostException {
        if (ExternalServiceInfoUtils.skipHostnameOrIp(host) || ExecutionTracer.skipHostname(host))
            return InetAddress.getByName(host);

        try {
            if (ExecutionTracer.hasLocalAddressForHost(host)) {
                String ip = ExecutionTracer.getLocalAddress(host);
                return InetAddress.getByName(ip);
            }
            InetAddress inetAddress = InetAddress.getByName(host);
            ExecutionTracer.addHostnameInfo(new HostnameResolutionInfo(host, inetAddress.getHostAddress()));
            return inetAddress;
         } catch (UnknownHostException e) {
            ExecutionTracer.addHostnameInfo(new HostnameResolutionInfo(host, null));
            throw e;
        }
    }

    @Replacement(
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.NET,
            replacingStatic = true,
            usageFilter = UsageFilter.ANY
    )
    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        if (ExternalServiceInfoUtils.skipHostnameOrIp(host) || ExecutionTracer.skipHostname(host))
            return InetAddress.getAllByName(host);

        try {
            if (ExecutionTracer.hasLocalAddressForHost(host)) {
                String ip = ExecutionTracer.getLocalAddress(host);
                return new InetAddress[]{InetAddress.getByName(ip)};
            }
            InetAddress[] inetAddresses = InetAddress.getAllByName(host);
            ExecutionTracer.addHostnameInfo(new HostnameResolutionInfo(host, inetAddresses[0].getHostAddress()));
            return inetAddresses;
        } catch (UnknownHostException e) {
            ExecutionTracer.addHostnameInfo(new HostnameResolutionInfo(host, null));
            throw e;
        }
    }
}
