package org.evomaster.solver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Separates the two components of what one solver invocation costs: the round-trip of executing a
 * command inside the Docker container, and the time Z3 itself spends deciding the formula.
 *
 * Why this matters. Measurements across a benchmark showed a per-invocation cost that stayed flat
 * between 126 and 188 ms regardless of formula size, over formulas spanning 414 to 15,493 bytes.
 * The natural reading is that the container round-trip dominates and Z3 is negligible, which in turn
 * makes "avoid the round-trip" — by batching queries, keeping a persistent process, or using the
 * native API — the highest-value optimisation. That reading is an inference from the flatness, not a
 * measurement, and the two components had never been timed apart.
 *
 * This test times them apart. It runs its own container from the same image rather than reaching
 * into {@link Z3DockerExecutor}, so it measures the same mechanism without widening production API.
 */

public class Z3InvocationCostTest {

    private static GenericContainer<?> container;
    private static Path resources;

    private static final int WARMUP = 3;
    private static final int SAMPLES = 15;

    /** A formula of roughly the median size seen in practice, with a satisfiable row constraint. */
    private static final String FORMULA = buildFormula(12);

    private static String buildFormula(int rows) {
        // No set-logic, matching what SmtLibGenerator emits. Declaring QF_SLIA here made Z3 reject
        // every datatype declaration that followed ("logic does not support algebraic datatypes"),
        // so it answered sat on an empty problem: these measurements were timing a rejection rather
        // than a solve.
        StringBuilder b = new StringBuilder();
        b.append("(declare-datatypes ((Row 0)) (((mk-row (ID Int) (NAME String) (AGE Int)))))\n");
        for (int i = 1; i <= rows; i++) {
            b.append("(declare-const r").append(i).append(" Row)\n");
        }
        for (int i = 1; i <= rows; i++) {
            b.append("(assert (> (ID r").append(i).append(") ").append(i * 10).append("))\n");
            b.append("(assert (< (AGE r").append(i).append(") 120))\n");
            b.append("(assert (= (NAME r").append(i).append(") \"user").append(i).append("\"))\n");
        }
        b.append("(check-sat)\n");
        for (int i = 1; i <= rows; i++) {
            b.append("(get-value (r").append(i).append("))\n");
        }
        return b.toString();
    }

    @BeforeAll
    static void setUp() throws Exception {
        resources = Files.createTempDirectory("z3-cost");
        Files.write(resources.resolve("formula.smt2"), FORMULA.getBytes());
        Files.copy(
                Z3InvocationCostTest.class.getResourceAsStream("/hard_factoring.smt"),
                resources.resolve("hard.smt2"));

        // Built the same way production does: the base image has no long-running entrypoint, so one
        // is baked in, otherwise the container exits immediately and execInContainer has nothing to
        // attach to.
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withDockerfileFromBuilder(b -> b
                        .from(Z3DockerExecutor.Z3_DOCKER_IMAGE)
                        .entryPoint(Z3DockerExecutor.ENTRYPOINT)
                        .build());

        container = new GenericContainer<>(image)
                .withFileSystemBind(resources.toString(), "/smt2-resources/", BindMode.READ_WRITE);
        container.start();
    }

    @AfterAll
    static void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    private long medianOf(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    /** Times a command, discarding the first few runs so container warm-up does not skew the result. */
    private long medianMillis(String... command) throws Exception {
        for (int i = 0; i < WARMUP; i++) {
            container.execInContainer(command);
        }
        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            container.execInContainer(command);
            samples.add((System.nanoTime() - start) / 1_000_000);
        }
        return medianOf(samples);
    }

    /** Z3's own accounting of how long it spent, from the statistics its `-st` flag prints. */
    private double z3SelfReportedSeconds() throws Exception {
        return selfReportedSeconds("formula.smt2");
    }

    private double selfReportedSeconds(String fileName) throws Exception {
        Container.ExecResult r =
                container.execInContainer("z3", "-st", "/smt2-resources/" + fileName);
        // Fail here rather than returning a sentinel: a negative "duration" would flow into the
        // printed report and the assertions below, where it reads as a fast solve instead of as a
        // broken measurement.
        assertEquals(0, r.getExitCode(),
                "z3 did not run successfully on " + fileName + ": " + r.getStderr());
        Matcher m = Pattern.compile(":total-time\\s+([0-9.]+)").matcher(r.getStdout());
        assertTrue(m.find(),
                "z3 -st printed no :total-time for " + fileName + ", so its own timing cannot be read:\n"
                        + r.getStdout());
        return Double.parseDouble(m.group(1));
    }

    @Test
    public void reportWhereTheInvocationCostGoes() throws Exception {
        long floor = medianMillis("true");
        long solve = medianMillis("z3", "/smt2-resources/formula.smt2");
        double z3Seconds = z3SelfReportedSeconds();
        long z3Millis = Math.round(z3Seconds * 1000);

        System.out.printf("%nWhere one solver invocation spends its time (median of %d, %d bytes)%n",
                SAMPLES, FORMULA.length());
        System.out.printf("  container round-trip, no work   %6d ms%n", floor);
        System.out.printf("  full z3 invocation              %6d ms%n", solve);
        System.out.printf("  z3 self-reported solve time     %6d ms  (:total-time %.4f)%n", z3Millis, z3Seconds);
        System.out.printf("  unexplained by either           %6d ms%n", solve - floor - z3Millis);

        assertTrue(floor > 0, "expected the round-trip to be measurable");
        assertTrue(solve >= floor, "a z3 invocation cannot be cheaper than an empty command");
    }

    /**
     * Separates Z3's fixed cost from the part that grows with the problem.
     *
     * The distinction decides which optimisation is worth making. A cost that is essentially fixed —
     * loading the binary, initialising, parsing the input — disappears if the same process is reused
     * across queries. A cost that grows with the formula does not, and would have to be attacked by
     * making the formulas smaller instead.
     *
     * This also explains how the per-invocation cost could look flat across a wide range of formula
     * sizes while Z3 still accounts for a large share of it: a fixed cost is flat by definition.
     */
    @Test
    public void reportHowZ3CostScalesWithFormulaSize() throws Exception {
        System.out.printf("%nHow much of Z3's own time is fixed rather than proportional%n");
        System.out.printf("  %8s %10s %14s %14s%n", "rows", "bytes", "z3 self (ms)", "full call (ms)");

        long smallest = -1;
        long largest = -1;
        for (int rows : new int[]{1, 12, 60, 240}) {
            String formula = buildFormula(rows);
            String name = "scaling-" + rows + ".smt2";
            Files.write(resources.resolve(name), formula.getBytes());

            long z3Millis = Math.round(selfReportedSeconds(name) * 1000);
            long full = medianMillis("z3", "/smt2-resources/" + name);
            System.out.printf("  %8d %10d %14d %14d%n", rows, formula.length(), z3Millis, full);

            if (rows == 1) smallest = z3Millis;
            largest = z3Millis;
        }

        assertTrue(smallest >= 0 && largest >= 0, "expected z3 to report its own time");
    }

    /**
     * What actually dominates an invocation: cost that is paid once per call regardless of the
     * problem, rather than cost that grows with it.
     *
     * This is a correction of the reading one would reach from the flatness alone. Observing that a
     * call costs the same for a small formula as for a large one suggests that solving is negligible
     * and the container round-trip is everything. The measurement shows otherwise: the round-trip and
     * Z3 each account for a comparable share, and the reason the total looks flat is that *both* are
     * essentially fixed — Z3's time barely moves across two orders of magnitude of formula size,
     * because it is spent starting the process and reading the input rather than searching.
     *
     * The practical conclusion survives, and for a better reason. Since almost the whole cost is
     * per-invocation overhead rather than work, reusing a single solver process across queries — or
     * batching several queries into one call — removes most of it, whereas simplifying the formulas
     * would remove almost none. What changes is the justification, not the remedy.
     */
    @Test
    public void theCostIsDominatedByFixedPerInvocationOverhead() throws Exception {
        String tiny = "tiny.smt2";
        String big = "big.smt2";
        Files.write(resources.resolve(tiny), buildFormula(1).getBytes());
        Files.write(resources.resolve(big), buildFormula(60).getBytes());

        long small = medianMillis("z3", "/smt2-resources/" + tiny);
        long large = medianMillis("z3", "/smt2-resources/" + big);

        // A ~34x increase in formula size must not come close to doubling the cost; if it ever does,
        // the problem has become size-driven and the optimisation priorities need revisiting.
        assertTrue(large < small * 2,
                "expected cost to be dominated by fixed overhead, but a 34x larger formula went from "
                        + small + " ms to " + large + " ms");
    }

    /**
     * Checks that the configured timeout actually bounds how long an invocation can take.
     *
     * The solver is invoked with Z3's {@code -t:<ms>} flag, which its own help describes as a *soft*
     * timeout that "only kills the current query". Soft is doing a lot of work in that sentence: it
     * is honoured at the points where Z3 chooses to check it, so on problems where those points are
     * far apart the wall-clock time can exceed the configured bound by a wide margin — and it does,
     * silently, because the run still finishes normally rather than reporting `unknown`.
     *
     * That matters because the timeout is the only mechanism preventing a single pathological query
     * from consuming a large share of the search budget. If it does not bound wall-clock time, there
     * is nothing that does.
     *
     * Z3 also offers {@code -T:<seconds>}, a hard timeout on wall clock. This test measures both so
     * the difference is visible rather than assumed.
     */
    @Test
    public void reportWhetherTheTimeoutBoundsWallClock() throws Exception {
        long softOnly = timedRun("z3", "-t:1000", "/smt2-resources/hard.smt2");
        long withHard = timedRun("z3", "-t:1000", "-T:2", "/smt2-resources/hard.smt2");

        System.out.printf("%nDoes the configured timeout bound wall-clock time?%n");
        System.out.printf("  soft only  (-t:1000)          %6d ms%n", softOnly);
        System.out.printf("  soft + hard (-t:1000 -T:2)    %6d ms%n", withHard);

        assertTrue(withHard <= softOnly + 500,
                "expected the hard timeout to bound the call at least as tightly as the soft one,"
                        + " but got " + softOnly + " ms vs " + withHard + " ms");
    }

    /**
     * The same question for a formula over strings, which is what the generator emits for any
     * VARCHAR column and therefore what most real schemas produce.
     *
     * Z3 decides string constraints with a different engine than arithmetic, and the points at which
     * it consults the soft timeout are further apart there. The concern is not that such a formula is
     * slow — some are — but that the configured bound stops applying to it, so the cost of a single
     * query becomes unbounded in practice while the run still reports a normal result rather than
     * `unknown`.
     */
    @Test
    public void reportWhetherTheTimeoutBoundsStringFormulas() throws Exception {
        Files.write(resources.resolve("strings.smt2"), buildStringFormula().getBytes());

        long softOnly = timedRun("z3", "-t:1000", "/smt2-resources/strings.smt2");
        long withHard = timedRun("z3", "-t:1000", "-T:2", "/smt2-resources/strings.smt2");

        System.out.printf("%nDoes the timeout bound a string-heavy formula?%n");
        System.out.printf("  soft only  (-t:1000)          %6d ms%n", softOnly);
        System.out.printf("  soft + hard (-t:1000 -T:2)    %6d ms%n", withHard);
        System.out.printf("  soft timeout overshoot        %6d ms%n", softOnly - 1000);

        assertTrue(withHard <= softOnly + 500,
                "expected the hard timeout to bound the call, but got " + softOnly + " ms vs "
                        + withHard + " ms");
    }

    /**
     * A formula whose difficulty lies in string reasoning: concatenations constrained to match a
     * literal, with length bounds that force the search to try many splits.
     */
    private static String buildStringFormula() {
        StringBuilder b = new StringBuilder();
        int parts = 8;
        for (int i = 1; i <= parts; i++) {
            b.append("(declare-const s").append(i).append(" String)\n");
        }
        StringBuilder concat = new StringBuilder("s1");
        for (int i = 2; i <= parts; i++) {
            concat = new StringBuilder("(str.++ " + concat + " s" + i + ")");
        }
        b.append("(assert (= ").append(concat)
         .append(" \"abcdefghijklmnopqrstuvwxyz0123456789abcdefghij\"))\n");
        for (int i = 1; i <= parts; i++) {
            b.append("(assert (> (str.len s").append(i).append(") 1))\n");
            b.append("(assert (< (str.len s").append(i).append(") 12))\n");
            b.append("(assert (not (str.contains s").append(i).append(" \"zz\")))\n");
        }
        b.append("(check-sat)\n");
        for (int i = 1; i <= parts; i++) {
            b.append("(get-value (s").append(i).append("))\n");
        }
        return b.toString();
    }

    /** Wall time of a single invocation, without the warm-up loop used for the cost medians. */
    private long timedRun(String... command) throws Exception {
        long start = System.nanoTime();
        container.execInContainer(command);
        return (System.nanoTime() - start) / 1_000_000;
    }
}
