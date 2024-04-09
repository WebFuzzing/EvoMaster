import kotlin.Pair;
import org.evomaster.dbconstraint.*;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class Solver implements AutoCloseable {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Solver.class.getName());

    public static final String Z3_DOCKER_IMAGE = "ghcr.io/z3prover/z3:ubuntu-20.04-bare-z3-sha-ba8d8f0";

    // The default entrypoint runs directly z3, we need to keep it running
    public static final String ENTRYPOINT = "while :; do sleep 1000 ; done";

    private final String containerPath = "/smt2-resources/";
    private final GenericContainer<?> z3Prover;

    private final String resourcesFolder;
    private final String tmpFolderPath;


    public Solver(String resourcesFolder) {
        this.resourcesFolder = resourcesFolder;
        String instant = Long.toString(Instant.now().getEpochSecond());
        this.tmpFolderPath = "tmp_" + instant + "/";

        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> builder
                        .from(Z3_DOCKER_IMAGE)
                        .entryPoint(ENTRYPOINT)
                        .build());

        z3Prover = new GenericContainer<>(image)
                .withFileSystemBind(resourcesFolder, containerPath, BindMode.READ_WRITE);

        z3Prover.start();
    }


    /**
     * Deletes the tmp folder with all its content and then stops the Z3 Docker container.
     */
    @Override
    public void close() {
        try {
            FileUtils.deleteDirectory(new File(this.resourcesFolder + this.tmpFolderPath));
        } catch (IOException e) {
            log.error(String.format("Error deleting tmp folder '%s'. ", this.tmpFolderPath), e);
        }
        z3Prover.stop();
    }

    /**
     * @param cs a list of constraints to solve
     * @return a list with all the row with values that the solver found
     */
    public Map<String, RowWithValues> solve(List<RowToSolve> rowsToSolve, List<TableConstraint> cs) {
        String text = parseConstraintsToSmtText(rowsToSolve, cs);
        String fileName = storeToTmpFile(text);
        String model = solveFromFile(this.tmpFolderPath + fileName);
        return toSolvedConstraintsMap(model);
    }

    private void loadVariablesFrom(Map<String, String> variables, List<RowToSolve> rowsToSolve) {
        for (RowToSolve row : rowsToSolve) {
            String tableName = row.getTableName();
            for (String key : row.getColumns().keySet()) {
                variables.put(tableName + "__" + key, row.getColumns().get(key).toString());
            }
        }
    }

    String parseConstraintsToSmtText(List<RowToSolve> rowsToSolve, List<TableConstraint> cs) {

        // The variables that solve the constraint, the key is the variable name and the value is the type in smt format (e.g. "x" -> "Int")
        Map<String, String> variables = new HashMap<>();

        // The assertions that those variables need to satisfy in smt format (e.g. "(> x 5)")
        List<String> constraints = new ArrayList<>();


        // First load all the variables that the user needs to solve
        loadVariablesFrom(variables, rowsToSolve);
        // The assertions that those variables need to satisfy in smt format (e.g. "(> x 5)")
        for (TableConstraint c : cs) {
            Pair<Map<String, String>, String> pair = toSmtFormat(c);
            String tableName = c.getTableName();
            for (String key : pair.getFirst().keySet()) {
                if (!variables.containsKey(tableName + "__" + key)) {
                    variables.put(tableName + "__" + key, pair.getFirst().get(key));
                }
            }
            if (pair.getSecond() != null) {
                constraints.add(pair.getSecond());
            }
        }
        return asText(variables, constraints);
    }

    /**
     * Stores the content of the Smt2Writer in a file in the tmp folder.
     * @return the name of the file
     */
    private String storeToTmpFile(String text) {
        String fileName = "smt2_" + System.currentTimeMillis() + ".smt2";
        try {
            Files.createDirectories(Paths.get(this.resourcesFolder + this.tmpFolderPath));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating tmp folder '%s'. ", this.tmpFolderPath), e);
        }

        Path fullPath = Paths.get(  this.resourcesFolder + this.tmpFolderPath + fileName);
        writeToFile(text, fullPath.toString());

        return fileName;
    }

    /**
     * Parses the model from Z3 response and returns a map with the solved variables from the constraints
     * @param model the model as string
     * @return a map with the solved variables and their values: key table name, value the row for that table
     */
    private Map<String, RowWithValues> toSolvedConstraintsMap(String model) {
        String[] lines = model.split("\n");
        Map<String, RowWithValues> response = new HashMap<>();

        for (int i = 1; i < lines.length; i++) {
            // example: lines[i] === "((products__stock 1))"
            String removeBraces = lines[i].substring(2, lines[i].length()-2); // "products__stock 1"
            String[] keyAndValue = removeBraces.split(" "); // ["products__stock", "1"]
            String[] key = keyAndValue[0].split("__"); // ["products", "stock"]
            String tableName = key[0];
            String columnName = key[1];
            String currentValueAsString = keyAndValue[1];


            RowWithValues rowToEdit;

            if (response.containsKey(tableName)) {
                rowToEdit = response.get(tableName);
            } else {
                RowWithValues newRow = new RowWithValues(tableName);
                response.put(tableName, newRow);
                rowToEdit = newRow;
            }

            try {
                int v = Integer.parseInt(currentValueAsString);
                rowToEdit.withColumn(columnName, new IntSolvedValue(v));
            } catch(NumberFormatException | NullPointerException e) {
                try {
                    Double v = Double.parseDouble(currentValueAsString);
                    rowToEdit.withColumn(columnName, new RealSolvedValue(v));
                } catch (NumberFormatException | NullPointerException f) {
                    rowToEdit.withColumn(columnName, new StringSolvedValue(currentValueAsString));
                }
            }
        }
        return response;
    }

    /**
     * Reads from a file in the container 'filename' the smt2 problem, and runs z3 with it.
     * Returns the result as string.
     * The file must be in the containerPath defined in the constructor.
     * @param fileName the name of the file to read
     * @return the result of the Z3 solver with the obtained model as string
     */
    String solveFromFile(String fileName) {
        try {
            Container.ExecResult result = z3Prover.execInContainer("z3", containerPath + fileName);
            String stdout = result.getStdout();
            if (stdout == null || stdout.isEmpty()) {
                String stderr = result.getStderr();
                throw new RuntimeException(stderr);
            }
            return stdout;
        }
        catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Writes the content of the Smt2Writer in a file with the given filename
     * @param filename the name of the file
     */
    public void writeToFile(String text, String filename) {
        try {
            Files.write(Paths.get(filename), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String asText(Map<String, String> variables, List<String> constraints) {
        StringBuilder sb = new StringBuilder();

        sb.append("(set-logic QF_SLIA)\n");

        declareConstants(variables, sb);
        assertConstraints(constraints, sb);

        sb.append("(check-sat)\n");
        getValues(variables, sb);

        return sb.toString();
    }

    private void declareConstants(Map<String, String> variables, StringBuilder sb) {
        for (String key: variables.keySet()) {
            sb.append("(declare-fun ");
            sb.append(key); // variable name
            sb.append(" () ");
            sb.append(variables.get(key)); // type
            sb.append(")\n");
        }
    }

    private void assertConstraints(List<String> constraints, StringBuilder sb){
        for (String constraint : constraints) {
            sb.append("(assert ");
            sb.append(constraint);
            sb.append(")\n");
        }
    }

    private void getValues(Map<String, String> variables, StringBuilder sb) {
        for (String key : variables.keySet()) {
            sb.append("(get-value (");
            sb.append(key);
            sb.append("))\n");
        }
    }


    private Pair<Map<String, String>, String> toSmtFormat(TableConstraint c) {
        if (c instanceof AndConstraint) {

            AndConstraint andCondition = (AndConstraint) c;
            Pair<Map<String, String>, String> leftResponse = toSmtFormat(andCondition.getLeft());
            Pair<Map<String, String>, String> rightResponse = toSmtFormat(andCondition.getRight());

            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.putAll(leftResponse.getFirst());
            variablesAndType.putAll(rightResponse.getFirst());

            String smtRule = "(and " + leftResponse.getSecond() + " " + rightResponse.getSecond() + ")";
            return new Pair<>(variablesAndType, smtRule);

        } else if (c instanceof OrConstraint) {

            OrConstraint orCondition = (OrConstraint) c;
            List<TableConstraint> conditions = orCondition.getConstraintList();
            List<String> orMembers = new ArrayList<>();
            Map<String, String> variablesAndType = new HashMap<>();
            for (TableConstraint condition : conditions) {
                Pair<Map<String, String>, String>  response = toSmtFormat(condition);
                variablesAndType.putAll(response.getFirst());
                orMembers.add(response.getSecond());
            }
            String smtRule = "(or " + String.join(" ", orMembers) + ")";
            return new Pair<>(variablesAndType, smtRule);

        } else if (c instanceof EnumConstraint) {
            EnumConstraint enumConstraint = (EnumConstraint) c;
            List<String> enumValues = enumConstraint.getValuesAsStrings();
            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.put(enumConstraint.getColumnName(), "String"); // TODO: Check If enum constraint always string?
            String smtRule = toInValues(c.getTableName(), enumConstraint.getColumnName(), enumValues);
            return new Pair<>(variablesAndType, smtRule);

        } else if (c instanceof LowerBoundConstraint) {
            LowerBoundConstraint lowerBoundConstraint = (LowerBoundConstraint) c;
            String smtRule = "(<= " + lowerBoundConstraint.getTableName() + "__" + lowerBoundConstraint.getColumnName() + " " + lowerBoundConstraint.getLowerBound() + ")";

            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.put(lowerBoundConstraint.getColumnName(), "Int"); // TODO: Check numeric type
            return new Pair<>(variablesAndType, smtRule);
        } else if (c instanceof UpperBoundConstraint) {
            UpperBoundConstraint upperBoundConstraint = (UpperBoundConstraint) c;
            String smtRule = "(>= " + upperBoundConstraint.getTableName() + "__" + upperBoundConstraint.getColumnName() + " " + upperBoundConstraint.getUpperBound() + ")";
            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.put(upperBoundConstraint.getColumnName(), "Int"); // TODO: Check numeric type
            return new Pair<>(variablesAndType, smtRule);
        } else if (c instanceof RangeConstraint) {
            RangeConstraint rangeConstraint = (RangeConstraint) c;
            String columnName = rangeConstraint.getColumnName();
            String smtRule = "(and (>= " + rangeConstraint.getTableName() + "__" + columnName + " " + rangeConstraint.getMinValue() + ") (<= " + rangeConstraint.getTableName() + "__" + columnName + " " + rangeConstraint.getMaxValue() + "))";
            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.put(rangeConstraint.getColumnName(), "Int"); // TODO: Check numeric type
            return new Pair<>(variablesAndType, smtRule);
        } else if (c instanceof SimilarToConstraint) {
            SimilarToConstraint similarToConstraint = (SimilarToConstraint) c;
            // TODO: Probably this doesnt work
            String smtRule = "(like " + similarToConstraint.getTableName() + "__" + similarToConstraint.getColumnName() + " " + similarToConstraint.getPattern() + ")";
            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.put(similarToConstraint.getColumnName(), "String"); // TODO: Check string type
            return new Pair<>(variablesAndType, smtRule);
        } else if (c instanceof LikeConstraint) {
            LikeConstraint likeConstraint = (LikeConstraint) c;
            // TODO: Probably this doesnt work
            String smtRule = "(like " + likeConstraint.getTableName() + "__" + likeConstraint.getColumnName() + " " + likeConstraint.getPattern() + ")";
            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.put(likeConstraint.getColumnName(), "String"); // TODO: Check string type
            return new Pair<>(variablesAndType, smtRule);
        } else if (c instanceof IffConstraint) {
            IffConstraint iffConstraint = (IffConstraint) c;
            Pair<Map<String, String>, String> leftResponse = toSmtFormat(iffConstraint.getLeft());
            Pair<Map<String, String>, String> rightResponse = toSmtFormat(iffConstraint.getRight());
            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.putAll(leftResponse.getFirst());
            variablesAndType.putAll(rightResponse.getFirst());
            String smtRule = "(or (and " + leftResponse.getSecond() + " " + rightResponse.getSecond() +
                    ") (and (not " + leftResponse.getSecond() + ") (not " + rightResponse.getSecond() + ")))";
            return new Pair<>(variablesAndType, smtRule);
        } else if (c instanceof IsNotNullConstraint) {
            IsNotNullConstraint isNotNullConstraint = (IsNotNullConstraint) c;
            // Declaring a variable in smt is enough for it to return a value
            Map<String, String> variablesAndType = new HashMap<>();
            variablesAndType.put(isNotNullConstraint.getColumnName(), "String"); // TODO: Check string type
            return new Pair<>(variablesAndType, null);
        } else {
            throw new IllegalArgumentException("Unknown constraint type: " + c);
        }
    }

    private String toInValues(String tableName, String columnName, List<String> enumValues) {
        if (enumValues.isEmpty()) {
            return "";
        } else if (enumValues.size() == 1) {
            return "(= " + tableName + "__" + columnName + " \"" + enumValues.get(0) + "\")";
        }
        return "(or (= " + tableName + "__" + columnName + " \"" +  enumValues.get(0) + "\") " +
                toInValues(tableName, columnName, enumValues.subList(1, enumValues.size())) + ")";
    }
}


