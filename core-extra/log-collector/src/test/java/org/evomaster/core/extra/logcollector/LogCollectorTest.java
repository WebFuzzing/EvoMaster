package org.evomaster.core.extra.logcollector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.evomaster.core.extra.shared.TargetInfo;

public class LogCollectorTest {

    private Path getResource(String name) throws Exception {
        URL url = getClass().getClassLoader().getResource(name);
        Assertions.assertNotNull(url, "Test resource not found: " + name);
        return Paths.get(url.toURI());
    }

    @Test
    void logCollector_testFinishedCollectResult_returnsTargetInfosFromDaemon() throws Exception {
        Path script = getResource("replay_daemon.py");
        Path logDir = getResource("test-logs/v2-sample");
        Path outputFile = Paths.get("").toAbsolutePath().resolve("templates/unique-templates.txt");

        String[] daemonCmd = {
            "python3", script.toString(), logDir.toString(), outputFile.toString()
        };

        LogCollector plugin = new LogCollector(daemonCmd);
        plugin.goingToStartExecutingNewTest();
        List<TargetInfo> result = plugin.testFinishedCollectResult();

        Assertions.assertFalse(result.isEmpty(), "Should return at least one TargetInfo from log data");
        for (TargetInfo t : result) {
            Assertions.assertTrue(t.getDescriptiveId().startsWith("log:"),
                "DescriptiveId should start with 'log:': " + t.getDescriptiveId());
            Assertions.assertTrue(t.getDescriptiveId().contains(":"),
                "DescriptiveId should contain service:drainId after 'log:': " + t.getDescriptiveId());
        }
    }

    @Test
    void logCollector_testFinishedCollectResult_returnsEmptyOnDaemonError() throws Exception {
        Path script = getResource("error_daemon.py");
        String[] daemonCmd = {"python3", script.toString()};

        LogCollector plugin = new LogCollector(daemonCmd);
        plugin.goingToStartExecutingNewTest();
        List<?> result = plugin.testFinishedCollectResult();

        Assertions.assertTrue(result.isEmpty(), "Should return empty list when daemon reports error");
    }
}
