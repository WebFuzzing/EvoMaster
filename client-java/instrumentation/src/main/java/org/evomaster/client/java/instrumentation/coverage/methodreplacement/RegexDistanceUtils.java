
/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 * <p>
 * This file is part of EvoSuite.
 * <p>
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 * <p>
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */

/*
    Note: some of the code here as been refactored into the ".regex" module
 */

package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.regex.CostMatrix;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.regex.RegexGraph;
import org.evomaster.client.java.instrumentation.coverage.methodreplacement.regex.RegexUtils;
import org.evomaster.client.java.utils.SimpleLogger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

/**
 *  Class used to define the distance between a string and a regex
 */
public class RegexDistanceUtils {

    private static final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    private static final Set<String> notSupported = new CopyOnWriteArraySet<>();

    /**
     * This is tricky... we could reasonably assume that in most cases the number of regex in the SUT
     * is finite, but that is not the case for the input strings, as those are parts of the generated
     * inputs by EM.
     * However, this code is really expensive... so any saving would be worthy.
     *
     * TODO in the future we could put a max number of regex evaluations per test, eg 50, if this
     * workaround is still not enough
     */
    private static final Map<String, Map<String, RegexGraph>> graphCache = new ConcurrentHashMap<>();

    /**
     * <p>
     * Get the distance between the arg and the given regex.
     * All operations (insertion/deletion/replacement) cost 1.
     * There is no assumption on where and how the operations
     * can be done (ie all sequences are valid).
     * </p>
     */
    public static int getStandardDistance(String arg, String regex) {
        if (!RegexUtils.isSupportedRegex(regex) || notSupported.contains(regex)) {
            return getDefaultDistance(arg, regex);
        }
        RegexGraph graph;

        //TODO

        try {
            graph = new RegexGraph(arg, regex);
        }catch (Exception e){
            SimpleLogger.uniqueWarn("Failed to build graph for regex: " + regex);
            notSupported.add(regex);
            return getDefaultDistance(arg, regex);
        }

        try {
            return CostMatrix.calculateStandardCost(graph);
        }catch (Exception e){
            SimpleLogger.uniqueWarn("Failed to compute distance cost for regex: " + regex);
            notSupported.add(regex);
            return getDefaultDistance(arg, regex);
        }
    }

    private static int getDefaultDistance(String arg, String regex) {
        Pattern p = patternCache.get(regex);
        if(p == null) {
            p = Pattern.compile(regex);
            patternCache.put(regex, p);
        }

        if (p.matcher(arg).matches())
            return 0;
        else
            return 1;
    }
}

