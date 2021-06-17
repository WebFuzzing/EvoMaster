/**
 * Note: this code needs to be kept in sync among the different programming
 * languages, eg, Java, JavaScript and C#.
 */

export default class EMTestUtils {

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
    public static resolveLocation(locationHeader: string, expectedTemplate: string): string {

        if (!locationHeader) {
            return expectedTemplate;
        }

        let locationURI: URL;
        try {
            locationURI = new URL(locationHeader);
        } catch (e){
            return locationHeader;
        }

        const locationPath = locationURI.pathname;
        const locationTokens = locationPath.split("/");

        //the template is not a valid URL, due to {}
        const normalizedTemplate = expectedTemplate.replace("{", "").replace("}", "");
        const templateURI = new URL(normalizedTemplate);
        const templatePath = templateURI.pathname;
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

        let targetURI: URL;

        try {
            //TODO how to check for locationURI.isAbsolute() ???
            if (locationURI.hostname) {
                targetURI =  locationURI;
                targetURI.pathname = targetPath;
            } else {
                targetURI = templateURI;
                targetURI.pathname = targetPath;
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
     */
    public static isValidURIorEmpty(uri: string): boolean {

        if (!uri) {
            return true;
        }

        try {
            new URL(uri);
            return true;
        } catch (e) {
            return false;
        }
    }
}