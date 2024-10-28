package com.foo.rest.emb.json.language;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evomaster.client.java.utils.SimpleLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This code is taken from LanguageTool
 * G: https://github.com/languagetool-org/languagetool
 * L: LGPL-2.1
 * P: src/main/java/org/languagetool/server/ResultExtender.java
 */
public class ResultExtender {

    private final URL url;
    private final int connectTimeoutMillis;
    private final ObjectMapper mapper = new ObjectMapper();

//    private static final Logger logger = LoggerFactory.getLogger(ResultExtender.class);

    private static final SimpleLogger logger = new SimpleLogger();


    ResultExtender(String url, int connectTimeoutMillis) {
        this.url = Tools.getUrl(url);
        if (connectTimeoutMillis <= 0) {
            throw new IllegalArgumentException("connectTimeoutMillis must be > 0: " + connectTimeoutMillis);
        }
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    List<RemoteRuleMatch> getExtensionMatches(String plainText, Map<String, String> params) throws IOException {
        HttpURLConnection huc = (HttpURLConnection) url.openConnection();
        HttpURLConnection.setFollowRedirects(false);
        huc.setConnectTimeout(connectTimeoutMillis);
        huc.setReadTimeout(connectTimeoutMillis*2);
        // longer texts take longer to check, so increase the timeout:
        float factor = plainText.length() / 1000.0f;
        if (factor > 1) {
            int increasedTimeout = (int)(connectTimeoutMillis * 2 * Math.min(factor, 5));
            huc.setReadTimeout(increasedTimeout);
        }
        huc.setRequestMethod("POST");
        huc.setDoOutput(true);
        try {
            huc.connect();
            try (DataOutputStream wr = new DataOutputStream(huc.getOutputStream())) {
                String urlParameters = "";
                List<String> ignoredParameters = Arrays.asList("enableHiddenRules", "username", "password", "token", "apiKey", "c");
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    // We could set 'language' to the language already detected, so the queried server
                    // wouldn't need to guess the language again. But then we'd run into cases where
                    // we get an error because e.g. 'noopLanguages' can only be used with 'language=auto'
                    if (!ignoredParameters.contains(entry.getKey())) {
                        urlParameters += "&" + encode(entry.getKey()) + "=" + encode(entry.getValue());
                    }
                }
                byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                wr.write(postData);
            }
            InputStream input = huc.getInputStream();
            return parseJson(input);
        } catch (SSLHandshakeException | SocketTimeoutException e) {
            // "hard" errors that will probably not resolve themselves easily:
            logger.error("Error while querying hidden matches server", e);
            throw e;
        } catch (Exception e) {
            // These are issue that can be request-specific, like wrong parameters. We don't throw an
            // exception, as the calling code would otherwise assume this is a persistent error:
            logger.warn("Warn: Failed to query hidden matches server at " + url + ": " + e.getClass() + ": " + e.getMessage() + ", input was " + plainText.length() + " characters - request-specific error, ignoring");
            return Collections.emptyList();
        } finally {
            huc.disconnect();
        }
    }

    private String encode(String plainText) throws UnsupportedEncodingException {
        return URLEncoder.encode(plainText, StandardCharsets.UTF_8.name());
    }

    private List<RemoteRuleMatch> parseJson(InputStream inputStream) throws IOException {
        Map map = mapper.readValue(inputStream, Map.class);
        List matches = (ArrayList) map.get("matches");
        List<RemoteRuleMatch> result = new ArrayList<>();
        for (Object match : matches) {
            RemoteRuleMatch remoteMatch = getMatch((Map<String, Object>)match);
            result.add(remoteMatch);
        }
        return result;
    }

    private RemoteRuleMatch getMatch(Map<String, Object> match) {
        Map<String, Object> rule = (Map<String, Object>) match.get("rule");
        int offset = (int) getRequired(match, "offset");
        int errorLength = (int) getRequired(match, "length");

        Map<String, Object> context = (Map<String, Object>) match.get("context");
        int contextOffset = (int) getRequired(context, "offset");
        int contextForSureMatch = match.get("contextForSureMatch") != null ? (int) match.get("contextForSureMatch") : 0;
        RemoteRuleMatch remoteMatch = new RemoteRuleMatch(getRequiredString(rule, "id"), getRequiredString(match, "message"),
                getRequiredString(context, "text"), contextOffset, offset, errorLength, contextForSureMatch);
        remoteMatch.setShortMsg(getOrNull(match, "shortMessage"));
        remoteMatch.setRuleSubId(getOrNull(rule, "subId"));
        remoteMatch.setLocQualityIssueType(getOrNull(rule, "issueType"));
        List<String> urls = getValueList(rule, "urls");
        if (urls.size() > 0) {
            remoteMatch.setUrl(urls.get(0));
        }
        Map<String, Object> category = (Map<String, Object>) rule.get("category");
        remoteMatch.setCategory(getOrNull(category, "name"));
        remoteMatch.setCategoryId(getOrNull(category, "id"));

        remoteMatch.setReplacements(getValueList(match, "replacements"));
        return remoteMatch;
    }

    private Object getRequired(Map<String, Object> elem, String propertyName) {
        Object val = elem.get(propertyName);
        if (val != null) {
            return val;
        }
        throw new RuntimeException("JSON item " + elem + " doesn't contain required property '" + propertyName + "'");
    }

    private String getRequiredString(Map<String, Object> elem, String propertyName) {
        return (String) getRequired(elem, propertyName);
    }

    private String getOrNull(Map<String, Object> elem, String propertyName) {
        Object val = elem.get(propertyName);
        if (val != null) {
            return (String) val;
        }
        return null;
    }

    private List<String> getValueList(Map<String, Object> match, String propertyName) {
        List<Object> matches = (List<Object>) match.get(propertyName);
        List<String> l = new ArrayList<>();
        if (matches != null) {
            for (Object o : matches) {
                Map<String, Object> item = (Map<String, Object>) o;
                l.add((String) item.get("value"));
            }
        }
        return l;
    }
}
