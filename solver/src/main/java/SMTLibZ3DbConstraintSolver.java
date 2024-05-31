import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.evomaster.client.java.controller.api.dto.database.schema.ColumnDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.api.dto.database.schema.DbSchemaDto;
import org.evomaster.client.java.controller.api.dto.database.schema.TableDto;
import org.evomaster.core.sql.SqlAction;
import org.evomaster.core.sql.schema.ColumnDataType;
import org.evomaster.core.sql.schema.ForeignKey;
import org.evomaster.dbconstraint.TableConstraint;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.evomaster.core.sql.schema.Column;
import org.evomaster.core.sql.schema.Table;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A smt2 solver implementation using Z3 in a Docker container.
 * It generates the SMT problem from the database schema and the query
 * and then executes Z3 to get values and returns the necessary list of SqlActions
 * to satisfy the query.
 */
public class SMTLibZ3DbConstraintSolver implements DbConstraintSolver {

    private final String resourcesFolder;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SMTLibZ3DbConstraintSolver.class.getName());

    private final SmtLibGenerator generator;
    private final Z3DockerExecutor executor;
    private final DbSchemaDto schemaDto;

    public SMTLibZ3DbConstraintSolver(DbSchemaDto schemaDto) {

        String resourcesFolder = tmpResourcesFolder();
        createTmpFolder(resourcesFolder);
        this.resourcesFolder = resourcesFolder;
        this.schemaDto = schemaDto;
        this.generator = new SmtLibGenerator(schemaDto);
        this.executor = new Z3DockerExecutor(resourcesFolder);
    }

    private String tmpResourcesFolder() {
        String instant = Long.toString(Instant.now().getEpochSecond());
        String tmpFolderPath = "/tmp/" + instant + "/";
        return System.getProperty("user.dir") + tmpFolderPath;
    }

    private void createTmpFolder(String resourcesFolderWithTmpDir) {
        try {
            Files.createDirectories(Paths.get(resourcesFolderWithTmpDir));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating tmp folder '%s'. ", resourcesFolderWithTmpDir), e);
        }
    }

    /**
     * Deletes the tmp folder with all its content and then stops the Z3 Docker container.
     */
    @Override
    public void close() {
        try {
            FileUtils.deleteDirectory(new File(this.resourcesFolder));
        } catch (IOException e) {
            log.error(String.format("Error deleting tmp folder '%s'. ", this.resourcesFolder), e);
        }
        executor.close();
    }

    /**
     * From the database schema and the query, it generates the SMT problem to make the query return a value
     * Then executes Z3 to get values and returns the necessary list of SqlActions
     * @param sqlQuery the SQL query to solve
     * @return a list of SQL actions that can be executed to satisfy the query
     */
    @Override
    public List<SqlAction> solve(String sqlQuery) {

        Statement queryStatement = parseStatement(sqlQuery);
        SMTLib smtLib = this.generator.generateSMT(queryStatement);
        String fileName = storeToTmpFile(smtLib);
        String z3Response = executor.solveFromFile(fileName);

        return toSqlActionList(z3Response);
    }

    private static Statement parseStatement(String sqlQuery) {
        Statement queryStatement;
        try {
            queryStatement = CCJSqlParserUtil.parse(sqlQuery);
        } catch (JSQLParserException e) {
            throw new RuntimeException(e);
        }
        return queryStatement;
    }

    private List<SqlAction> toSqlActionList(String z3Response) {
        return new ArrayList<>();
    }

    private String storeToTmpFile(SMTLib smtLib) {
        String fileName = "smt2_" + System.currentTimeMillis() + ".smt2";
        String filePath = this.resourcesFolder + fileName;

        try {
            Files.write(Paths.get(filePath), smtLib.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return fileName;
    }

}
