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
 * This file contains the definitions of the Evernote-related errors that
 * can occur when making calls to EDAM services. 
 */

include "Types.thrift"

namespace as3 com.evernote.edam.error
namespace java com.evernote.edam.error
namespace csharp Evernote.EDAM.Error
namespace py evernote.edam.error
namespace cpp evernote.edam
namespace rb Evernote.EDAM.Error
namespace php EDAM.Error
namespace perl EDAMErrors
namespace go edam

/**
 * Numeric codes indicating the type of error that occurred on the
 * service.
 * <dl>
 *   <dt>UNKNOWN</dt>
 *     <dd>No information available about the error</dd>
 *   <dt>BAD_DATA_FORMAT</dt>
 *     <dd>The format of the request data was incorrect</dd>
 *   <dt>PERMISSION_DENIED</dt>
 *     <dd>Not permitted to perform action</dd>
 *   <dt>INTERNAL_ERROR</dt>
 *     <dd>Unexpected problem with the service</dd>
 *   <dt>DATA_REQUIRED</dt>
 *     <dd>A required parameter/field was absent</dd>
 *   <dt>LIMIT_REACHED</dt>
 *     <dd>Operation denied due to data model limit</dd>
 *   <dt>QUOTA_REACHED</dt>
 *     <dd>Operation denied due to user storage limit</dd>
 *   <dt>INVALID_AUTH</dt>
 *     <dd>Username and/or password incorrect</dd>
 *   <dt>AUTH_EXPIRED</dt>
 *     <dd>Authentication token expired</dd>
 *   <dt>DATA_CONFLICT</dt>
 *     <dd>Change denied due to data model conflict</dd>
 *   <dt>ENML_VALIDATION</dt>
 *     <dd>Content of submitted note was malformed</dd>
 *   <dt>SHARD_UNAVAILABLE</dt>
 *     <dd>Service shard with account data is temporarily down</dd>
 *   <dt>LEN_TOO_SHORT</dt>
 *     <dd>Operation denied due to data model limit, where something such
 *         as a string length was too short</dd>
 *   <dt>LEN_TOO_LONG</dt>
 *     <dd>Operation denied due to data model limit, where something such
 *         as a string length was too long</dd>
 *   <dt>TOO_FEW</dt>
 *     <dd>Operation denied due to data model limit, where there were
 *         too few of something.</dd>
 *   <dt>TOO_MANY</dt>
 *     <dd>Operation denied due to data model limit, where there were
 *         too many of something.</dd>
 *   <dt>UNSUPPORTED_OPERATION</dt>
 *     <dd>Operation denied because it is currently unsupported.</dd>
 *   <dt>TAKEN_DOWN</dt>
 *     <dd>Operation denied because access to the corresponding object is
 *         prohibited in response to a take-down notice.</dd>
 *   <dt>RATE_LIMIT_REACHED</dt>
 *     <dd>Operation denied because the calling application has reached
 *         its hourly API call limit for this user.</dd>
 *   <dt>BUSINESS_SECURITY_LOGIN_REQUIRED</dt>
 *     <dd>Access to a business account has been denied because the user must complete
 *        additional steps in order to comply with business security requirements.</dd>
 *   <dt>DEVICE_LIMIT_REACHED</dt>
 *     <dd>Operation denied because the user has exceeded their maximum allowed
 *        number of devices.</dd>
 *   <dt>OPENID_ALREADY_TAKEN</dt>
 *     <dd>Operation failed because the Open ID is already associated with another user.</dd>
 *   <dt>INVALID_OPENID_TOKEN</dt>
 *     <dd>Operation denied because the Open ID token is invalid. Please re-issue a valid  
 *        token.</dd>
 *	 <dt>USER_NOT_REGISTERED</dt>
 *     <dd>There is no Evernote user associated with this OpenID account, 
 *     	   and no Evernote user with a matching email</dd>
 *	 <dt>USER_NOT_ASSOCIATED</dt>
 *     <dd>There is no Evernote user associated with this OpenID account, 
 *		   but Evernote user with matching email exists</dd>
 *	 <dt>USER_ALREADY_ASSOCIATED</dt>
 * 	   <dd>Evernote user is already associated with this provider 
 *		   using a different email address.</dd>
 *	 <dt>ACCOUNT_CLEAR</dt>
 *     <dd>The user's account has been disabled. Clients should deal with this errorCode
 *       by logging the user out and purging all locally saved content, including local
 *       edits not yet pushed to the server.</dd>
 *	 <dt>SSO_AUTHENTICATION_REQUIRED</dt>
  *     <dd>SSO authentication is the only tyoe of authentication allowed for the user's
  *     account. This error is thrown when the user attempts to authenticate by another
   *     method (password, OpenId, etc).</dd>
  * </dl>
 */
enum EDAMErrorCode {
  UNKNOWN = 1,
  BAD_DATA_FORMAT = 2,
  PERMISSION_DENIED = 3,
  INTERNAL_ERROR = 4,
  DATA_REQUIRED = 5,
  LIMIT_REACHED = 6,
  QUOTA_REACHED = 7,
  INVALID_AUTH = 8,
  AUTH_EXPIRED = 9,
  DATA_CONFLICT = 10,
  ENML_VALIDATION = 11,
  SHARD_UNAVAILABLE = 12,
  LEN_TOO_SHORT = 13,
  LEN_TOO_LONG = 14,
  TOO_FEW = 15,
  TOO_MANY = 16,
  UNSUPPORTED_OPERATION = 17,
  TAKEN_DOWN = 18,
  RATE_LIMIT_REACHED = 19,
  BUSINESS_SECURITY_LOGIN_REQUIRED = 20,
  DEVICE_LIMIT_REACHED = 21,
  OPENID_ALREADY_TAKEN = 22,
  INVALID_OPENID_TOKEN = 23,
  USER_NOT_ASSOCIATED = 24,
  USER_NOT_REGISTERED = 25,
  USER_ALREADY_ASSOCIATED = 26,
  ACCOUNT_CLEAR = 27,
  SSO_AUTHENTICATION_REQUIRED = 28
}


/**
 * An enumeration that provides a reason for why a given contact was invalid, for example,
 * as thrown via an EDAMInvalidContactsException.
 *
 * <dl>
 *   <dt>BAD_ADDRESS</dt>
 *     <dd>The contact information does not represent a valid address for a recipient.
 *         Clients should be validating and normalizing contacts, so receiving this
 *         error code commonly represents a client error. 
 *         </dd>
 *   <dt>DUPLICATE_CONTACT</dt>
 *     <dd>If the method throwing this exception accepts a list of contacts, this error
 *         code indicates that the given contact is a duplicate of another contact in
 *         the list.  Note that the server may clean up contacts, and that this cleanup
 *         occurs before checking for duplication.  Receiving this error is commonly
 *         an indication of a client issue, since client should be normalizing contacts
 *         and removing duplicates. All instances that are duplicates are returned.  For
 *         example, if a list of 5 contacts has the same e-mail address twice, the two
 *         conflicting e-mail address contacts will be returned.
 *         </dd>
 *   <dt>NO_CONNECTION</dt>
 *     <dd>Indicates that the given contact, an Evernote type contact, is not connected
 *         to the user for which the call is being made. It is possible that clients are
 *         out of sync with the server and should re-synchronize their identities and
 *         business user state. See Identity.userConnected for more information on user
 *         connections.
 *         </dd>
 * </dl>
 *
 * Note that if multiple reasons may apply, only one is returned. The precedence order
 * is BAD_ADDRESS, DUPLICATE_CONTACT, NO_CONNECTION, meaning that if a contact has a bad
 * address and is also duplicated, it will be returned as a BAD_ADDRESS.
 */
enum EDAMInvalidContactReason {
  BAD_ADDRESS,
  DUPLICATE_CONTACT,
  NO_CONNECTION
}


/**
 * This exception is thrown by EDAM procedures when a call fails as a result of 
 * a problem that a caller may be able to resolve.  For example, if the user
 * attempts to add a note to their account which would exceed their storage
 * quota, this type of exception may be thrown to indicate the source of the
 * error so that they can choose an alternate action.
 *
 * This exception would not be used for internal system errors that do not
 * reflect user actions, but rather reflect a problem within the service that
 * the user cannot resolve.
 *
 * errorCode:  The numeric code indicating the type of error that occurred.
 *   must be one of the values of EDAMErrorCode.
 *
 * parameter:  If the error applied to a particular input parameter, this will
 *   indicate which parameter. For some errors (USER_NOT_ASSOCIATED, USER_NOT_REGISTERED,
 *   SSO_AUTHENTICATION_REQUIRED), this is the user's email.
 */
exception EDAMUserException {
  1:  required  EDAMErrorCode errorCode,
  2:  optional  string parameter
}


/**
 * This exception is thrown by EDAM procedures when a call fails as a result of
 * a problem in the service that could not be changed through caller action.
 *
 * errorCode:  The numeric code indicating the type of error that occurred.
 *   must be one of the values of EDAMErrorCode.
 *
 * message:  This may contain additional information about the error
 *
 * rateLimitDuration:  Indicates the minimum number of seconds that an application should
 *   expect subsequent API calls for this user to fail. The application should not retry
 *   API requests for the user until at least this many seconds have passed. Present only
 *   when errorCode is RATE_LIMIT_REACHED,
 */
exception EDAMSystemException {
  1:  required  EDAMErrorCode errorCode,
  2:  optional  string message,
  3:  optional  i32 rateLimitDuration
}


/**
 * This exception is thrown by EDAM procedures when a caller asks to perform
 * an operation on an object that does not exist.  This may be thrown based on an invalid
 * primary identifier (e.g. a bad GUID), or when the caller refers to an object
 * by another unique identifier (e.g. a User's email address).
 *
 * identifier:  A description of the object that was not found on the server.
 *   For example, "Note.notebookGuid" when a caller attempts to create a note in a
 *   notebook that does not exist in the user's account.
 *
 * key:  The value passed from the client in the identifier, which was not
 *   found. For example, the GUID that was not found.
 */
exception EDAMNotFoundException {
  1:  optional  string identifier,
  2:  optional  string key
}

/**
 * An exception thrown when the provided Contacts fail validation. For instance,
 * email domains could be invalid, phone numbers might not be valid for SMS,
 * etc.
 *
 * We will not provide individual reasons for each Contact's validation failure.
 * The presence of the Contact in this exception means that the user must figure
 * out how to take appropriate action to fix this Contact.
 *
 * <dl>
 *   <dt>contacts</dt>
 *   <dd>The list of Contacts that are considered invalid by the service</dd>
 *
 *   <dt>parameter</dt>
 *   <dd>If the error applied to a particular input parameter, this will
 *   indicate which parameter.</dd>
 *
 *   <dt>reasons</dt>
 *   <dd>If supplied, the list of reasons why the server considered a contact invalid,
 *   matching, in order, the list returned in the contacts field.</dd>
 * </dl>
 */
exception EDAMInvalidContactsException {
  1:  required  list<Types.Contact> contacts,
  2:  optional  string parameter,
  3:  optional  list<EDAMInvalidContactReason> reasons
}
