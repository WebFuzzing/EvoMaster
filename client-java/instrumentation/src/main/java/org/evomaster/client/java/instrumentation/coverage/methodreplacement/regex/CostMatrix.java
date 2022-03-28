package org.evomaster.client.java.instrumentation.coverage.methodreplacement.regex;

/**
 * Class used to calculate the cost, ie the actual distance, based on a RegexGraph.
 *
 */
public class CostMatrix {

    private static final int DEL = 0;
    private static final int REP = 1;
    private static final int INS = 2;


    public static int calculateStandardCost(RegexGraph graph) {
        final int ROWS = graph.getNumberOfRows();
        final int COLUMNS = graph.getNumberOfColumns();

        final double[][] matrix = new double[ROWS][COLUMNS];

        // First row is cost of matching empty sequence on regex
        final int FIRST_ROW = 0;

        /*
         * init first starting state with 0 costs
         */
        matrix[FIRST_ROW][0] = 0;

        //look at first row (which is special)
        for (int col = 1; col < graph.getNumberOfColumns(); col++) {

            double min = Double.MAX_VALUE;

            for (GraphTransition t : graph.getIncomingTransitions(FIRST_ROW, col)) {

                int otherCol = graph.getColumn(t.fromState);

                //self transition
                if (col == otherCol) {
                    continue;
                }

                double otherCost = matrix[FIRST_ROW][otherCol];

                min = Math.min(min, getSubPathCost(otherCost, Math.ceil(t.cost)));
            }

            matrix[FIRST_ROW][col] = min;
        }

        //then look at the other rows
        for (int i = 1; i < ROWS; i++) {

            for (int col = 0; col < COLUMNS; col++) {

                matrix[i][col] = Double.MAX_VALUE;

                for (GraphTransition t : graph.getIncomingTransitions(i, col)) {

                    int otherCol = graph.getColumn(t.fromState);
                    int otherRow = t.fromRow;

                    if (!t.type.equals(GraphTransition.TransitionType.PHANTOM)) {
                        matrix[i][col] = Math.min(matrix[i][col], getSubPathCost(matrix[otherRow][otherCol], Math.ceil(t.cost)));
                    } else {
                        /*
                         * artificial transition to final/sink state, so just take same values as previous state
                         */
                        matrix[i][col] = Math.min(matrix[i][col], matrix[otherRow][otherCol]);

                    }
                }
            }
        }

        double min = matrix[ROWS - 1][COLUMNS - 1];
        return (int) Math.round(min);
    }

    /**
     * Note: this is different from normal matching algorithms, as we enforce an order
     * among the operators: delete, replace and then insert.
     * @param graph
     * @return
     */
    public static double calculateCostForStringAVM(RegexGraph graph) {

        final int ROWS = graph.getNumberOfRows();
        final int COLUMNS = graph.getNumberOfColumns();

        /*
         * we create a matrix based on each row and each column in the graph.
         * Each cell has 3 values, each representing the cost of thre different types of path:
         *
         * 0) only deletion
         * 1) deletions followed by replacement
         * 2) as above, and then followed by insertions
         */
        final double[][][] matrix = new double[ROWS][COLUMNS][3];

        calculateInsertionCostOnFirstRow(graph, matrix);

        for (int i = 1; i < ROWS; i++) {

            for (int col = 0; col < COLUMNS; col++) {

                /*
                 * unless a path is explicitly updated, it will have maximum distance by default
                 */
                matrix[i][col][DEL] = Double.MAX_VALUE;
                matrix[i][col][REP] = Double.MAX_VALUE;
                matrix[i][col][INS] = Double.MAX_VALUE;

                for (GraphTransition t : graph.getIncomingTransitions(i, col)) {

                    int otherCol = graph.getColumn(t.fromState);
                    int otherRow = t.fromRow;

                    if (t.type.equals(GraphTransition.TransitionType.INSERTION)) {
                        assert otherRow == i;
                        /*
                         * if we have an insertion, only the insertion path can be continued.
                         * that's the reason why on the left side we only update for [INS].
                         * An insertion can continue any type of path (and so all types are present on the right side).
                         */
                        matrix[i][col][INS] = Math.min(matrix[i][col][INS], getSubPathCost(matrix[otherRow][otherCol][DEL], t.cost));
                        matrix[i][col][INS] = Math.min(matrix[i][col][INS], getSubPathCost(matrix[otherRow][otherCol][REP], t.cost));
                        matrix[i][col][INS] = Math.min(matrix[i][col][INS], getSubPathCost(matrix[otherRow][otherCol][INS], t.cost));
                    } else if (t.type.equals(GraphTransition.TransitionType.REPLACEMENT)) {
                        /*
                         * if we have a replacement, then we cannot continue a delete path.
                         * So, no [DEL] on the left side.
                         * A replacement can continue a delete or replace path, but not an insertion one (and so [DEL] and
                         * [REP] on right side)
                         */
                        matrix[i][col][REP] = Math.min(matrix[i][col][REP], getSubPathCost(matrix[otherRow][otherCol][DEL], t.cost));
                        matrix[i][col][REP] = Math.min(matrix[i][col][REP], getSubPathCost(matrix[otherRow][otherCol][REP], t.cost));
                        /*
                         * from this state on, an insertion path can be followed, with same cost (ie right side) as replacement path
                         */
                        matrix[i][col][INS] = Math.min(matrix[i][col][INS], getSubPathCost(matrix[otherRow][otherCol][DEL], t.cost));
                        matrix[i][col][INS] = Math.min(matrix[i][col][INS], getSubPathCost(matrix[otherRow][otherCol][REP], t.cost));
                    } else if (t.type.equals(GraphTransition.TransitionType.DELETION)) {
                        /*
                         * deletion can only follow a deletion path (so only [DEL] or right side).
                         * but, from this state on, any new path can be followed (so all on left side)
                         */
                        matrix[i][col][DEL] = Math.min(matrix[i][col][DEL], getSubPathCost(matrix[otherRow][otherCol][DEL], t.cost));
                        matrix[i][col][REP] = Math.min(matrix[i][col][REP], getSubPathCost(matrix[otherRow][otherCol][DEL], t.cost));
                        matrix[i][col][INS] = Math.min(matrix[i][col][INS], getSubPathCost(matrix[otherRow][otherCol][DEL], t.cost));
                    } else if (t.type.equals(GraphTransition.TransitionType.PHANTOM)) {
                        assert t.cost == 0;
                        /*
                         * artificial transition to final/sink state, so just take same values as previous state
                         */
                        matrix[i][col][DEL] = Math.min(matrix[i][col][DEL], matrix[otherRow][otherCol][DEL]);
                        matrix[i][col][REP] = Math.min(matrix[i][col][REP], matrix[otherRow][otherCol][REP]);
                        matrix[i][col][INS] = Math.min(matrix[i][col][INS], matrix[otherRow][otherCol][INS]);
                    }
                }
            }

            /*
             * TODO: The algorithm of Myers's paper, at page 12, makes a distinction between D and E transitions.
             * Insertions of type E are done last. Not fully clear if it has an effect here: ie, recall that
             * here we do minimization (calculate distance) and not maximization (similarity)
             */
        }

        /*
         * get the minimum among the 3 different paths in the sink state
         */
        double min = Double.MAX_VALUE;
        for (double value : matrix[ROWS - 1][COLUMNS - 1]) {
            if (value < min) {
                min = value;
            }
        }

        return min;
    }

    /**
     * We cannot just do previousStateCost + transitionCost, as there might be computational overflows
     *
     * @param previousStateCost
     * @param transitionCost
     * @return
     * @throws IllegalArgumentException
     */
    private static double getSubPathCost(double previousStateCost, double transitionCost) throws IllegalArgumentException {
        if (previousStateCost < 0) {
            throw new IllegalArgumentException("previousStateCost cannot be negative: " + previousStateCost);
        }
        if (transitionCost < 0) {
            throw new IllegalArgumentException("transitionCost cannot be negative: " + transitionCost);
        }

        if (previousStateCost == Double.MAX_VALUE || transitionCost == Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        }

        double sum = previousStateCost + transitionCost;

        if (sum < previousStateCost || sum < transitionCost) {
            /*
             * likely overflow
             */
            return Double.MAX_VALUE;
        }

        return sum;
    }

    /**
     * First row is special, ie very different from the others
     *
     * @param graph
     * @param matrix
     */
    private static void calculateInsertionCostOnFirstRow(RegexGraph graph, final double[][][] matrix) {

        // First row is cost of matching empty sequence on regex
        final int FIRST_ROW = 0;

        /*
         * init first starting state with 0 costs
         */
        matrix[FIRST_ROW][0][0] = 0;
        matrix[FIRST_ROW][0][1] = 0;
        matrix[FIRST_ROW][0][2] = 0;

        for (int col = 1; col < graph.getNumberOfColumns(); col++) {

            double min = Double.MAX_VALUE;

            for (GraphTransition t : graph.getIncomingTransitions(FIRST_ROW, col)) {

                /*
                 * on first row, there can be only insertions coming from the same row,
                 * apart from last node that can have a phantom transition to sink state
                 */
                assert t.type.equals(GraphTransition.TransitionType.INSERTION) ||
                        t.type.equals(GraphTransition.TransitionType.PHANTOM);
                assert t.fromRow == 0;

                int otherCol = graph.getColumn(t.fromState);

                //self transition
                if (col == otherCol) {
                    continue;
                }

                double otherCost = matrix[FIRST_ROW][otherCol][2];

                min = Math.min(min, getSubPathCost(otherCost, t.cost));
            }

            /*
             * as there can be only insertions, the delete and replace paths cannot be followed, and
             * so maximum distance
             */
            matrix[FIRST_ROW][col][0] = Double.MAX_VALUE;
            matrix[FIRST_ROW][col][1] = Double.MAX_VALUE;
            matrix[FIRST_ROW][col][2] = min;
        }
    }
}
