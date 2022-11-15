package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
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
 *
 * in order to make it connected, we could do replacement for 1) collecting host info, and
 * 2) providing an ip address
 *
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
        if (ExternalServiceInfoUtils.skipHostnameOrIp(host))
            return InetAddress.getByName(host);
        // FIXME -1 leads a crash, but do we really need the real port info here. might use specified one
        // If the given hostname is available, this method works properly otherwise throws UnknownHostException
        // This will never create a WM since the port won't be updated, if Inet only used
        ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo("https", host, 10000);
         try{
             if (ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())){
                 String ip = ExecutionTracer.getExternalMapping(remoteHostInfo.signature());
                 return InetAddress.getByName(ip);
             }
             return InetAddress.getByName(host);
         }catch (UnknownHostException e){
             ExecutionTracer.addExternalServiceHost(remoteHostInfo);
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
        if (ExternalServiceInfoUtils.skipHostnameOrIp(host))
            return InetAddress.getAllByName(host);
        // FIXME -1 leads a crash, but do we really need the real port info here. might use specified one
        ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo("https", host, 10000);
        try{
            if (ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())){
                String ip = ExecutionTracer.getExternalMapping(remoteHostInfo.signature());
                return new InetAddress[]{InetAddress.getByName(ip)};
            }
            return InetAddress.getAllByName(host);
        }catch (UnknownHostException e){
            // TODO: Using Inet extract the host information and then add to the collection
            ExecutionTracer.addExternalServiceHost(remoteHostInfo);
            throw e;
        }
    }
}
