/**
 * Note: this code needs to be kept in sync among the different programming
 * languages, eg, Java, JavaScript and Python.
 */

const URI = require("urijs");

module.exports = class EMTestUtils {

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