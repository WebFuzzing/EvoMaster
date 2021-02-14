package org.evomaster.client.java.instrumentation.coverage.methodreplacement.regex;


import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A graph created based on an "arg" that is matched against a "regex".
 * There is going to be arg.length+1 copies of the regex automaton.
 * Each copy represents a "row".
 * Each automaton state, in topological order, represents a "column".
 * The graph can be considered as a "rows"x"columns" matrix.
 *
 * @author arcuri
 *
 */
public class RegexGraph {


    /*
     * Automatons for regex can be expensive to build. So we cache them,
     * as we might need to access to them several times during the search
     */
    private static final Map<String, List<State>> regexStateCache = new ConcurrentHashMap<>();
    private static final Map<String, Automaton> regexAutomatonCache = new ConcurrentHashMap<>();

    private final Map<Integer, Map<State, Set<GraphTransition>>> transitions;
    private Map<Integer, State> intToStateMap;
    private Map<State, Integer> stateToIntMap;

    /**
     * Build the graph
     * @param arg
     * @param regex
     */
    public RegexGraph(String arg, String regex) {
        transitions = createGraph(arg, regex);
    }

    public int getNumberOfRows() {
        return transitions.keySet().size();
    }

    public int getNumberOfColumns() {
        return stateToIntMap.size();
    }

    /**
     * Get all the incoming transitions to the node located at coordinate "row" and "column"
     */
    public Set<GraphTransition> getIncomingTransitions(int row, int column) {
        State state = intToStateMap.get(column);
        return transitions.get(row).get(state);
    }

    public int getColumn(State state) {
        return stateToIntMap.get(state);
    }

    /**
     * Normalize x in [0,1]
     */
    private static double normalize(double x) {
        return x / (x + 1.0);
    }

    private static Automaton getAndCacheAutomaton(String regex) {
        /*
         * Cache it if first time we build it
         */
        if (!regexAutomatonCache.containsKey(regex)) {
            /*
             * Create an automaton representing the regex
             */
            cacheRegex(regex);
        }

        Automaton automaton = regexAutomatonCache.get(regex);
        return automaton;
    }

    private Map<Integer, Map<State, Set<GraphTransition>>> createGraph(String arg, String regex) {

        /*
         * Create a graph to calculate the distance. The algorithm is based on what discussed in:
         *
         * Mohammad Alshraideh and Leonardo Bottaci
         * Search-based software test data generation for string data using program-specific search operators
         * http://neo.lcc.uma.es/mase/attachments/085_TestDataGenerationForStringData.pdf
         *
         * and
         *
         * EUGENE W. MYERS and WEBB MILLER
         * APPROXIMATE MATCHING OF REGULAR EXPRESSIONS
         * http://www.cs.mun.ca/~harold/Courses/Old/Ling6800.W06/Diary/reg.aprox.pdf
         */

        Automaton automaton = getAndCacheAutomaton(regex);
        final int NUM_CHARS = arg.length();


        List<State> topologicalOrder = regexStateCache.get(regex);

        Map<Integer, Map<State, Set<GraphTransition>>> transitions = new HashMap<>();

        intToStateMap = new HashMap<>();
        stateToIntMap = new HashMap<>();
        int numState = 0;

        for (State currentState : topologicalOrder) {

            /*
             * Init data structure to quickly map/access state/index
             */
            stateToIntMap.put(currentState, numState);
            intToStateMap.put(numState, currentState);
            numState++;

            for (Transition t : currentState.getTransitions()) {

                State destination = t.getDest();
                ensureState(transitions, destination, NUM_CHARS);

                for (int row = 0; row <= NUM_CHARS; row++) {
                    /*
                     *  add an insertion edge from currentState in row to target state in same row
                     */

                    transitions.get(row).get(destination)
                            .add(new GraphTransition(1.0, row, currentState, GraphTransition.TransitionType.INSERTION));
                }

                for (int row = 0; row < NUM_CHARS; row++) {
                    /*
                     *  Add a replacement edge from currentState in row to t.getDest in row+1
                     *  if charAt row+1 == the parameter of this transition, this is a zero-cost edge
                     */

                    double cost = 0.0;

                    if (arg.charAt(row) < t.getMin() || arg.charAt(row) > t.getMax()) {
                        int distMin = Math.abs(arg.charAt(row) - t.getMin());
                        int distMax = Math.abs(arg.charAt(row) - t.getMax());
                        cost = normalize(Math.min(distMin, distMax));
                    }

                    /*
                     * Important: even if the cost is 0 (eg match on the arg/regex in which we replace char X with X), we CANNOT
                     * use a PHANTOM transition. Even if we do not replace anything, we still need to consider it as a replacement
                     * transition. Consider the case
                     *
                     *  "ac".matches("abc")
                     *
                     *  If we used a phantom transition to represent the alignment c/c, then it would be possible to insert 'b' in the
                     *  middle of "abc". On the other hand, if we use a replacement c/c, then inserting 'b' would not be allowed, as an
                     *  insertion cannot be followed by a replacement.
                     */

                    transitions.get(row + 1).get(destination)
                            .add(new GraphTransition(cost, row, currentState, GraphTransition.TransitionType.REPLACEMENT));
                }
            }

            ensureState(transitions, currentState, NUM_CHARS);

            for (int row = 0; row < NUM_CHARS; row++) {

                /*
                 * add a deletion edge with cost 1 from currentState to currentState in next row
                 */

                transitions.get(row + 1).get(currentState)
                        .add(new GraphTransition(1.0, row, currentState, GraphTransition.TransitionType.DELETION));
            }
        }

        // Add zero-cost transitions from accepting states to final state
        State finalState = new State();
        ensureState(transitions, finalState, NUM_CHARS);
        for (State s : automaton.getStates()) {
            if (s.isAccept()) {
                transitions.get(NUM_CHARS).get(finalState)
                        .add(new GraphTransition(0, NUM_CHARS, s, GraphTransition.TransitionType.PHANTOM));
            }
        }
        intToStateMap.put(numState, finalState);
        stateToIntMap.put(finalState, numState);

        return transitions;
    }

    /**
     * Ensure that each row has the full data structures containing the target state
     *
     */
    private static void ensureState(
            Map<Integer, Map<State, Set<GraphTransition>>> transitions,
            State state,
            int numRows) {

        for (int row = 0; row <= numRows; row++) {
            if (!transitions.containsKey(row)) {
                transitions.put(row, new HashMap<>());
            }
            if (!transitions.get(row).containsKey(state)) {
                transitions.get(row).put(state, new HashSet<>());
            }
        }
    }

    private static void cacheRegex(String regex) {
        String r = RegexUtils.expandRegex(regex);
        Automaton automaton = new RegExp(r, RegExp.NONE).toAutomaton();
        automaton.expandSingleton();

        // We convert this to a graph without self-loops in order to determine the topological order
        DirectedGraph<State, DefaultEdge> regexGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Set<State> visitedStates = new HashSet<>();
        Queue<State> states = new LinkedList<>();
        State initialState = automaton.getInitialState();
        states.add(initialState);

        while (!states.isEmpty()) {
            State currentState = states.poll();
            if (visitedStates.contains(currentState)) {
                continue;
            }
            if (!regexGraph.containsVertex(currentState)) {
                regexGraph.addVertex(currentState);
            }

            for (Transition t : currentState.getTransitions()) {
                // Need to get rid of back edges, otherwise there is no topological order!
                if (!t.getDest().equals(currentState)) {
                    regexGraph.addVertex(t.getDest());
                    regexGraph.addEdge(currentState, t.getDest());
                    states.add(t.getDest());
                    CycleDetector<State, DefaultEdge> det = new CycleDetector<>(regexGraph);
                    if (det.detectCycles()) {
                        regexGraph.removeEdge(currentState, t.getDest());
                    }
                }
            }
            visitedStates.add(currentState);
        }

        TopologicalOrderIterator<State, DefaultEdge> iterator = new TopologicalOrderIterator<>(regexGraph);
        List<State> topologicalOrder = new ArrayList<>();
        while (iterator.hasNext()) {
            topologicalOrder.add(iterator.next());
        }

        regexStateCache.put(regex, topologicalOrder);
        regexAutomatonCache.put(regex, automaton);
    }
}
