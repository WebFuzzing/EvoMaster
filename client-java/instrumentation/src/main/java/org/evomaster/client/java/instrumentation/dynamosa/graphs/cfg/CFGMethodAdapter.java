/*
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evomaster.client.java.instrumentation.dynamosa.graphs.cfg;

import org.evomaster.client.java.instrumentation.AnnotatedMethodNode;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.Arrays;
import java.util.List;

/**
 * Create a minimized control flow graph for the method and store it. In
 * addition, this adapter also adds instrumentation for branch distance
 * measurement
 * <p>
 * defUse, concurrency and LCSAJs instrumentation is also added (if the
 * properties are set).
 *
 * @author Gordon Fraser
 */
public class CFGMethodAdapter extends MethodVisitor {

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
     * Constructor for CFGMethodAdapter.
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
    public CFGMethodAdapter(ClassLoader classLoader, String className, int access,
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
        BytecodeAnalyzer bytecodeAnalyzer = new BytecodeAnalyzer();
        SimpleLogger.info("Generating CFG for method " + methodName);
        try {
            bytecodeAnalyzer.analyze(classLoader, className, methodName, mn);
            SimpleLogger.debug("Method graph for "
                    + className
                    + "."
                    + methodName
                    + " contains "
                    + bytecodeAnalyzer.retrieveCFGGenerator().getRawGraph().vertexSet().size()
                    + " nodes for " + bytecodeAnalyzer.getFrames().length
                    + " instructions");
            // compute Raw and ActualCFG and put both into GraphPool
            bytecodeAnalyzer.retrieveCFGGenerator().registerCFGs();
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
