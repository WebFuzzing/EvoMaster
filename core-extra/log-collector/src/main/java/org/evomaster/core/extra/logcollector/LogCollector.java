package org.evomaster.core.extra.logcollector;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.evomaster.core.extra.shared.AdditionalTargetCollector;
import org.evomaster.core.extra.shared.TargetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AdditionalTargetCollector} that uses log-template coverage as an extra fitness signal.
 * After each test, it collects Docker container logs and uses Drain (via loglead) to extract
 * novel log event templates, which are reported as additional coverage targets to EvoMaster.
 *
 * <p>Note: {@code LoggerFactory.getLogger()} is used here instead of {@code LoggingUtil.getInfoLogger()}
 * because this module ({@code core-extra}) cannot depend on {@code core} without creating a circular
 * dependency. Informational log messages will therefore not appear in EvoMaster's console output.</p>
 */
public class LogCollector implements AdditionalTargetCollector {

  /** Accumulated targets for the current test; cleared at the start of each new test. */
  private final List<TargetInfo> results = new ArrayList<>();

  // LoggerFactory is used instead of LoggingUtil (see class-level note above).
  private static final Logger log = LoggerFactory.getLogger(LogCollector.class);

  /** Absolute path of the EvoMaster working directory; all output paths are resolved relative to it. */
  private final Path workDir;

  /** Command used to launch the parser daemon process. */
  private String[] daemonCommand;

  /** Path to the file capturing daemon stderr output: {@code {workDir}/logs/processbuilder-log}. */
  private String logFilePath;

  /** Path to the Drain results file written by the daemon: {@code {workDir}/templates/unique-templates.txt}. */
  private String uniqueTemplatesFile;

  /**
   * Docker Compose project name used to filter which containers are monitored.
   * Read from the {@code LOG_COLLECTOR_PROJECT} environment variable; {@code null} means all containers.
   */
  private final String composeProject;

  /** Epoch-millisecond timestamp recorded at the start of each test execution. */
  private long startTimeMillis;

  /**
   * Persistent daemon process kept alive across all tests to avoid paying Python's startup
   * cost (loading docker, polars, loglead) on every test.
   * Non-final so it can be replaced if the daemon crashes and must be restarted.
   */
  private Process daemonProcess;

  /** Writer wired to the daemon's stdin; used to send time-window requests. */
  private BufferedWriter daemonIn;

  /** Reader wired to the daemon's stdout; used to receive "OK" / "ERROR" responses. */
  private BufferedReader daemonOut;

  /**
   * Creates a LogCollector for production use.
   *
   * <p><strong>Side effects:</strong></p>
   * <ul>
   *   <li>Creates {@code logs/} and {@code templates/} directories under the working directory.</li>
   *   <li>Pulls and starts the parser daemon as a Docker container ({@code nanarei/evolog-parser:latest}).</li>
   *   <li>Registers a JVM shutdown hook that closes the daemon and deletes collected log files.</li>
   * </ul>
   */
  public LogCollector() {
    workDir = Paths.get("").toAbsolutePath();
    composeProject = System.getenv("LOG_COLLECTOR_PROJECT");

    // Build the docker run command. The parser daemon runs as a container so the
    // user needs no local Python installation. The Docker socket is mounted so the
    // container can reach the host Docker daemon to fetch SUT container logs.
    List<String> cmd = new ArrayList<>(Arrays.asList(
        "docker", "run", "--rm", "-i",
        "-v", workDir + "/logs:/app/logs",
        "-v", workDir + "/templates:/app/templates",
        "-v", "/var/run/docker.sock:/var/run/docker.sock"
    ));
    if (composeProject != null && !composeProject.isEmpty()) {
      cmd.add("-e");
      cmd.add("COMPOSE_PROJECT=" + composeProject);
    }
    cmd.add("nanarei/evolog-parser:latest");
    daemonCommand = cmd.toArray(new String[0]);

    initCommon();
  }

  /**
   * Package-private constructor for tests: skips JAR extraction and drives a custom daemon command.
   * Has the same side effects as the public constructor except no file is extracted from the JAR.
   */
  LogCollector(String[] daemonCommand) {
    workDir = Paths.get("").toAbsolutePath();
    this.daemonCommand = daemonCommand;
    composeProject = null;
    initCommon();
  }

  /**
   * Shared initialisation for both constructors.
   *
   * <p><strong>Side effects:</strong> creates directories on disk, spawns the parser daemon subprocess,
   * and registers a JVM shutdown hook that stops the daemon and cleans up collected log files.</p>
   */
  private void initCommon() {
    logFilePath         = workDir.resolve("logs/processbuilder-log").toString();
    uniqueTemplatesFile = workDir.resolve("templates/unique-templates.txt").toString();

    try {
      Files.createDirectories(Paths.get(logFilePath).getParent());
      Files.newOutputStream(Paths.get(logFilePath),
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).close();
    } catch (IOException ignored) {}

    try {
      Files.createDirectories(Paths.get(uniqueTemplatesFile).getParent());
    } catch (IOException ignored) {}

    try {
      startDaemon();
    } catch (IOException e) {
      throw new RuntimeException("Failed to start parser daemon", e);
    }

    Path evoLogsDir = workDir.resolve("logs/evomaster-logs");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      // Closing daemonIn signals the Python daemon to exit cleanly (EOF on stdin).
      try { daemonIn.close(); } catch (IOException ignored) {}
      if (!Files.exists(evoLogsDir)) return;
      try {
        Files.walk(evoLogsDir)
            .sorted(Comparator.reverseOrder())
            .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
      } catch (IOException ignored) {}
    }));
  }

  // The daemon runs with CWD as its working directory so relative paths in parser.py resolve correctly.
  private void startDaemon() throws IOException {
    ProcessBuilder pb = new ProcessBuilder(daemonCommand);
    pb.directory(workDir.toFile());
    // Redirect daemon stderr to the log file so debug output does not pollute the IPC stdout channel.
    pb.redirectError(new File(logFilePath));
    daemonProcess = pb.start();
    daemonIn  = new BufferedWriter(new OutputStreamWriter(
        daemonProcess.getOutputStream(), StandardCharsets.UTF_8));
    daemonOut = new BufferedReader(new InputStreamReader(
        daemonProcess.getInputStream(), StandardCharsets.UTF_8));
  }

  // Forcibly kills the current daemon and starts a fresh one so subsequent tests are not blocked.
  private void restartDaemon() {
    try {
      daemonProcess.destroyForcibly();
    } catch (Exception ignored) {}
    try {
      startDaemon();
      log.warn("Parser daemon restarted successfully");
    } catch (IOException e) {
      log.error("Failed to restart parser daemon: {}", e.getMessage());
    }
  }

  @Override
  public void goingToStartExecutingNewTest() {
    log.info("Going to start executing new test");
    results.clear();
    startTimeMillis = Instant.now().toEpochMilli();
  }

  @Override
  public void reportActionIndex(int actionIndex) {
    // actionIndex is not tracked; TargetInfo uses -1 to indicate absence
  }

  @Override
  public List<TargetInfo> testFinishedCollectResult() {
    log.info("Test finished, collecting log templates");
    results.clear();

    long endTimeMillis = Instant.now().toEpochMilli();

    if (!daemonProcess.isAlive()) {
      log.warn("Parser daemon is not running — restarting for next test");
      restartDaemon();
      return Collections.emptyList();
    }

    try {
      // Send the test's time window to the daemon as "startMs endMs\n".
      daemonIn.write(startTimeMillis + " " + endTimeMillis);
      daemonIn.newLine();
      daemonIn.flush();

      // Poll for the daemon's response with a timeout so a hanging daemon does not freeze EvoMaster.
      long deadline = System.currentTimeMillis() + 60_000;
      while (!daemonOut.ready()) {
        if (System.currentTimeMillis() > deadline) {
          log.warn("Parser daemon timed out after 60 s — skipping this test");
          return Collections.emptyList();
        }
        if (!daemonProcess.isAlive()) {
          log.warn("Parser daemon exited unexpectedly — restarting for next test");
          restartDaemon();
          return Collections.emptyList();
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return Collections.emptyList();
        }
      }

      // The daemon responds with "OK" on success or "ERROR: <message>" on failure.
      String response = daemonOut.readLine();
      if (response == null || !response.equals("OK")) {
        log.error("Parser daemon error: {}", response);
        return Collections.emptyList();
      }

      Path resultPath = Paths.get(uniqueTemplatesFile);
      if (!Files.exists(resultPath)) {
        log.error("Result file not found: {}", uniqueTemplatesFile);
        return Collections.emptyList();
      }

      // Each line is "{service}:{drain_id}\t{template_text}".
      // Only the stable drain ID is used as descriptiveId; the template text is for debugging only.
      for (String line : Files.readAllLines(resultPath, StandardCharsets.UTF_8)) {
        if (line.trim().isEmpty()) continue;
        String descriptiveId = "log:" + line.split("\t", 2)[0];
        results.add(new TargetInfo(descriptiveId, 1.0, -1));
      }

    } catch (IOException e) {
      throw new RuntimeException("Parser daemon communication failed", e);
    }

    return results;
  }
}
