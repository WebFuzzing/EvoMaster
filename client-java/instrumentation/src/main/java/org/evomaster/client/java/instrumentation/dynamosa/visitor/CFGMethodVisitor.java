/*
 * Adapted from the EvoSuite project (https://github.com/EvoSuite/evosuite)
 * and modified for use in EvoMaster's Dynamosa module.
 */
package org.evomaster.client.java.instrumentation.dynamosa.visitor;

import org.evomaster.client.java.instrumentation.dynamosa.AnnotatedMethodNode;
import org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg.CFGGenerator;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.Arrays;
import java.util.List;

/**
 * Create a minimized control flow graph for the method and store it. In
 * addition, this visitor also adds instrumentation for branch distance
 * measurement
 * <p>
 * defUse, concurrency and LCSAJs instrumentation is also added (if the
 * properties are set).
 *
 */
public class CFGMethodVisitor extends MethodVisitor {

    /**
     * Methods to skip during CFG analysis.
     */
    public static final List<String> EXCLUDE = Arrays.asList("<clinit>()V");

    /**
     * This is the name + the description of the method. It is more like the
     * signature and less like the name. The name of the method can be found in
     * this.plain_name
     */
    private final String methodName;

    private final MethodVisitor next;
    private final int access;
    private final String className;
    private final ClassLoader classLoader;

    // no line tracking needed here

    /**
     * <p>
     * Constructor for CFGMethodVisitor.
     * </p>
     *
     * @param className  a {@link java.lang.String} object.
     * @param access     a int.
     * @param name       a {@link java.lang.String} object.
     * @param desc       a {@link java.lang.String} object.
     * @param signature  a {@link java.lang.String} object.
     * @param exceptions an array of {@link java.lang.String} objects.
     * @param mv         a {@link org.objectweb.asm.MethodVisitor} object.
     */
    public CFGMethodVisitor(ClassLoader classLoader, String className, int access,
                            String name, String desc, String signature, String[] exceptions,
                            MethodVisitor mv) {

        // super(new MethodNode(access, name, desc, signature, exceptions),
        // className,
        // name.replace('/', '.'), null, desc);

        super(Opcodes.ASM9, new AnnotatedMethodNode(access, name, desc, signature,
                exceptions));

        this.next = mv;
        this.className = className; // .replace('/', '.');
        this.access = access;
        this.methodName = name + desc;
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd() {
        SimpleLogger.debug("Creating CFG of " + className + "." + methodName);
        MethodNode mn = (AnnotatedMethodNode) mv;
        // skip excluded, abstract or native methods
        if (EXCLUDE.contains(methodName)
                || (access & Opcodes.ACC_ABSTRACT) != 0
                || (access & Opcodes.ACC_NATIVE) != 0) {
            mn.accept(next);
            return;
        }
        SimpleLogger.info("Analyzing method " + methodName + " in class " + className);
        CFGGenerator cfgGenerator = new CFGGenerator(classLoader, className, methodName, mn);
        SimpleLogger.info("Generating CFG for method " + methodName);
        try {
            Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(new SourceInterpreter()) {
                @Override
                protected void newControlFlowEdge(int src, int dst) {
                    cfgGenerator.registerControlFlowEdge(src, dst, getFrames(), false);
                }

                @Override
                protected boolean newControlFlowExceptionEdge(int src, int dst) {
                    cfgGenerator.registerControlFlowEdge(src, dst, getFrames(), true);
                    return true;
                }
            };
            analyzer.analyze(className, mn);
            SimpleLogger.debug("Method graph for "
                    + className
                    + "."
                    + methodName
                    + " contains "
                    + cfgGenerator.getRawGraph().vertexSet().size()
                    + " nodes for " + analyzer.getFrames().length
                    + " instructions");
            // compute Raw and ActualCFG and put both into GraphPool
            cfgGenerator.registerCFGs();
            SimpleLogger.info("Created CFG for method " + methodName);
        } catch (AnalyzerException e) {
            SimpleLogger.error("Analyzer exception while analyzing " + className + "."
                    + methodName + ": " + e);
            e.printStackTrace();
        }
        mn.accept(next);
    }

    // removed auxiliary methods related to method listings and filters
}
