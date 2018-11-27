package org.evomaster.client.java.instrumentation;

import org.objectweb.asm.Opcodes;

public class Constants {

    public static final int ASM = Opcodes.ASM7;

    public static final String CLASS_INIT_METHOD = "<clinit>";

    public static boolean isMethodSyntheticOrBridge(int methodAccess){
        return (methodAccess & Opcodes.ACC_SYNTHETIC) > 0
                || (methodAccess & Opcodes.ACC_BRIDGE) > 0 ;
    }
}
