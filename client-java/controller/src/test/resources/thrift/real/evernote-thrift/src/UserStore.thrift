/*
 * Copyright 2007-2018 Evernote Corporation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This file contains the EDAM protocol interface for operations to query
 * and/or authenticate users.
 */

include "Types.thrift"
include "Errors.thrift"

namespace as3 com.evernote.edam.userstore
namespace java com.evernote.edam.userstore
namespace csharp Evernote.EDAM.UserStore
namespace py evernote.edam.userstore
namespace cpp evernote.edam
namespace rb Evernote.EDAM.UserStore
namespace php EDAM.UserStore
namespace cocoa EDAM
namespace perl EDAMUserStore
namespace go edam


/**
 * The major version number for the current revision of the EDAM protocol.
 * Clients pass this to the service using UserStore.checkVersion at the
 * beginning of a session to confirm that they are not out of date.
 */
const i16 EDAM_VERSION_MAJOR = 1

/**
 * The minor version number for the current revision of the EDAM protocol.
 * Clients pass this to the service using UserStore.checkVersion at the
 * beginning of a session to confirm that they are not out of date.
 */
const i16 EDAM_VERSION_MINOR = 28

//============================= Enumerations ==================================

/**
 * This structure is used to provide publicly-available user information
 * about a particular account.
 *<dl>
 * <dt>userId:</dt>
 *   <dd>
 *   The unique numeric user identifier for the user account.
 *   </dd>
 * <dt>serviceLevel:</dt>
 *   <dd>
 *   The service level of the account.
 *   </dd>
 * <dt>noteStoreUrl:</dt>
 *   <dd>
 *   This field will contain the full URL that clients should use to make
 *   NoteStore requests to the server shard that contains that user's data.
 *   I.e. this is the URL that should be used to create the Thrift HTTP client
 *   transport to send messages to the NoteStore service for the account.
 *   </dd>
 * <dt>webApiUrlPrefix:</dt>
 *   <dd>
 *   This field will contain the initial part of the URLs that should be used
 *   to make requests to Evernote's thin client "web API", which provide
 *   optimized operations for clients that aren't capable of manipulating
 *   the full contents of accounts via the full Thrift data model. Clients
 *   should concatenate the relative path for the various servlets onto the
 *   end of this string to construct the full URL, as documented on our
 *   developer web site.
 *   </dd>
 * </dl>
 */
struct PublicUserInfo {
  1:  required  Types.UserID userId,
  7:  optional  Types.ServiceLevel serviceLevel,
  4:  optional  string username,
  5:  optional  string noteStoreUrl,
  6:  optional  string webApiUrlPrefix
}

/**
 * <dl>
 * <dt>noteStoreUrl:</dt>
 *   <dd>
 *   This field will contain the full URL that clients should use to make
 *   NoteStore requests to the server shard that contains that user's data.
 *   I.e. this is the URL that should be used to create the Thrift HTTP client
 *   transport to send messages to the NoteStore service for the account.
 *   </dd>
 * <dt>webApiUrlPrefix:</dt>
 *   <dd>
 *   This field will contain the initial part of the URLs that should be used
 *   to make requests to Evernote's thin client "web API", which provide
 *   optimized operations for clients that aren't capable of manipulating
 *   the full contents of accounts via the full Thrift data model. Clients
 *   should concatenate the relative path for the various servlets onto the
 *   end of this string to construct the full URL, as documented on our
 *   developer web site.
 *   </dd>
 * <dt>userStoreUrl:</dt>
 *   <dd>
 *   This field will contain the full URL that clients should use to make UserStore
 *   requests after successfully authenticating. I.e. this is the URL that should be used
 *   to create the Thrift HTTP client transport to send messages to the UserStore service
 *   for this account.
 *   </dd>
 * <dt>utilityUrl:</dt>
 *   <dd>
 *   This field will contain the full URL that clients should use to make Utility requests
 *   to the server shard that contains that user's data. I.e. this is the URL that should
 *   be used to create the Thrift HTTP client transport to send messages to the Utility
 *   service for the account.
 *   </dd>
 * <dt>messageStoreUrl:</dt>
 *   <dd>
 *   This field will contain the full URL that clients should use to make MessageStore
 *   requests to the server. I.e. this is the URL that should be used to create the
 *   Thrift HTTP client transport to send messages to the MessageStore service for the
 *   account.
 *   </dd>
 * <dt>userWebSocketUrl:</dt>
 *   <dd>
 *   This field will contain the full URL that clients should use when opening a
 *   persistent web socket to recieve notification of events for the authenticated user.
 *   </dd>
 * </dl>
 */
struct UserUrls {
  1: optional string noteStoreUrl,
  2: optional string webApiUrlPrefix,
  3: optional string userStoreUrl,
  4: optional string utilityUrl,
  5: optional string messageStoreUrl,
  6: optional string userWebSocketUrl
}

/**
 * When an authentication (or re-authentication) is performed, this structure
 * provides the result to the client.
 *<dl>
 * <dt>currentTime:</dt>
 *   <dd>
 *   The server-side date and time when this result was
 *   generated.
 *   </dd>
 * <dt>authenticationToken:</dt>
 *   <dd>
 *   Holds an opaque, ASCII-encoded token that can be
 *   used by the client to perform actions on a NoteStore.
 *   </dd>
 * <dt>expiration:</dt>
 *   <dd>
 *   Holds the server-side date and time when the
 *   authentication token will expire.
 *   This time can be compared to "currentTime" to produce an expiration
 *   time that can be reconciled with the client's local clock.
 *   </dd>
 * <dt>user:</dt>
 *   <dd>
 *   Holds the information about the account which was
 *   authenticated if this was a full authentication.  May be absent if this
 *   particular authentication did not require user information.
 *   </dd>
 * <dt>publicUserInfo:</dt>
 *   <dd>
 *   If this authentication result was achieved without full permissions to
 *   access the full User structure, this field may be set to give back
 *   a more limited public set of data.
 *   </dd>
 * <dt>noteStoreUrl:</dt>
 *   <dd>
 *   DEPRECATED - Client applications should use urls.noteStoreUrl.
 *   </dd>
 * <dt>webApiUrlPrefix:</dt>
 *   <dd>
 *   DEPRECATED - Client applications should use urls.webApiUrlPrefix.
 *   </dd>
 * <dt>secondFactorRequired:</dt>
 *   <dd>
 *   If set to true, this field indicates that the user has enabled two-factor
 *   authentication and must enter their second factor in order to complete
 *   authentication. In this case the value of authenticationResult will be
 *   a short-lived authentication token that may only be used to make a
 *   subsequent call to completeTwoFactorAuthentication.
 *   </dd>
 * <dt>secondFactorDeliveryHint:</dt>
 *   <dd>
 *   When secondFactorRequired is set to true, this field may contain a string
 *   describing the second factor delivery method that the user has configured.
 *   This will typically be an obfuscated mobile device number, such as
 *   "(xxx) xxx-x095". This string can be displayed to the user to remind them
 *   how to obtain the required second factor.
 *   </dd>
 * <dt>urls</dt>
 *   <dd>
 *   This structure will contain all of the URLs that clients need to make requests to the
 *   Evernote service on behalf of the authenticated User.
 *   </dd>
 * </dl>
 */
struct AuthenticationResult {
  1:  required Types.Timestamp currentTime,
  2:  required string authenticationToken,
  3:  required Types.Timestamp expiration,
  4:  optional Types.User user,
  5:  optional PublicUserInfo publicUserInfo,
  6:  optional string noteStoreUrl,
  7:  optional string webApiUrlPrefix,
  8:  optional bool secondFactorRequired,
  9:  optional string secondFactorDeliveryHint,
  10: optional UserUrls urls
}

/**
 * This structure describes a collection of bootstrap settings.
 *<dl>
 * <dt>serviceHost:</dt>
 *   <dd>
 *   The hostname and optional port for composing Evernote web service URLs.
 *   This URL can be used to access the UserStore and related services,
 *   but must not be used to compose the NoteStore URL. Client applications
 *   must handle serviceHost values that include only the hostname
 *   (e.g. www.evernote.com) or both the hostname and port (e.g. www.evernote.com:8080).
 *   If no port is specified, or if port 443 is specified, client applications must
 *   use the scheme "https" when composing URLs. Otherwise, a client must use the
 *   scheme "http".
 * </dd>
 * <dt>marketingUrl:</dt>
 *   <dd>
 *   The URL stem for the Evernote corporate marketing website, e.g. http://www.evernote.com.
 *   This stem can be used to compose website URLs. For example, the URL of the Evernote
 *   Trunk is composed by appending "/about/trunk/" to the value of marketingUrl.
 *   </dd>
 * <dt>supportUrl:</dt>
 *   <dd>
 *   The full URL for the Evernote customer support website, e.g. https://support.evernote.com.
 *   </dd>
 * <dt>accountEmailDomain:</dt>
 *   <dd>
 *   The domain used for an Evernote user's incoming email address, which allows notes to
 *   be emailed into an account. E.g. m.evernote.com.
 *   </dd>
 * <dt>enableFacebookSharing:</dt>
 *   <dd>
 *   Whether the client application should enable sharing of notes on Facebook.
 *   </dd>
 * <dt>enableGiftSubscriptions:</dt>
 *   <dd>
 *   Whether the client application should enable gift subscriptions.
 *   </dd>
 * <dt>enableSupportTickets:</dt>
 *   <dd>
 *   Whether the client application should enable in-client creation of support tickets.
 *   </dd>
 * <dt>enableSharedNotebooks:</dt>
 *   <dd>
 *   Whether the client application should enable shared notebooks.
 *   </dd>
 * <dt>enableSingleNoteSharing:</dt>
 *   <dd>
 *   Whether the client application should enable single note sharing.
 *   </dd>
 * <dt>enableSponsoredAccounts:</dt>
 *   <dd>
 *   Whether the client application should enable sponsored accounts.
 *   </dd>
 * <dt>enableTwitterSharing:</dt>
 *   <dd>
 *   Whether the client application should enable sharing of notes on Twitter.
 *   </dd>
 * <dt>enableGoogle:</dt>
 *   <dd>
 *   Whether the client application should enable authentication with Google,
 *   for example to allow integration with a user's Gmail contacts.
 * </dl>
 */
struct BootstrapSettings {
  1: required string serviceHost,
  2: required string marketingUrl,
  3: required string supportUrl,
  4: required string accountEmailDomain,
  5: optional bool enableFacebookSharing,
  6: optional bool enableGiftSubscriptions,
  7: optional bool enableSupportTickets,
  8: optional bool enableSharedNotebooks,
  9: optional bool enableSingleNoteSharing,
  10: optional bool enableSponsoredAccounts,
  11: optional bool enableTwitterSharing,
  12: optional bool enableLinkedInSharing,
  13: optional bool enablePublicNotebooks,
  16: optional bool enableGoogle
}

/**
 * This structure describes a collection of bootstrap settings.
 *<dl>
 * <dt>name:</dt>
 *   <dd>
 *   The unique name of the profile, which is guaranteed to remain consistent across
 *   calls to getBootstrapInfo.
 *   </dd>
 * <dt>settings:</dt>
 *   <dd>
 *   The settings for this profile.
 *   </dd>
 * </dl>
 */
struct BootstrapProfile {
  1: required string name,
  2: required BootstrapSettings settings,
}

/**
 * This structure describes a collection of bootstrap profiles.
 *<dl>
 * <dt>profiles:</dt>
 *   <dd>
 *   List of one or more bootstrap profiles, in descending
 *   preference order.
 *   </dd>
 * </dl>
 */
struct BootstrapInfo {
  1: required list<BootstrapProfile> profiles
}

/**
 * Service:  UserStore
 * <p>
 * The UserStore service is primarily used by EDAM clients to establish
 * authentication via username and password over a trusted connection (e.g.
 * SSL).  A client's first call to this interface should be checkVersion() to
 * ensure that the client's software is up to date.
 * </p>
 * All calls which require an authenticationToken may throw an
 * EDAMUserException for the following reasons:
 *  <ul>
 *   <li> AUTH_EXPIRED "authenticationToken" - token has expired
 *   <li> BAD_DATA_FORMAT "authenticationToken" - token is malformed
 *   <li> DATA_REQUIRED "authenticationToken" - token is empty
 *   <li> INVALID_AUTH "authenticationToken" - token signature is invalid
 *   <li> PERMISSION_DENIED "authenticationToken" - token does not convey sufficient
 *     privileges
 * </ul>
 */
service UserStore {

  /**
   * This should be the first call made by a client to the EDAM service.  It
   * tells the service what protocol version is used by the client.  The
   * service will then return true if the client is capable of talking to
   * the service, and false if the client's protocol version is incompatible
   * with the service, so the client must upgrade.  If a client receives a
   * false value, it should report the incompatibility to the user and not
   * continue with any more EDAM requests (UserStore or NoteStore).
   *
   * @param clientName
   *   This string provides some information about the client for
   *   tracking/logging on the service.  It should provide information about
   *   the client's software and platform. The structure should be:
   *   application/version; platform/version; [ device/version ]
   *   E.g. "Evernote Windows/3.0.1; Windows/XP SP3".
   *
   * @param edamVersionMajor
   *   This should be the major protocol version that was compiled by the
   *   client.  This should be the current value of the EDAM_VERSION_MAJOR
   *   constant for the client.
   *
   * @param edamVersionMinor
   *   This should be the major protocol version that was compiled by the
   *   client.  This should be the current value of the EDAM_VERSION_MINOR
   *   constant for the client.
   */
  bool checkVersion(1: string clientName,
                    2: i16 edamVersionMajor = EDAM_VERSION_MAJOR,
                    3: i16 edamVersionMinor = EDAM_VERSION_MINOR),

  /**
   * This provides bootstrap information to the client. Various bootstrap
   * profiles and settings may be used by the client to configure itself.
   *
   * @param locale
   *   The client's current locale, expressed in language[_country]
   *   format. E.g., "en_US". See ISO-639 and ISO-3166 for valid
   *   language and country codes.
   *
   * @return
   *   The bootstrap information suitable for this client.
   */
  BootstrapInfo getBootstrapInfo(1: string locale),

  /**
   * This is used to check a username and password in order to create a
   * long-lived authentication token that can be used for further actions.
   *
   * This function is not available to most third party applications,
   * which typically authenticate using OAuth as
   * described at
   * <a href="http://dev.evernote.com/documentation/cloud/">dev.evernote.com</a>.
   * If you believe that your application requires permission to authenticate
   * using username and password instead of OAuth, please contact Evernote
   * developer support by visiting
   * <a href="http://dev.evernote.com">dev.evernote.com</a>.
   *
   * @param username
   *   The username or registered email address of the account to
   *   authenticate against.
   *
   * @param password
   *   The plaintext password to check against the account.  Since
   *   this is not protected by the EDAM protocol, this information must be
   *   provided over a protected transport (i.e. SSL).
   *
   * @param consumerKey
   *   The "consumer key" portion of the API key issued to the client application
   *   by Evernote.
   *
   * @param consumerSecret
   *   The "consumer secret" portion of the API key issued to the client application
   *   by Evernote.
   *
   * @param deviceIdentifier
   *   An optional string that uniquely identifies the device from which the
   *   authentication is being performed. This string allows the service to return the
   *   same authentication token when a given application requests authentication
   *   repeatedly from the same device. This may happen when the user logs out of an
   *   application and then logs back in, or when the application is uninstalled
   *   and later reinstalled. If no reliable device identifier can be created,
   *   this value should be omitted. If set, the device identifier must be between
   *   1 and EDAM_DEVICE_ID_LEN_MAX characters long and must match the regular expression
   *   EDAM_DEVICE_ID_REGEX.
   *
   * @param deviceDescription
   *   A description of the device from which the authentication is being performed.
   *   This field is displayed to the user in a list of authorized applications to
   *   allow them to distinguish between multiple tokens issued to the same client
   *   application on different devices. For example, the Evernote iOS client on
   *   a user's iPhone and iPad might pass the iOS device names "Bob's iPhone" and
   *   "Bob's iPad". The device description must be between 1 and
   *   EDAM_DEVICE_DESCRIPTION_LEN_MAX characters long and must match the regular
   *   expression EDAM_DEVICE_DESCRIPTION_REGEX.
   *
   * @param supportsTwoFactor
   *   Whether the calling application supports two-factor authentication. If this
   *   parameter is false, this method will fail with the error code INVALID_AUTH and the
   *   parameter "password" when called for a user who has enabled two-factor
   *   authentication.
   *
   * @return
   *   <p>The result of the authentication. The level of detail provided in the returned
   *   AuthenticationResult.User structure depends on the access level granted by
   *   calling application's API key.</p>
   *   <p>If the user has two-factor authentication enabled,
   *   AuthenticationResult.secondFactorRequired will be set and
   *   AuthenticationResult.authenticationToken will contain a short-lived token
   *   that may only be used to complete the two-factor authentication process by calling
   *   UserStore.completeTwoFactorAuthentication.</p>
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "username" - username is empty
   *   <li> DATA_REQUIRED "password" - password is empty
   *   <li> DATA_REQUIRED "consumerKey" - consumerKey is empty
   *   <li> DATA_REQUIRED "consumerSecret" - consumerSecret is empty
   *   <li> DATA_REQUIRED "deviceDescription" - deviceDescription is empty
   *   <li> BAD_DATA_FORMAT "deviceDescription" - deviceDescription is not valid.
   *   <li> BAD_DATA_FORMAT "deviceIdentifier" - deviceIdentifier is not valid.
   *   <li> INVALID_AUTH "username" - username not found
   *   <li> INVALID_AUTH "password" - password did not match
   *   <li> INVALID_AUTH "consumerKey" - consumerKey is not authorized
   *   <li> INVALID_AUTH "consumerSecret" - consumerSecret is incorrect
   *   <li> INVALID_AUTH "businessOnly" - the user is a business-only account
   *   <li> PERMISSION_DENIED "User.active" - user account is closed
   *   <li> PERMISSION_DENIED "User.tooManyFailuresTryAgainLater" - user has
   *     failed authentication too often
   *   <li> AUTH_EXPIRED "password" - user password is expired
   * </ul>
   */
  AuthenticationResult authenticateLongSession(1: string username,
                                               2: string password,
                                               3: string consumerKey,
                                               4: string consumerSecret,
                                               5: string deviceIdentifier,
                                               6: string deviceDescription,
                                               7: bool supportsTwoFactor)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Complete the authentication process when a second factor is required. This
   * call is made after a successful call to authenticate or authenticateLongSession
   * when the authenticating user has enabled two-factor authentication.
   *
   * @param authenticationToken An authentication token returned by a previous
   *   call to UserStore.authenticate or UserStore.authenticateLongSession that
   *   could not be completed in a single call because a second factor was required.
   *
   * @param oneTimeCode The one time code entered by the user. This value is delivered
   *   out-of-band, typically via SMS or an authenticator application.
   *
   * @param deviceIdentifier See the corresponding parameter in authenticateLongSession.
   *
   * @param deviceDescription See the corresponding parameter in authenticateLongSession.
   *
   * @return
   *   The result of the authentication. The level of detail provided in the returned
   *   AuthenticationResult.User structure depends on the access level granted by the
   *   calling application's API key. If the initial authentication call was made to
   *   authenticateLongSession, the AuthenticationResult will contain a long-lived
   *   authentication token.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "authenticationToken" - authenticationToken is empty
   *   <li> DATA_REQUIRED "oneTimeCode" - oneTimeCode is empty
   *   <li> BAD_DATA_FORMAT "deviceIdentifier" - deviceIdentifier is not valid
   *   <li> BAD_DATA_FORMAT "authenticationToken" - authenticationToken is not well formed
   *   <li> INVALID_AUTH "oneTimeCode" - oneTimeCode did not match
   *   <li> AUTH_EXPIRED "authenticationToken" - authenticationToken has expired
   *   <li> PERMISSION_DENIED "authenticationToken" - authenticationToken is not valid
   *   <li> PERMISSION_DENIED "User.active" - user account is closed
   *   <li> PERMISSION_DENIED "User.tooManyFailuresTryAgainLater" - user has
   *     failed authentication too often
   *   <li> DATA_CONFLICT "User.twoFactorAuthentication" - The user has not enabled
   *      two-factor authentication.</li>
   * </ul>
   */
  AuthenticationResult completeTwoFactorAuthentication(1: string authenticationToken,
                                                       2: string oneTimeCode,
                                                       3: string deviceIdentifier,
                                                       4: string deviceDescription)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Revoke an existing long lived authentication token. This can be used to
   * revoke OAuth tokens or tokens created by calling authenticateLongSession,
   * and allows a user to effectively log out of Evernote from the perspective
   * of the application that holds the token. The authentication token that is
   * passed is immediately revoked and may not be used to call any authenticated
   * EDAM function.
   *
   * @param authenticationToken the authentication token to revoke.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "authenticationToken" - no authentication token provided
   *   <li> BAD_DATA_FORMAT "authenticationToken" - the authentication token is not well formed
   *   <li> INVALID_AUTH "authenticationToken" - the authentication token is invalid
   *   <li> AUTH_EXPIRED "authenticationToken" - the authentication token is expired or
   *     is already revoked.
   * </ul>
   */
  void revokeLongSession(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * This is used to take an existing authentication token that grants access
   * to an individual user account (returned from 'authenticate',
   * 'authenticateLongSession' or an OAuth authorization) and obtain an additional
   * authentication token that may be used to access business notebooks if the user
   * is a member of an Evernote Business account.
   *
   * The resulting authentication token may be used to make NoteStore API calls
   * against the business using the NoteStore URL returned in the result.
   *
   * @param authenticationToken
   *   The authentication token for the user. This may not be a shared authentication
   *   token (returned by NoteStore.authenticateToSharedNotebook or
   *   NoteStore.authenticateToSharedNote) or a business authentication token.
   *
   * @return
   *   The result of the authentication, with the token granting access to the
   *   business in the result's 'authenticationToken' field. The URL that must
   *   be used to access the business account NoteStore will be returned in the
   *   result's 'noteStoreUrl' field.  The 'User' field will
   *   not be set in the result.
   *
   * @throws EDAMUserException <ul>
   *   <li> PERMISSION_DENIED "authenticationToken" - the provided authentication token
   *        is a shared or business authentication token. </li>
   *   <li> PERMISSION_DENIED "Business" - the user identified by the provided
   *        authentication token is not currently a member of a business. </li>
   *   <li> PERMISSION_DENIED "Business.status" - the business that the user is a
   *        member of is not currently in an active status. </li>
   *   <li> BUSINESS_SECURITY_LOGIN_REQUIRED "sso" - the user must complete single
   *        sign-on before authenticating to the business.
   * </ul>
   */
  AuthenticationResult authenticateToBusiness(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Returns the User corresponding to the provided authentication token,
   * or throws an exception if this token is not valid.
   * The level of detail provided in the returned User structure depends on
   * the access level granted by the token, so a web service client may receive
   * fewer fields than an integrated desktop client.
   */
  Types.User getUser(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Asks the UserStore about the publicly available location information for
   * a particular username.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "username" - username is empty
   * </ul>
   */
  PublicUserInfo getPublicUserInfo(1: string username)
    throws (1: Errors.EDAMNotFoundException notFoundException,
    	    2: Errors.EDAMSystemException systemException,
    	    3: Errors.EDAMUserException userException),

  /**
   * <p>Returns the URLs that should be used when sending requests to the service on
   * behalf of the account represented by the provided authenticationToken.</p>
   *
   * <p>This method isn't needed by most clients, who can retreive the correct set of
   * UserUrls from the AuthenticationResult returned from
   * UserStore#authenticateLongSession(). This method is typically only needed to look up
   * the correct URLs for an existing long-lived authentication token.</p>
   */

  UserUrls getUserUrls(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Invite a user to join an Evernote Business account.
   *
   * Behavior will depend on the auth token. <ol>
   *   <li>
   *     auth token with privileges to manage Evernote Business membership.
   *       "External Provisioning" - The user will receive an email inviting
   *       them to join the business. They do not need to have an existing Evernote
   *       account. If the user has already been invited, a new invitation email
   *       will be sent.
   *   </li>
   *   <li>
   *     business auth token issued to an admin user. Only for first-party clients:
   *       "Approve Invitation" - If there has been a request to invite the email,
   *       approve it. Invited user will receive email with a link to join business.
   *       "Invite User" - If no invitation for the email exists, create an approved
   *       invitation for the email. An email will be sent to the emailAddress with
   *       a link to join the caller's business.
   *   </li>
   *   </li>
   *     business auth token:
   *       "Request Invitation" - If no invitation exists, create a request to
   *       invite the user to the business. These requests do not count towards a
   *       business' max active user limit.
   *   </li>
   * </ol>
   *
   * @param authenticationToken
   *   the authentication token with sufficient privileges to manage Evernote Business
   *   membership or a business auth token.
   *
   * @param emailAddress
   *   the email address of the user to invite to join the Evernote Business account.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "email" - if no email address was provided </li>
   *   <li> BAD_DATA_FORMAT "email" - if the email address is not well formed </li>
   *   <li> DATA_CONFLICT "BusinessUser.email" - if there is already a user in the business
   *     whose business email address matches the specified email address. </li>
   *   <li> LIMIT_REACHED "Business.maxActiveUsers" - if the business has reached its
   *     user limit. </li>
   * </ul>
   */
  void inviteToBusiness(1: string authenticationToken,
                        2: string emailAddress)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Remove a user from an Evernote Business account. Once removed, the user will no
   * longer be able to access content within the Evernote Business account.
   *
   * <p>The email address of the user to remove from the business must match the email
   * address used to invite a user to join the business via UserStore.inviteToBusiness.
   * This function will only remove users who were invited by external provisioning</p>
   *
   * @param authenticationToken
   *   An authentication token with sufficient privileges to manage Evernote Business
   *   membership.
   *
   * @param emailAddress
   *   The email address of the user to remove from the Evernote Business account.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "email" - if no email address was provided </li>
   *   <li> BAD_DATA_FORMAT "email" - The email address is not well formed </li>
   * </ul>
   * @throws EDAMNotFoundException <ul>
   *   <li> "email" - If there is no user with the specified email address in the
   *     business or that user was not invited via external provisioning. </li>
   * </ul>
   */
  void removeFromBusiness(1: string authenticationToken,
                          2: string emailAddress)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Update the email address used to uniquely identify an Evernote Business user.
   *
   * This will update the identifier for a user who was previously invited using
   * inviteToBusiness, ensuring that caller and the Evernote service maintain an
   * agreed-upon identifier for a specific user.
   *
   * For example, the following sequence of calls would invite a user to join
   * a business, update their email address, and then remove the user
   * from the business using the updated email address.
   *
   * inviteToBusiness("foo@bar.com")
   * updateBusinessUserIdentifier("foo@bar.com", "baz@bar.com")
   * removeFromBusiness("baz@bar.com")
   *
   * @param authenticationToken
   *   An authentication token with sufficient privileges to manage Evernote Business
   *   membership.
   *
   * @param oldEmailAddress
   *   The existing email address used to uniquely identify the user.
   *
   * @param newEmailAddress
   *   The new email address used to uniquely identify the user.
   *
   * @throws EDAMUserException <ul>
   *   <li>DATA_REQUIRED "oldEmailAddress" - No old email address was provided</li>
   *   <li>DATA_REQUIRED "newEmailAddress" - No new email address was provided</li>
   *   <li>BAD_DATA_FORMAT "oldEmailAddress" - The old email address is not well formed</li>
   *   <li>BAD_DATA_FORMAT "newEmailAddress" - The new email address is not well formed</li>
   *   <li>DATA_CONFLICT "oldEmailAddress" - The old and new email addresses were the same</li>
   *   <li>DATA_CONFLICT "newEmailAddress" - There is already an invitation or registered user with
   *     the provided new email address.</li>
   *   <li>DATA_CONFLICT "invitation.externallyProvisioned" - The user identified by
   *     oldEmailAddress was not added via UserStore.inviteToBusiness and therefore cannot be
   *     updated.</li>
   * </ul>
   * @throws EDAMNotFoundException <ul>
   *   <li>"oldEmailAddress" - If there is no user or invitation with the specified oldEmailAddress
   *     in the business.</li>
   * </ul>
   */
  void updateBusinessUserIdentifier(1: string authenticationToken,
                                    2: string oldEmailAddress,
                                    3: string newEmailAddress)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns a list of active business users in a given business.
   *
   * Clients are required to cache this information and re-fetch no more than once per day
   * or when they encountered a user ID or username that was not known to them.
   *
   * To avoid excessive look ups, clients should also track user IDs and usernames that belong
   * to users who are not in the business, since they will not be included in the result.
   *
   * I.e., when a client encounters a previously unknown user ID as a note's creator, it may query
   * listBusinessUsers to find information about this user. If the user is not in the resulting
   * list, the client should track that fact and not re-query the service the next time that it sees
   * this user on a note.
   *
   * @param authenticationToken
   *   A business authentication token returned by authenticateToBusiness or with sufficient
   *   privileges to manage Evernote Business membership.
   */
  list<Types.UserProfile> listBusinessUsers(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Returns a list of outstanding invitations to join an Evernote Business account.
   *
   * Only outstanding invitations are returned by this function. Users who have accepted an
   * invitation and joined a business are listed using listBusinessUsers.
   *
   * @param authenticationToken
   *   An authentication token with sufficient privileges to manage Evernote Business membership.
   *
   * @param includeRequestedInvitations
   *   If true, invitations with a status of BusinessInvitationStatus.REQUESTED will be included
   *   in the returned list. If false, only invitations with a status of
   *   BusinessInvitationStatus.APPROVED will be included.
   */
  list<Types.BusinessInvitation> listBusinessInvitations(1: string authenticationToken,
                                                         2: bool includeRequestedInvitations)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Retrieve the standard account limits for a given service level. This should only be
   * called when necessary, e.g. to determine if a higher level is available should the
   * user upgrade, and should be cached for long periods (e.g. 30 days) as the values are
   * not expected to fluctuate frequently.
   *
   * @throws EDAMUserException <ul>
   *   <li>DATA_REQUIRED "serviceLevel" - serviceLevel is null</li>
   * </ul>
   */
  Types.AccountLimits getAccountLimits(1: Types.ServiceLevel serviceLevel)
    throws (1: Errors.EDAMUserException userException),
}
