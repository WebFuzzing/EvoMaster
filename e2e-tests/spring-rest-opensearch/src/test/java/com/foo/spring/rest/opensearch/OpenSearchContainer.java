package com.foo.spring.rest.opensearch;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.regex.Pattern;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

/**
 * The Opensearch Docker container (single node cluster) which exposes by default ports 9200
 * (http/https) and 9300 (tcp, deprecated).
 */
public class OpenSearchContainer<SELF extends OpenSearchContainer<SELF>> extends GenericContainer<SELF> {
    // The initial password is required starting from OpenSearch 2.12.0
    private static final Pattern OPENSEARCH_INITIAL_PASSWORD_VERSION = Pattern.compile(
        "^(([3-9][.]\\d+[.]\\d+|[2][.][1][2-9]+[.]\\d+|[2][.][2-9]\\d+[.]\\d+)(-SNAPSHOT)?|latest)$");

    // Default username to connect to Opensearch instance
    private static final String DEFAULT_USER = "admin";
    // Default password to connect to Opensearch instance
    private static final String DEFAULT_PASSWORD = "admin";
    // Default initial password to connect to Opensearch instance
    private static final String DEFAULT_INITIAL_PASSWORD = "_ad0m#Ns_";

    // Default HTTP port.
    private static final int DEFAULT_HTTP_PORT = 9200;

    // Default TCP port (deprecated and may be removed in future versions).
    private static final int DEFAULT_TCP_PORT = 9300;

    // Opensearch Docker base image.
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("opensearchproject/opensearch");

    // Disables (or enables) security plugin. If security is enabled, the communication protocol switches from HTTP to
    // HTTPs,
    // along with Basic Auth being used.
    private boolean disableSecurity = true;
    private boolean requireInitialPassword = false;
    private String password = DEFAULT_PASSWORD;

    /**
     * Create an Opensearch Container by passing the full docker image name.
     *
     * @param dockerImageName Full docker image name as a {@link String}, like:
     *     opensearchproject/opensearch:1.2.4 opensearchproject/opensearch:1.3.1
     *     opensearchproject/opensearch:2.0.0
     */
    public OpenSearchContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Create an Opensearch Container (with security plugin enabled) by passing the full docker image
     * name.
     *
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like:
     *
     *      DockerImageName.parse("opensearchproject/opensearch:1.2.4")
     *      DockerImageName.parse("opensearchproject/opensearch:1.3.1")
     *      DockerImageName.parse("opensearchproject/opensearch:2.0.0")
     *
     */
    public OpenSearchContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        final String version = dockerImageName.getVersionPart();
        if (version == null || version.isEmpty()) {
            requireInitialPassword = false; /* we don't know the version */
        } else {
            requireInitialPassword =
                OPENSEARCH_INITIAL_PASSWORD_VERSION.matcher(version).matches();
        }
    }

    /**
     * Should the security plugin be enabled or stay disabled (default value). If the security
     * plugin is enabled, HTTPS protocol is going to be used along with the default username / password.
     * @return this container instance
     */
    public SELF withSecurityEnabled() {
        this.disableSecurity = false;
        return self();
    }

    @Override
    protected void configure() {
        super.configure();

        withNetworkAliases("opensearch-" + Base58.randomString(6));
        withEnv("discovery.type", "single-node");
        if (disableSecurity) {
            withEnv("DISABLE_SECURITY_PLUGIN", Boolean.toString(disableSecurity));
        } else if (requireInitialPassword) {
            // Check if the OPENSEARCH_INITIAL_ADMIN_PASSWORD is already provided
            password = getEnvMap().get("OPENSEARCH_INITIAL_ADMIN_PASSWORD");
            if (password == null || password.isEmpty()) {
                withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", DEFAULT_INITIAL_PASSWORD);
                password = DEFAULT_INITIAL_PASSWORD;
            }
        }
        addExposedPorts(DEFAULT_HTTP_PORT, DEFAULT_TCP_PORT);

        final WaitStrategy waitStrategy;
        if (!disableSecurity) {
            // By default, Opensearch uses self-signed certificates for HTTPS, allowing insecure
            // connection in order to skip the certificate validation checks.
            waitStrategy = new HttpWaitStrategy()
                .usingTls()
                .allowInsecure()
                .forPort(DEFAULT_HTTP_PORT)
                .withBasicCredentials(DEFAULT_USER, password)
                .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED)
                .withReadTimeout(Duration.ofSeconds(10))
                .withStartupTimeout(Duration.ofMinutes(5));
        } else {
            waitStrategy = new HttpWaitStrategy()
                .forPort(DEFAULT_HTTP_PORT)
                .forStatusCodeMatching(response -> response == HTTP_OK)
                .withReadTimeout(Duration.ofSeconds(10))
                .withStartupTimeout(Duration.ofMinutes(5));
        }

        setWaitStrategy(waitStrategy);
    }

    /**
     * Return HTTP(s) host and port to connect to Opensearch container.
     *
     * @return HTTP(s) host and port (in a form of "host:port")
     */
    public String getHttpHostAddress() {
        return (disableSecurity ? "http://" : "https://") + getHost() + ":" + getMappedPort(DEFAULT_HTTP_PORT);
    }

    /**
     * Check if security plugin was enabled or not for this container
     *
     * @return "true" if if security plugin was enabled for this container, "false" otherwise
     */
    public boolean isSecurityEnabled() {
        return !disableSecurity;
    }

    /**
     * Return socket address to connect to Opensearch over TCP. The TransportClient will is deprecated
     * and may be removed in future versions.
     *
     * @return TCP socket address
     */
    @Deprecated
    public InetSocketAddress getTcpHost() {
        return new InetSocketAddress(getHost(), getMappedPort(DEFAULT_TCP_PORT));
    }

    /**
     * Return user name to connect to Opensearch container (if security plugin is enabled)
     * @return user name to connect to Opensearch container
     */
    public String getUsername() {
        return DEFAULT_USER;
    }

    /**
     * Return password to connect to Opensearch container (if security plugin is enabled)
     * @return password to connect to Opensearch container
     */
    public String getPassword() {
        return password;
    }
}