package org.evomaster.client.java.instrumentation;



import org.evomaster.client.java.instrumentation.coverage.CoverageClassVisitor;
import org.evomaster.client.java.instrumentation.coverage.ThirdPartyClassVisitor;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.client.java.instrumentation.tracker.TrackerClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Instrumentator {

    private final List<String> prefixes;

    public Instrumentator(String packagePrefixesToCover) {
        Objects.requireNonNull(packagePrefixesToCover);

        prefixes = Arrays.asList(
                packagePrefixesToCover.split(","))
                .stream()
                .map(s -> s.trim())
                .filter(s -> ! s.isEmpty())
                .collect(Collectors.toList());

        if (prefixes.isEmpty()) {
            throw new IllegalArgumentException("You have to specify at least one non-empty prefix, e.g. 'com.yourapplication'");
        }


    }

    /**
     * Get the raw bytes of instrumented class with name {@code className}
     *
     * @param classLoader
     * @param className
     * @param reader
     * @return
     */
    public byte[] transformBytes(ClassLoader classLoader, ClassName className, ClassReader reader) {
        Objects.requireNonNull(classLoader);
        Objects.requireNonNull(className);
        Objects.requireNonNull(reader);

        if (!ClassesToExclude.checkIfCanInstrument(className)) {
            throw new IllegalArgumentException("Cannot instrument " + className);
        }

        int asmFlags = ClassWriter.COMPUTE_FRAMES;
        ClassWriter writer = new ComputeClassWriter(asmFlags);
        ClassVisitor cv = writer;

        //avoid reading frames, as we re-compute them
        int readFlags = ClassReader.SKIP_FRAMES;

        ClassNode cn = new ClassNode();
        reader.accept(cn, readFlags);

        if(canInstrumentForCoverage(className)){

            cv = new CoverageClassVisitor(cv, className);

            /*
                this should be done after coverage instrumentation, as
                we don't want these extra methods added as part of
                targets to cover
             */
            cv = new TrackerClassVisitor(cv, className);

        } else {

            cv = new ThirdPartyClassVisitor(cv, className);

            //reader.accept(cv, readFlags);
        }

        cn.accept(cv);

        return writer.toByteArray();
    }


    private boolean canInstrumentForCoverage(ClassName className){

        return prefixes.stream()
                .anyMatch(s -> className.getFullNameWithDots().startsWith(s));
    }
}