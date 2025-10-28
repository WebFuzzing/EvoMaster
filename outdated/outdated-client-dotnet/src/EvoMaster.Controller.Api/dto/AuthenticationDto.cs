using System.Collections.Generic;

namespace EvoMaster.Controller.Api {
    /**
   * To authenticate a user, would need specific settings, like
   * specific values in the HTTP headers (eg, cookies)
   */
    public class AuthenticationDto {
        public AuthenticationDto() { }

        public AuthenticationDto(string name) {
            this.Name = name;
        }

        /**
     * The name given to this authentication info.
     * Just needed for display/debugging reasons
     */
        public string Name { get; set; }

        /**
     * The headers needed for authentication
     */
        public IList<HeaderDto> Headers { get; set; } = new List<HeaderDto>();

        /**
     * If the login is based on cookies, need to provide info on
     * how to get such a cookie
     */
        public CookieLoginDto CookieLogin { get; set; }
    }
}