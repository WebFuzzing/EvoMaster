package org.evomaster.clientJava.instrumentation;

import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

/**
 * Entry point for the JavaAgent that will do the bytecode instrumentation
 */
public class InstrumentingAgent {

    /**
     * WARN: static variable with dynamic state.
     * Forced to use it due to very special nature of how
     * JavaAgents are handled
     */
    private static Instrumentator instrumentator;

    private static boolean active = false;


    /**
     * Actual method that is going to be called when the JavaAgent is started
     * @param agentArgs in this case, the {@code packagePrefixesToCover}
     * @param inst
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        instrumentator = new Instrumentator(agentArgs);
        inst.addTransformer(new TransformerForTests());
        active = true;
    }

    public static boolean isActive(){
        return active;
    }


    public static void changePackagesToInstrument(String packagePrefixesToCover){
        instrumentator = new Instrumentator(packagePrefixesToCover);
    }


    private static class TransformerForTests implements ClassFileTransformer{

        @Override
        public byte[] transform(ClassLoader loader, String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {

            if (!ClassesToExclude.checkIfCanInstrument(ClassName.get(className))) {
                return classfileBuffer;
            }


            ClassReader reader = new ClassReader(classfileBuffer);

            return instrumentator.transformBytes(loader, ClassName.get(className), reader);
        }
    }
}
