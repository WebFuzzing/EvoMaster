namespace Controller.Api {
  /**
   * Information on how to do a login based on username/password,
   * from which we then get a cookie back
   *
   * Created by arcuri82 on 24-Oct-19.
   */
  public class CookieLoginDto {

    /**
     * The id of the user
     */
    public string Username { get; set; }

    /**
     * The password of the user.
     * This must NOT be hashed.
     */
    public string Password { get; set; }

    /**
     * The name of the field in the body payload containing the username
     */
    public string UsernameField { get; set; }

    /**
     * The name of the field in the body payload containing the password
     */
    public string PasswordField { get; set; }

    /**
     * The URL of the endpoint, e.g., "/login"
     */
    public string LoginEndpointUrl { get; set; }

    /**
     * The HTTP verb used to send the data.
     * Usually a "POST".
     */
    public HttpVerb HttpVerb { get; set; }

    /**
     * The encoding type used to specify how the data is sent
     */
    public ContentType ContentType { get; set; }
  }
}