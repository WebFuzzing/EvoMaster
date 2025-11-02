/**
 * To authenticate a user, would need specific settings, like
 * specific values in the HTTP headers (eg, cookies)
 */
import HeaderDto from "./HeaderDto";
import {CookieLoginDto} from "./CookieLoginDto";
import JsonTokenPostLoginDto from "./JsonTokenPostLoginDto";

export default class AuthenticationDto {

    /**
     * The name given to this authentication info.
     * Just needed for display/debugging reasons
     */
    public name: string;

    /**
     * The headers needed for authentication
     */
    public headers = new Array<HeaderDto>();

    /**
     * If the login is based on cookies, need to provide info on
     * how to get such a cookie
     */
    public cookieLogin: CookieLoginDto;

    public jsonTokenPostLogin: JsonTokenPostLoginDto;

}
