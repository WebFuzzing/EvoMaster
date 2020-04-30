package org.evomaster.client.java.instrumentation.tracker;

import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.Constants;
import org.evomaster.client.java.instrumentation.staticstate.UnitsInfoRecorder;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

@Deprecated
public class TrackerMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;
    private final String descriptor;

    public TrackerMethodVisitor(MethodVisitor mv,
                                String className,
                                String methodName,
                                String descriptor) {
        super(Constants.ASM, mv);

        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc, boolean itf) {

        addTrackerIfNeeded(owner, name, desc);

        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }


    @Override
    public void visitMaxs(int maxStack, int maxLocals) {

        /*
            When we want to check how a certain method is called,
            we need to copy the values that are on the stack.

            However, if the method takes at most 1 input, we could
            just return it in our support methods, and so avoiding
            duplicating the value on the stack.
         */
        int maxCopiedValues = 0;

        super.visitMaxs(maxStack + maxCopiedValues, maxLocals);
    }

    private void addTrackerIfNeeded(String owner, String name, String desc) {

        if (owner.equals("org/springframework/web/context/request/WebRequest")) {

            if ((name.equals("getParameter") && desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                    ||
                    (name.equals("getParameterValues") && desc.equals("(Ljava/lang/String;)[Ljava/lang/String;"))) {

                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ClassName.get(Tracker.class).getBytecodeName(),
                        Tracker.TRACK_QUERY_PARAMETER_METHOD_NAME,
                        Tracker.TRACK_QUERY_PARAMETER_DESCRIPTOR,
                        Tracker.class.isInterface()); //false

                UnitsInfoRecorder.markNewTrackedMethod();

            } else if((name.equals("getHeader") && desc.equals("(Ljava/lang/String;)Ljava/lang/String;"))
                    ||
                    (name.equals("getHeaderValues") && desc.equals("(Ljava/lang/String;)[Ljava/lang/String;"))){

                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ClassName.get(Tracker.class).getBytecodeName(),
                        Tracker.TRACK_HEADER_METHOD_NAME,
                        Tracker.TRACK_HEADER_DESCRIPTOR,
                        Tracker.class.isInterface()); //false

                UnitsInfoRecorder.markNewTrackedMethod();
            }
        } else if(owner.equals("javax/servlet/http/ServletRequest")){

            if(name.equals("getInputStream") && desc.equals("()V")){

                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        ClassName.get(Tracker.class).getBytecodeName(),
                        Tracker.TRACK_INPUT_STREAM_METHOD_NAME,
                        Tracker.TRACK_INPUT_STREAM_DESCRIPTOR,
                        Tracker.class.isInterface()); //false

                UnitsInfoRecorder.markNewTrackedMethod();
            }
        }
    }
}