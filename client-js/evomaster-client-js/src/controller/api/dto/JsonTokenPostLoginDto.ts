/**
 * refer to JsonTokenPostLogin.kt
 */
export default class JsonTokenPostLoginDto {

    /**
     * The id representing this user that is going to login
     */
    public userId: string;

    /**
     * The endpoint where to execute the login
     */
    public endpoint: string;

    /**
     * The payload to send, as stringified JSON object
     */
    public jsonPayload: string;


    /**
     * How to extract the token from a JSON response, as such
     * JSON could have few fields, possibly nested.
     * It is expressed as a JSON Pointer
     */
    public extractTokenField: string;

    /**
     * When sending out the obtained token in the Authorization header,
     * specify if there should be any prefix (e.g., "Bearer " or "JWT ")
     */
    public headerPrefix: string;

}
