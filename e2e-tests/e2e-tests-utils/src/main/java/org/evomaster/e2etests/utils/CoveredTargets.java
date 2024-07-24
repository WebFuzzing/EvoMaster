package org.evomaster.e2etests.utils;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class CoveredTargets {

    private static final Set<String> coveredTargets = new CopyOnWriteArraySet<>();

    public static void reset(){
        coveredTargets.clear();
    }

    public static void cover(String target){
        coveredTargets.add(target);
    }

    public static boolean isCovered(String target){
        return coveredTargets.contains(target);
    }

    public static boolean areCovered(Collection<String> targets) {
        return coveredTargets.containsAll(targets);
    }

    public static int numberOfCoveredTargets(){
        return coveredTargets.size();
    }
}
