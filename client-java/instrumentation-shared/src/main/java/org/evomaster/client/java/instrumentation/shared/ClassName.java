package org.evomaster.client.java.instrumentation.shared;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ClassName {

    /**
     * Name used in the bytecode, where "/" are used
     * instead of "."
     * eg
     * org.bar.Foo turns into org/bar/Foo
     */
    private final String bytecodeName;

    /**
     * What usually returned with Foo.class.getName(),
     * eg
     * org.bar.Foo
     */
    private final String fullNameWithDots;


    private static final Map<Class<?>,ClassName> cacheClass = new ConcurrentHashMap<>(10_000);
    private static final Map<String,ClassName> cacheString = new ConcurrentHashMap<>(10_000);

    public static ClassName get(Class<?> klass){
        return  cacheClass.computeIfAbsent(klass, k -> new ClassName(k));
        //return new ClassName(klass);
    }

    public static ClassName get(String name) {
        return cacheString.computeIfAbsent(name, n -> new ClassName(n));
        //return new ClassName(name);
    }

    public ClassName(Class<?> klass){
        this(Objects.requireNonNull(klass).getName());
    }

    /**
     *
     * @param name of the class, or path resource
     */
    public ClassName(String name){
        Objects.requireNonNull(name);

        if(name.endsWith(".class")){
            name = name.substring(0, name.length() - ".class".length());
        }
        if(name.endsWith(".java")){
            name = name.substring(0, name.length() - ".java".length());
        }

        if(name.contains("/") && name.contains(".")){
            throw new IllegalArgumentException("Do not know how to handle name: " +name);
        }

        if(name.contains("/")){
            bytecodeName = name;
            fullNameWithDots = name.replace("/", ".");
        } else {
            bytecodeName = name.replace(".", "/");
            fullNameWithDots = name;
        }
    }

    /**
        Name of the class as used in the bytecode instructions.
        This means that foo.bar.Hello would be foo/bar/Hello
     */
    public String getBytecodeName() {
        return bytecodeName;
    }

    /**
     * Eg, foo.bar.Hello
     * @return
     */
    public String getFullNameWithDots() {
        return fullNameWithDots;
    }

    public String getAsResourcePath(){
        return bytecodeName + ".class";
    }

    @Override
    public String toString(){
        return "[" + this.getClass().getSimpleName()+": "+fullNameWithDots+"]";
    }
}
