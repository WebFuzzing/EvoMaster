package com.webfuzzing.asyncapi.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.IOException;

/**
 * Reading an AsyncAPI document as a tree.
 *
 * One reader handles both YAML and JSON, as JSON is valid YAML. snakeyaml's out-of-the-box
 * limits are far too low for real documents, so they are raised here, in one place rather than
 * at each call site.
 */
public class AsyncApiMapper {

    /*
        TODO Once the parser has stabilized, these could be configuration parameters instead of
        hard-coded constants.

        They exist because snakeyaml's defaults reject documents that are perfectly valid: the
        out-of-the-box code-point limit and cap on aliases are both reached by large published
        specifications.
     */

    private static final int CODE_POINT_LIMIT = 50 * 1024 * 1024;

    private static final int MAX_ALIASES_FOR_COLLECTIONS = 1000;

    private static final int NESTING_DEPTH_LIMIT = 100;

    private AsyncApiMapper() {
    }

    /**
     * Read a document, whether it is written in YAML or in JSON.
     *
     * @throws IOException if the text is not valid YAML/JSON
     */
    public static JsonNode readTree(String text) throws IOException {
        return mapper().readTree(text);
    }

    private static ObjectMapper mapper() {

        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(CODE_POINT_LIMIT);
        options.setMaxAliasesForCollections(MAX_ALIASES_FOR_COLLECTIONS);
        options.setNestingDepthLimit(NESTING_DEPTH_LIMIT);

        YAMLFactory yaml = new YAMLFactoryBuilder(new YAMLFactory())
                .loaderOptions(options)
                .build();

        return new ObjectMapper(yaml);
    }
}
