enum ContentType {
    JSON= "JSON", X_WWW_FORM_URLENCODED = "X_WWW_FORM_URLENCODED"
}

enum HttpVerb {
    GET = "GET", POST = "POST"
}

/**
 * Information on how to do a login based on username/password,
 * from which we then get a cookie back
 *
 * Created by arcuri82 on 24-Oct-19.
 */
export class CookieLoginDto {

    /**
     * The id of the user
     */
    public username: string;

    /**
     * The password of the user.
     * This must NOT be hashed.
     */
    public password: string;

    /**
     * The name of the field in the body payload containing the username
     */
    public usernameField: string;

    /**
     * The name of the field in the body payload containing the password
     */
    public passwordField: string;

    /**
     * The URL of the endpoint, e.g., "/login"
     */
    public loginEndpointUrl: string;

    /**
     * The HTTP verb used to send the data.
     * Usually a "POST".
     */
    public httpVerb: HttpVerb;

    /**
     * The encoding type used to specify how the data is sent
     */
    public contentType: ContentType;
}
