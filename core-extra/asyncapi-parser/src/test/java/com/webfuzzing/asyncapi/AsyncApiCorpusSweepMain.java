package com.webfuzzing.asyncapi;

import com.webfuzzing.asyncapi.access.AsyncApiAccess;
import com.webfuzzing.asyncapi.models.AsyncApiDocument;
import com.webfuzzing.asyncapi.models.AsyncApiOperation;
import com.webfuzzing.asyncapi.models.AsyncApiServer;
import com.webfuzzing.asyncapi.parser.AsyncApiParsingException;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Parses every AsyncAPI document in a folder tree and prints what came out of each.
 *
 * This is a research aid rather than a test: it is meant to be pointed at a local corpus of
 * real documents, far too large to check into a repository. It is how a change to the parser is
 * checked against every document at once -- what still parses, how many operations were found,
 * and what had to be skipped.
 *
 * Usage: pass the folder to sweep as the first argument.
 */
public class AsyncApiCorpusSweepMain {

    private AsyncApiCorpusSweepMain() {
    }

    public static void main(String[] args) {

        File root = new File(args.length > 0 ? args[0] : defaultFolder());

        if (!root.exists()) {
            System.out.println("Usage: AsyncApiCorpusSweepMain <folder of AsyncAPI documents>");
            System.out.println("No such folder: " + root.getAbsolutePath());
            return;
        }

        List<File> documents = new ArrayList<>();
        collectDocuments(root, documents);
        Collections.sort(documents, Comparator.comparing(File::getPath));

        int parsed = 0;
        int rejected = 0;
        int withWarnings = 0;

        for (File file : documents) {

            String label = file.getAbsolutePath().substring(root.getAbsolutePath().length() + 1);

            try {
                AsyncApiDocument document = AsyncApiAccess.getAsyncApiFromLocation(file.getAbsolutePath());

                parsed++;

                int driveable = 0;
                int withReply = 0;

                for (AsyncApiOperation operation : document.getOperations().values()) {
                    if (operation.getAction() == AsyncApiOperation.Action.RECEIVE
                            && !operation.getMessageIds().isEmpty()) {
                        driveable++;
                    }
                    if (operation.getReply() != null) {
                        withReply++;
                    }
                }

                System.out.println(
                        "OK    " + label
                                + " | v" + document.getVersion()
                                + " | " + document.getOperations().size() + " ops"
                                + " (" + driveable + " consumed, " + withReply + " with reply)"
                                + " | " + document.getChannels().size() + " channels"
                                + " | " + document.getMessages().size() + " messages"
                                + " | " + protocolsOf(document));

                if (!document.getWarnings().isEmpty()) {
                    withWarnings++;
                    for (String warning : document.getWarnings()) {
                        System.out.println("        ! " + warning);
                    }
                }

            } catch (AsyncApiParsingException e) {
                rejected++;
                System.out.println("SKIP  " + label + " | " + e.getMessage());
            } catch (Exception e) {
                rejected++;
                System.out.println(
                        "FAIL  " + label + " | " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        System.out.println();
        System.out.println(
                documents.size() + " documents: " + parsed + " parsed (" + withWarnings
                        + " with warnings), " + rejected + " not parsed");
    }

    private static String protocolsOf(AsyncApiDocument document) {

        if (document.getServers().isEmpty()) {
            return "no server";
        }

        StringBuilder protocols = new StringBuilder();

        for (AsyncApiServer server : document.getServers().values()) {
            if (protocols.length() > 0) {
                protocols.append(",");
            }
            protocols.append(server.getProtocol());
        }

        return protocols.toString();
    }

    private static void collectDocuments(File folder, List<File> found) {

        File[] entries = folder.listFiles();

        if (entries == null) {
            return;
        }

        for (File entry : entries) {
            if (entry.isDirectory()) {
                collectDocuments(entry, found);
            } else if (entry.getName().endsWith(".yaml") || entry.getName().endsWith(".yml")) {
                found.add(entry);
            }
        }
    }

    /**
     * The documents that ship with the tests, so that this runs for anybody who checks the
     * repository out rather than only where a research corpus happens to live.
     */
    private static String defaultFolder() {

        URL resource = AsyncApiCorpusSweepMain.class.getResource("/asyncapi");

        if (resource == null) {
            return ".";
        }

        try {
            return new File(resource.toURI()).getAbsolutePath();
        } catch (Exception e) {
            return ".";
        }
    }
}
