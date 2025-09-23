package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Common parameters shared across multiple OpenSearch query operations.
 */
public class CommonQueryParameters {
    private final Float boost;
    private final String name;
    private final String rewrite;
    private final Boolean caseInsensitive;

    public CommonQueryParameters(Float boost, String name, String rewrite, Boolean caseInsensitive) {
        this.boost = boost;
        this.name = name;
        this.rewrite = rewrite;
        this.caseInsensitive = caseInsensitive;
    }

    public static CommonQueryParameters empty() {
        return new CommonQueryParameters(null, null, null, null);
    }

    public static CommonQueryParameters withBoost(Float boost) {
        return new CommonQueryParameters(boost, null, null, null);
    }

    public static CommonQueryParameters withCaseInsensitive(Boolean caseInsensitive) {
        return new CommonQueryParameters(null, null, null, caseInsensitive);
    }

    public Float getBoost() {
        return boost;
    }

    public String getName() {
        return name;
    }

    public String getRewrite() {
        return rewrite;
    }

    public Boolean getCaseInsensitive() {
        return caseInsensitive;
    }

    public static class Builder {
        private Float boost;
        private String name;
        private String rewrite;
        private Boolean caseInsensitive;

        public Builder boost(Float boost) {
            this.boost = boost;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder rewrite(String rewrite) {
            this.rewrite = rewrite;
            return this;
        }

        public Builder caseInsensitive(Boolean caseInsensitive) {
            this.caseInsensitive = caseInsensitive;
            return this;
        }

        public CommonQueryParameters build() {
            return new CommonQueryParameters(boost, name, rewrite, caseInsensitive);
        }
    }
}
