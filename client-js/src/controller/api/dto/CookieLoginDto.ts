enum ContentType {
    JSON, X_WWW_FORM_URLENCODED
}

enum HttpVerb {
    GET, POST
}


/**
 * Information on how to do a login based on username/password,
 * from which we then get a cookie back
 *
 * Created by arcuri82 on 24-Oct-19.
 */
class CookieLoginDto {


    /**
     * The id of the user
     */
    username: string;

    /**
     * The password of the user.
     * This must NOT be hashed.
     */
    password: string;

    /**
     * The name of the field in the body payload containing the username
     */
    usernameField: string;

    /**
     * The name of the field in the body payload containing the password
     */
    passwordField: string;

    /**
     * The URL of the endpoint, e.g., "/login"
     */
    loginEndpointUrl: string;


    /**
     * The HTTP verb used to send the data.
     * Usually a "POST".
     */
    httpVerb: HttpVerb;


    /**
     * The encoding type used to specify how the data is sent
     */
    contentType: ContentType;
}
