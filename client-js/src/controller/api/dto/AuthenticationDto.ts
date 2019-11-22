/**
 * To authenticate a user, would need specific settings, like
 * specific values in the HTTP headers (eg, cookies)
 */
export default class AuthenticationDto {

    /**
     * The name given to this authentication info.
     * Just needed for display/debugging reasons
     */
    name: string;

    /**
     * The headers needed for authentication
     */
    headers = new Array<HeaderDto>();

    /**
     * If the login is based on cookies, need to provide info on
     * how to get such a cookie
     */
    cookieLogin: CookieLoginDto;


}
