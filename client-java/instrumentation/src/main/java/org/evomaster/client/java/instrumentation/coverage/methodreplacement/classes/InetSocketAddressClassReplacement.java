package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.MethodReplacementClass;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.Replacement;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.UsageFilter;
import org.evomaster.client.java.instrumentation.shared.ReplacementCategory;
import org.evomaster.client.java.instrumentation.shared.ReplacementType;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class InetSocketAddressClassReplacement implements MethodReplacementClass {

    private static final ThreadLocal<Object> instance = new ThreadLocal<>();

    @Override
    public Class<?> getTargetClass() {
        return InetSocketAddress.class;
    }

    public static Object consumeInstance(){

        Object client = instance.get();
        if(client == null){
            throw new IllegalStateException("No instance to consume");
        }
        instance.remove();
        return client;
    }

    private static void addInstance(Object x){
        Object client = instance.get();
        if(client != null){
            throw new IllegalStateException("Previous instance was not consumed");
        }
        instance.set(x);
    }

    @Replacement(replacingStatic = false,
            type = ReplacementType.TRACKER,
            id = "InetSocketAddress_constructor",
            usageFilter = UsageFilter.ANY,
            category = ReplacementCategory.NET,
            replacingConstructor = true,
            castTo = "java.net.InetSocketAddress"
    )
    public static void InetSocketAddress(String hostname, int port){

        try {
            InetAddress address = InetAddressClassReplacement.getByName(hostname);
            addInstance(new InetSocketAddress(address,port));
        } catch (UnknownHostException e) {
            addInstance(new InetSocketAddress(hostname, port));
        }
    }
}
