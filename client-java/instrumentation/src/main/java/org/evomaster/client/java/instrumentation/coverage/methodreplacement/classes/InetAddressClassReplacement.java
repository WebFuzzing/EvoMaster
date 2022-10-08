package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

import java.net.*;

public class InetAddressClassReplacement implements MethodReplacementClass {
    @Override
    public Class<?> getTargetClass() {
        return InetAddress.class;
    }

    @Replacement(
            type = ReplacementType.TRACKER,
            category = ReplacementCategory.NET,
            id = "InetAddress_getByName_Replacement",
            replacingStatic = true,
            usageFilter = UsageFilter.ANY
    )
    public static InetAddress getByName(String host) throws UnknownHostException {
        if (host.startsWith("localhost") || host.startsWith("127.0.0") || host.startsWith("0.0.0"))
            return InetAddress.getByName(host);
        // FIXME -1 leads a crash, but do we really need the real port info here. might use specified one
        ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo("TCP", host, 80);
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
            id = "InetAddress_getAllByName_Replacement",
            replacingStatic = true,
            usageFilter = UsageFilter.ANY
    )
    public static InetAddress[] getAllByName(String host) throws UnknownHostException {
        if (host.startsWith("localhost") || host.startsWith("127.0.0") || host.startsWith("0.0.0"))
            return InetAddress.getAllByName(host);
        // FIXME -1 leads a crash, but do we really need the real port info here. might use specified one
        ExternalServiceInfo remoteHostInfo = new ExternalServiceInfo("TCP", host, 80);
        try{
            if (ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())){
                String ip = ExecutionTracer.getExternalMapping(remoteHostInfo.signature());
                return new InetAddress[]{InetAddress.getByName(ip)};
            }
            return InetAddress.getAllByName(host);
        }catch (UnknownHostException e){
            ExecutionTracer.addExternalServiceHost(remoteHostInfo);
            throw e;
        }
    }
}
