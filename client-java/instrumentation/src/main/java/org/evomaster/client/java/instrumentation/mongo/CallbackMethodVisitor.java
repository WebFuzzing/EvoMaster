package org.evomaster.client.java.instrumentation.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.Constants;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

public class CallbackMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;
    private final String descriptor;

    public CallbackMethodVisitor(MethodVisitor mv,
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

        Method m = findMethodToReplace(owner,name,desc);
        if (m==null) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        } else {
            replaceMethod(m);
        }
    }

    private Method findMethodToReplace(String owner, String name, String desc) {
        if (owner.equals("com/mongodb/client/internal/MongoCollectionImpl")
        || (owner.equals("com/mongodb/client/MongoCollection"))) {
            if ((name.equals("find"))) {
                switch (desc){
                    case "(Lorg/bson/conversions/Bson;)Lcom/mongodb/client/FindIterable;":
                        try {
                            return MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, Bson.class);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException("Expecting method but it was not found");
                        }
                    case "(Lorg/bson/conversions/Bson;Ljava/lang/Class;)Lcom/mongodb/client/FindIterable;": {
                        try {
                            return MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, Bson.class,Class.class);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException("Expecting method but it was not found");
                        }
                    }
                    case "(Lcom/mongodb/client/ClientSession;Lorg/bson/conversions/Bson;Ljava/lang/Class;)Lcom/mongodb/client/FindIterable;": {
                        try {
                            return MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, ClientSession.class, Bson.class,Class.class);
                        } catch (NoSuchMethodException e) {
                            throw new RuntimeException("Expecting method but it was not found");
                        }

                    }
                    default: {
                        throw new RuntimeException("Must implement find " + desc);
                    }
                }
            }
        }
        return null;
    }

    private void replaceMethod(Method m) {

        /*
                    In the case of replacing a non-static method a.foo(x,y),
                    we will need a replacement bar(a,x,y,id)

                    So, the stack
                    a
                    x
                    y
                    foo # non-static

                    will be replaced by
                    a
                    x
                    y
                    bar # static

                    This means we do not need to handle "a", but still need to replace "foo" with "bar".
        */

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(m.getDeclaringClass()),
                m.getName(),
                Type.getMethodDescriptor(m),
                false);
    }

}