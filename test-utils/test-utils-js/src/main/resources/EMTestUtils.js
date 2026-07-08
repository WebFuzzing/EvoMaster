/**
 * Note: this code needs to be kept in sync among the different programming
 * languages, eg, Java, JavaScript and Python.
 */

const URI = require("urijs");

module.exports = class EMTestUtils {


    /**
     * Loaded only once at module loading.
     * Seed is still going to incremented with ++ at each use.
     * The idea is to force each value unique during a session, even when generating hundreds of thousands of tests.
     * However, when running again in generated test suite, a new starting seed might reduce chances of clashes,
     * albeit cannot guarantee removal of them
     */
    static seed = Date.now();

    /**
     *
     * @param {number|null} minLength - Optional minimum length of the generated string
     * @param {number|null} maxLength - Optional maximum length of the generated string
     * @param {string|null} prefix - Optional fixed prefix shared by all generated strings
     * @param {string|null} postfix - Optional fixed postfix shared by all generated strings
     * @returns {string}
     */
    static createString(minLength = null, maxLength = null, prefix = null, postfix = null) {

        if (minLength !== null && minLength < 0) {
            throw new Error(`Negative minimum length: ${minLength}`);
        }
        if (maxLength !== null && maxLength < 0) {
            throw new Error(`Negative maximum length: ${maxLength}`);
        }

        let min = 0;
        if (minLength !== null) {
            min = minLength;
        }

        let len = 0;
        if (prefix !== null) {
            len += prefix.length;
        }
        if (postfix !== null) {
            len += postfix.length;
        }
        min = Math.max(min, len);

        // Actual check on inputs
        if (maxLength !== null && maxLength < len) {
            throw new Error(
                `Maximum length ${maxLength} does not cover minimum prefix+postfix length: ${prefix}${postfix}`
            );
        }

        // Recompute with default values if not specified
        if (prefix === null) {
            prefix = "u";
        }
        if (postfix === null) {
            postfix = "";
        }
        len = prefix.length + postfix.length;

        let maxDigits = 6; // 999 999 values
        if (maxDigits + len < min) {
            maxDigits = min - len;
        }
        if (maxLength !== null && maxDigits + len > maxLength) {
            maxDigits = maxLength - len;
        }

        let mask = 1;
        for (let i = 0; i < maxDigits; i++) {
            mask = mask * 10;
        }

        let value = EMTestUtils.seed % mask;
        EMTestUtils.seed++;

        return `${prefix}${value}${postfix}`;
    }

    /**
     *
     * @param locationHeader a URI-reference, coming from a "location" header. See RFC 7231.
     *                       Note: it can be a relative reference
     * @param expectedTemplate a full URI of the target resource, but with some path elements
     *                         that might (or might not) be unresolved. If {@code locationHeader} is not
     *                         empty, it will replace the beginning of this template.
     * @return a fully resolved URI for the target resource. If there are problems, just
     *          return the input locationHeader. If this latter is empty/null, then return the template
     */
    static resolveLocation(locationHeader /* string */, expectedTemplate /* string */) /*: string */ {

        if (!locationHeader) {
            return expectedTemplate;
        }

        let locationURI;
        try {
            locationURI = new URI(locationHeader);
            //FIXME this never throws an exception
        } catch (e){
            return locationHeader;
        }

        const locationPath = locationURI.pathname();
        const locationTokens = locationPath.split("/");

        //the template is not a valid URL, due to {}
        const normalizedTemplate = expectedTemplate.replace("{", "").replace("}", "");
        const templateURI = new URI(normalizedTemplate);
        const templatePath = templateURI.pathname();
        const templateTokens = templatePath.split("/");

        let targetPath = locationPath;

        if (templateTokens.length > locationTokens.length) {
            /*
                This is to handle cases like:

                POST /elements
                PUT  /elements/{id}/x

                where the location header of POST does point to

                /elements/{id}

                and not directly to "x"
             */

            for (let i = locationTokens.length; i < templateTokens.length; i++) {
                targetPath += "/" + templateTokens[i];
            }
        }

        let targetURI;

        try {
            //TODO how to check for locationURI.isAbsolute() ???
            if (locationURI.hostname()) {
                targetURI =  locationURI;
                targetURI.pathname( targetPath);
            } else {
                targetURI = templateURI;
                targetURI.pathname(targetPath);
            }
        } catch (e) {
            //shouldn't really happen
            throw e;
        }

        return targetURI.toString();
    }


    /**
     * @param uri a string representing a URI
     * @return whether the given input string is either empty or a valid URI
     *
     * FIXME this currently always returns true...
     */
    static isValidURIorEmpty(uri /* string */) /*: boolean */{

        if (!uri) {
            return true;
        }

        try {
            new URI(uri);
            /*
                FIXME: this does not work... the library just ignores malformed URIs...
             */
            return true;
        } catch (e) {
            return false;
        }
    }
}