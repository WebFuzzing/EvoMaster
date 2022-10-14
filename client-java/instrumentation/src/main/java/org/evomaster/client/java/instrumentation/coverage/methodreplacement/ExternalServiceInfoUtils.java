package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.ExternalServiceInfo;
import org.evomaster.client.java.instrumentation.shared.ExternalServiceSharedUtils;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;

public class ExternalServiceInfoUtils {

    /**
     *
     * @param remoteHostInfo is the host info collected from the SUT
     * @param remotePort is the port employed by the SUT
     * @return redirected an array with two elements
     */
    public static String[] collectExternalServiceInfo(ExternalServiceInfo remoteHostInfo, int remotePort){

        ExecutionTracer.addExternalServiceHost(remoteHostInfo);

        String signature  = remoteHostInfo.signature();
        int connectPort = remotePort;
        if (!ExecutionTracer.hasExternalMapping(remoteHostInfo.signature())) {
            ExecutionTracer.addEmployedDefaultWMHost(remoteHostInfo);
            signature = ExternalServiceSharedUtils.getWMDefaultSignature(remoteHostInfo.getProtocol(), remotePort);
            connectPort = ExternalServiceSharedUtils.getDefaultWMPort(signature);
        }

        return new String[]{ExecutionTracer.getExternalMapping(signature), "" + connectPort};
    }
}
