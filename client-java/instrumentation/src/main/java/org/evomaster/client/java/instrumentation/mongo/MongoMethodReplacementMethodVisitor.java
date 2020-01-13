package org.evomaster.client.java.instrumentation.mongo;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;
import org.evomaster.client.java.instrumentation.Constants;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MongoMethodReplacementMethodVisitor extends MethodVisitor {

    private final String className;
    private final String methodName;
    private final String descriptor;

    public MongoMethodReplacementMethodVisitor(MethodVisitor mv,
                                               String className,
                                               String methodName,
                                               String descriptor) {
        super(Constants.ASM, mv);

        this.className = className;
        this.methodName = methodName;
        this.descriptor = descriptor;

        fillMethodsToReplace();
    }


    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String desc, boolean itf) {

        Method m = findMethodToReplace(owner, name, desc);
        if (m == null) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        } else {
            replaceMethod(m);
        }
    }


    /**
     * A mapping owner->name->desc->method to replace existing methods
     */
    private final Map<String, Map<String, Map<String, Method>>> methodsToReplace = new HashMap<String, Map<String, Map<String, Method>>>();

    /**
     * Adds a method from method replacement such that matches the expected signature for
     * a replacement method
     *
     * @param owner  the owner of the invoke
     * @param name   the name of the method being called
     * @param desc   the signature of the method being called
     * @param method the new method that will replace the original one
     */
    private void addMethodToReplace(String owner, String name, String desc, Method method) {
        if (!methodsToReplace.containsKey(owner)) {
            methodsToReplace.put(owner, new HashMap<String, Map<String, Method>>());
        }
        if (!methodsToReplace.get(owner).containsKey(name)) {
            methodsToReplace.get(owner).put(name, new HashMap<String, Method>());
        }
        methodsToReplace.get(owner).get(name).put(desc, method);
    }


    private void fillMethodsToReplace() {

        try {
            addMethodToReplace("com/mongodb/client/internal/MongoCollectionImpl",
                    "find",
                    "(Lorg/bson/conversions/Bson;)Lcom/mongodb/client/FindIterable;",
                    MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, Bson.class));

            addMethodToReplace("com/mongodb/client/internal/MongoCollectionImpl",
                    "find",
                    "(Lorg/bson/conversions/Bson;Ljava/lang/Class;)Lcom/mongodb/client/FindIterable;",
                    MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, Bson.class, Class.class));

            addMethodToReplace("com/mongodb/client/internal/MongoCollectionImpl",
                    "find",
                    "(Lcom/mongodb/client/ClientSession;Lorg/bson/conversions/Bson;Ljava/lang/Class;)Lcom/mongodb/client/FindIterable;",
                    MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, ClientSession.class, Bson.class, Class.class));

            addMethodToReplace("com/mongodb/client/MongoCollection",
                    "find",
                    "(Lorg/bson/conversions/Bson;)Lcom/mongodb/client/FindIterable;",
                    MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, Bson.class));

            addMethodToReplace("com/mongodb/client/MongoCollection",
                    "find",
                    "(Lorg/bson/conversions/Bson;Ljava/lang/Class;)Lcom/mongodb/client/FindIterable;",
                    MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, Bson.class, Class.class));

            addMethodToReplace("com/mongodb/client/MongoCollection",
                    "find",
                    "(Lcom/mongodb/client/ClientSession;Lorg/bson/conversions/Bson;Ljava/lang/Class;)Lcom/mongodb/client/FindIterable;",
                    MongoCollectionClassReplacement.class.getMethod("find", MongoCollection.class, ClientSession.class, Bson.class, Class.class));

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("A replacement method for MongoDB was not found", e);
        }
    }


    private boolean containsMethodToReplace(String owner, String name, String desc) {
        return methodsToReplace.containsKey(owner) &&
                methodsToReplace.get(owner).containsKey(name) &&
                methodsToReplace.get(owner).get(name).containsKey(desc);
    }

    private Method getMethodToReplace(String owner, String name, String desc) {
        return methodsToReplace.get(owner).get(name).get(desc);
    }

    private Method findMethodToReplace(String owner, String name, String desc) {
        if (containsMethodToReplace(owner, name, desc)) {
            return getMethodToReplace(owner, name, desc);
        } else {
            return null;
        }
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