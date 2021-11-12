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
 * This file contains the definitions of the Evernote data model as it
 * is represented through the EDAM protocol.  This is the "client-independent"
 * view of the contents of a user's account.  Each client will translate the
 * neutral data model into an appropriate form for storage on that client.
 */

include "Limits.thrift"

namespace as3 com.evernote.edam.type
namespace java com.evernote.edam.type
namespace csharp Evernote.EDAM.Type
namespace py evernote.edam.type
namespace cpp evernote.edam
namespace rb Evernote.EDAM.Type
namespace php EDAM.Types
namespace cocoa EDAM
namespace perl EDAMTypes
namespace go edam


// =============================== typedefs ====================================

/**
 * A monotonically incrementing number on each shard that identifies a cross shard
 * cache invalidation event.
 */
typedef i64 InvalidationSequenceNumber


/**
 * A type alias for the primary identifiers for Identity objects.
 */
typedef i64 IdentityID


/**
 * Every Evernote account is assigned a unique numeric identifier which
 * will not change for the life of the account.  This is independent of
 * the (string-based) "username" which is known by the user for login
 * purposes.  The user should have no reason to know their UserID.
 */
typedef i32 UserID


/**
 * Most data elements within a user's account (e.g. notebooks, notes, tags,
 * resources, etc.) are internally referred to using a globally unique
 * identifier that is written in a standard string format.  For example:
 *
 *    "8743428c-ef91-4d05-9e7c-4a2e856e813a"
 *
 * The internal components of the GUID are not given any particular meaning:
 * only the entire string is relevant as a unique identifier.
 */
typedef string Guid


/**
 * An Evernote Timestamp is the date and time of an event in UTC time.
 * This is expressed as a specific number of milliseconds since the
 * standard base "epoch" of:
 *
 *    January 1, 1970, 00:00:00 GMT
 *
 * NOTE:  the time is expressed at the resolution of milliseconds, but
 * the value is only precise to the level of seconds.   This means that
 * the last three (decimal) digits of the timestamp will be '000'.
 *
 * The Thrift IDL specification does not include a native date/time type,
 * so this value is used instead.
 *
 * The service will accept timestamp values (e.g. for Note created and update
 * times) between 1000-01-01 and 9999-12-31
 */
typedef i64 Timestamp

/**
 * A sequence number for the MessageStore subsystem.
 */
typedef i64 MessageEventID

/**
 * A type alias for the primary identifiers for MessageThread objects.
 */
typedef i64 MessageThreadID

// ============================= Enumerations ==================================

/**
 * This enumeration defines the possible permission levels for a user.
 * Free accounts will have a level of NORMAL and paid Premium accounts
 * will have a level of PREMIUM.
 */
enum PrivilegeLevel {
  NORMAL = 1,
  PREMIUM = 3,
  VIP = 5,
  MANAGER = 7,
  SUPPORT = 8,
  ADMIN = 9
}

/**
 * This enumeration defines the possible tiers of service that a user may have. A
 * ServiceLevel of BUSINESS signifies a business-only account, which can never be any
 * other ServiceLevel.
 */
enum ServiceLevel {
  BASIC = 1,
  PLUS = 2,
  PREMIUM = 3,
  BUSINESS = 4
}

/**
 * Every search query is specified as a sequence of characters.
 * Currently, only the USER query format is supported.
 */
enum QueryFormat {
  USER = 1,
  SEXP = 2
}


/**
 * This enumeration defines the possible sort ordering for notes when
 * they are returned from a search result.
 */
enum NoteSortOrder {
  CREATED = 1,
  UPDATED = 2,
  RELEVANCE = 3,
  UPDATE_SEQUENCE_NUMBER = 4,
  TITLE = 5
}


/**
 * This enumeration defines the possible states of a premium account
 *
 * NONE:    the user has never attempted to become a premium subscriber
 *
 * PENDING: the user has requested a premium account but their charge has not
 *   been confirmed
 *
 * ACTIVE:  the user has been charged and their premium account is in good
 *  standing
 *
 * FAILED:  the system attempted to charge the was denied. We will periodically attempt to
 *  re-validate their order.
 *
 * CANCELLATION_PENDING: the user has requested that no further charges be made
 *   but the current account is still active.
 *
 * CANCELED: the premium account was canceled either because of failure to pay
 *   or user cancelation. No more attempts will be made to activate the account.
 */
enum PremiumOrderStatus {
  NONE                 = 0,
  PENDING              = 1,
  ACTIVE               = 2,
  FAILED               = 3,
  CANCELLATION_PENDING = 4,
  CANCELED             = 5
}

/**
 * Privilege levels for accessing shared notebooks.
 *
 * Note that as of 2014-04, FULL_ACCESS is synonymous with BUSINESS_FULL_ACCESS.  If a
 * user is a member of a business and has FULL_ACCESS privileges, then they will
 * automatically be granted BUSINESS_FULL_ACCESS for notebooks in their business.  This
 * will happen implicitly when they attempt to access the corresponding notebooks of
 * the business.  BUSINESS_FULL_ACCESS is therefore deprecated.
 *
 * READ_NOTEBOOK: Recipient is able to read the contents of the shared notebook
 *   but does not have access to information about other recipients of the
 *   notebook or the activity stream information.
 *
 * MODIFY_NOTEBOOK_PLUS_ACTIVITY: Recipient has rights to read and modify the contents
 *   of the shared notebook, including the right to move notes to the trash and to create
 *   notes in the notebook.  The recipient can also access information about other
 *   recipients and the activity stream.
 *
 * READ_NOTEBOOK_PLUS_ACTIVITY: Recipient has READ_NOTEBOOK rights and can also
 *   access information about other recipients and the activity stream.
 *
 * GROUP: If the user belongs to a group, such as a Business, that has a defined
 *   privilege level, use the privilege level of the group as the privilege for
 *   the individual.
 *
 * FULL_ACCESS: Recipient has full rights to the shared notebook and recipient lists,
 *   including privilege to revoke and create invitations and to change privilege
 *   levels on invitations for individuals.  For members of a business, FULL_ACCESS
 *   privilege on business notebooks also grants the ability to change how the notebook
 *   will appear when shared with the business, including the rights to share and
 *   unshare the notebook with the business.
 *
 * BUSINESS_FULL_ACCESS: Deprecated.  See the note above about BUSINESS_FULL_ACCESS and
 *   FULL_ACCESS being synonymous.
 */
enum SharedNotebookPrivilegeLevel {
  READ_NOTEBOOK                  = 0,
  MODIFY_NOTEBOOK_PLUS_ACTIVITY  = 1,
  READ_NOTEBOOK_PLUS_ACTIVITY    = 2,
  GROUP                          = 3,
  FULL_ACCESS                    = 4,
  BUSINESS_FULL_ACCESS           = 5
}

/**
 * Privilege levels for accessing a shared note. All privilege levels convey "activity feed" access,
 * which allows the recipient to access information about other recipients and the activity stream.
 *
 * READ_NOTE: Recipient has rights to read the shared note.
 *
 * MODIFY_NOTE: Recipient has all of the rights of READ_NOTE, plus rights to modify the shared
 *   note's content, title and resources. Other fields, including the notebook, tags and metadata,
 *   may not be modified.
 *
 * FULL_ACCESS: Recipient has all of the rights of MODIFY_NOTE, plus rights to share the note with
 *   other users via email, public note links, and note sharing. Recipient may also update and
 *   remove other recipient's note sharing rights.
 */
enum SharedNotePrivilegeLevel {
  READ_NOTE = 0,
  MODIFY_NOTE = 1,
  FULL_ACCESS = 2
}

/**
 * Enumeration of the roles that a User can have within a sponsored group.
 *
 * GROUP_MEMBER: The user is a member of the group with no special privileges.
 *
 * GROUP_ADMIN: The user is an administrator within the group.
 *
 * GROUP_OWNER: The user is the owner of the group.
 */
enum SponsoredGroupRole {
  GROUP_MEMBER = 1,
  GROUP_ADMIN = 2,
  GROUP_OWNER = 3
}

/**
 * Enumeration of the roles that a User can have within an Evernote Business account.
 *
 * ADMIN: The user is an administrator of the Evernote Business account.
 *
 * NORMAL: The user is a regular user within the Evernote Business account.
 */
enum BusinessUserRole {
  ADMIN = 1,
  NORMAL = 2,
}

/**
 * The BusinessUserStatus indicates the status of the user in the business.
 *
 * A BusinessUser will typically start as ACTIVE.
 * Only ACTIVE users can authenticate to the Business.
 *
 * <dl>
 * <dt>ACTIVE<dt>
 * <dd>The business user can authenticate to and access the business.</dd>
 * <dt>DEACTIVATED<dt>
 * <dd>The business user has been deactivated and cannot access the business</dd>
 * </dl>
 */
enum BusinessUserStatus {
  ACTIVE = 1,
  DEACTIVATED = 2,
}

/**
 * An enumeration describing restrictions on the domain of shared notebook
 * instances that are valid for a given operation, as used, for example, in
 * NotebookRestrictions.
 *
 * ASSIGNED: The domain consists of shared notebooks that belong, or are assigned,
 * to the recipient.
 *
 * NO_SHARED_NOTEBOOKS: No shared notebooks are applicable to the operation.
 */
enum SharedNotebookInstanceRestrictions {
  /*
   * originally had the name ONLY_JOINED_OR_PREVIEW and was renamed after the
   * allowPreview feature was removed.
   */
  ASSIGNED = 1,

  /*
   * most restrictive
   */
  NO_SHARED_NOTEBOOKS = 2
}

/**
 * An enumeration describing the configuration state related to receiving
 * reminder e-mails from the service.  Reminder e-mails summarize notes
 * based on their Note.attributes.reminderTime values.
 *
 * DO_NOT_SEND: The user has selected to not receive reminder e-mail.
 *
 * SEND_DAILY_EMAIL: The user has selected to receive reminder e-mail for those
 *   days when there is a reminder.
 */
enum ReminderEmailConfig {
  DO_NOT_SEND      = 1,
  SEND_DAILY_EMAIL = 2
}

/**
 * An enumeration defining the possible states of a BusinessInvitation.
 *
 * APPROVED: The invitation was created or approved by a business admin and may be redeemed by the
 *   invited email.
 *
 * REQUESTED: The invitation was requested by a non-admin member of the business and must be
 *   approved by an admin before it may be redeemed. Invitations in this state do not count
 *   against a business' seat limit.
 *
 * REDEEMED: The invitation has already been redeemed. Invitations in this state do not count
 *   against a business' seat limit.
 */
enum BusinessInvitationStatus {
  APPROVED = 0,
  REQUESTED = 1,
  REDEEMED = 2
}

/**
 * What kinds of Contacts does the Evernote service know about?
 */
enum ContactType {
  EVERNOTE = 1,
  SMS = 2,
  FACEBOOK = 3,
  EMAIL = 4,
  TWITTER = 5,
  LINKEDIN = 6,
}

/**
 * Entity types
 */
enum EntityType {
  NOTE = 1,
  NOTEBOOK = 2,
  WORKSPACE = 3
}


// ============================== Constants ===================================

/**
 * A value for the "recipe" key in the "classifications" map in NoteAttributes
 * that indicates the user has classified a note as being a non-recipe.
 */
const string CLASSIFICATION_RECIPE_USER_NON_RECIPE = "000";

/**
 * A value for the "recipe" key in the "classifications" map in NoteAttributes
 * that indicates the user has classified a note as being a recipe.
 */
const string CLASSIFICATION_RECIPE_USER_RECIPE = "001";

/**
 * A value for the "recipe" key in the "classifications" map in NoteAttributes
 * that indicates the Evernote service has classified a note as being a recipe.
 */
const string CLASSIFICATION_RECIPE_SERVICE_RECIPE = "002";

/**
 * Standardized value for the 'source' NoteAttribute for notes that
 * were clipped from the web in some manner.
 */
const string EDAM_NOTE_SOURCE_WEB_CLIP = "web.clip";

/**
 * Standardized value for the 'source' NoteAttribute for notes that
 * were clipped using the "simplified article" function of the clipper.
 */
const string EDAM_NOTE_SOURCE_WEB_CLIP_SIMPLIFIED = "Clearly";

/**
 * Standardized value for the 'source' NoteAttribute for notes that
 * were clipped from an email message.
 */
const string EDAM_NOTE_SOURCE_MAIL_CLIP = "mail.clip";

/**
 * Standardized value for the 'source' NoteAttribute for notes that
 * were created via email sent to Evernote's email interface.
 */
const string EDAM_NOTE_SOURCE_MAIL_SMTP_GATEWAY = "mail.smtp";


// ============================== Structures ===================================

/**
 * In several places, EDAM exchanges blocks of bytes of data for a component
 * which may be relatively large.  For example:  the contents of a clipped
 * HTML note, the bytes of an embedded image, or the recognition XML for
 * a large image.  This structure is used in the protocol to represent
 * any of those large blocks of data when they are transmitted or when
 * they are only referenced their metadata.
 *
 *<dl>
 * <dt>bodyHash</dt>
 *   <dd>This field carries a one-way hash of the contents of the
 *   data body, in binary form.  The hash function is MD5<br/>
 *   Length:  EDAM_HASH_LEN (exactly)
 *   </dd>
 *
 * <dt>size</dt>
 *   <dd>The length, in bytes, of the data body.
 *   </dd>
 *
 * <dt>body</dt>
 *   <dd>This field is set to contain the binary contents of the data
 *   whenever the resource is being transferred.  If only metadata is
 *   being exchanged, this field will be empty.  For example, a client could
 *   notify the service about the change to an attribute for a resource
 *   without transmitting the binary resource contents.
 *   </dd>
 * </dl>
 */
struct Data {
  1:  optional  binary bodyHash,
  2:  optional  i32 size,
  3:  optional  binary body
}


/**
 * A structure holding the optional attributes that can be stored
 * on a User.  These are generally less critical than the core User fields.
 *
 *<dl>
 * <dt>defaultLocationName</dt>
 *   <dd>the location string that should be associated
 *   with the user in order to determine where notes are taken if not otherwise
 *   specified.<br/>
 *   Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>defaultLatitude</dt>
 *   <dd>if set, this is the latitude that should be
 *   assigned to any notes that have no other latitude information.
 *   </dd>
 *
 * <dt>defaultLongitude</dt>
 *   <dd>if set, this is the longitude that should be
 *   assigned to any notes that have no other longitude information.
 *   </dd>
 *
 * <dt>preactivation</dt>
 *   <dd>if set, the user account is not yet confirmed for
 *   login.  I.e. the account has been created, but we are still waiting for
 *   the user to complete the activation step.
 *   </dd>
 *
 * <dt>viewedPromotions</dt>
 *   <dd>a list of promotions the user has seen.
 *    This list may occasionally be modified by the system when promotions are
 *    no longer available.<br/>
 *    Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>incomingEmailAddress</dt>
 *   <dd>if set, this is the email address that the
 *    user may send email to in order to add an email note directly into the
 *    account via the SMTP email gateway.  This is the part of the email
 *    address before the '@' symbol ... our domain is not included.
 *    If this is not set, the user may not add notes via the gateway.<br/>
 *    Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>recentMailedAddresses</dt>
 *   <dd>if set, this will contain a list of email
 *    addresses that have recently been used as recipients
 *    of outbound emails by the user.  This can be used to pre-populate a
 *    list of possible destinations when a user wishes to send a note via
 *    email.<br/>
 *    Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX each<br/>
 *    Max:  EDAM_USER_RECENT_MAILED_ADDRESSES_MAX entries
 *   </dd>
 *
 * <dt>comments</dt>
 *   <dd>Free-form text field that may hold general support
 *    information, etc.<br/>
 *    Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>dateAgreedToTermsOfService</dt>
 *   <dd>The date/time when the user agreed to
 *    the terms of service.  This can be used as the effective "start date"
 *    for the account.
 *   </dd>
 *
 * <dt>maxReferrals</dt>
 *   <dd>The number of referrals that the user is permitted
 *    to make.
 *   </dd>
 *
 * <dt>referralCount</dt>
 *   <dd>The number of referrals sent from this account.
 *   </dd>
 *
 * <dt>refererCode</dt>
 *   <dd>A code indicating where the user was sent from. AKA
 *    promotion code
 *   </dd>
 *
 * <dt>sentEmailDate</dt>
 *   <dd>The most recent date when the user sent outbound
 *    emails from the service.  Used with sentEmailCount to limit the number
 *    of emails that can be sent per day.
 *   </dd>
 *
 * <dt>sentEmailCount</dt>
 *   <dd>The number of emails that were sent from the user
 *    via the service on sentEmailDate.  Used to enforce a limit on the number
 *    of emails per user per day to prevent spamming.
 *   </dd>
 *
 * <dt>dailyEmailLimit</dt>
 *   <dd>If set, this is the maximum number of emails that
 *    may be sent in a given day from this account.  If unset, the server will
 *    use the configured default limit.
 *   </dd>
 *
 * <dt>emailOptOutDate</dt>
 *   <dd>If set, this is the date when the user asked
 *    to be excluded from offers and promotions sent by Evernote.  If not set,
 *    then the user currently agrees to receive these messages.
 *   </dd>
 *
 * <dt>partnerEmailOptInDate</dt>
 *   <dd>If set, this is the date when the user asked
 *    to be included in offers and promotions sent by Evernote's partners.
 *    If not sent, then the user currently does not agree to receive these
 *    emails.
 *   </dd>
 *
 * <dt>preferredLanguage</dt>
 *   <dd>a 2 character language codes based on:
 *       http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt used for
 *      localization purposes to determine what language to use for the web
 *      interface and for other direct communication (e.g. emails).
 *   </dd>
 *
 * <dt>preferredCountry</dt>
 *   <dd>Preferred country code based on ISO 3166-1-alpha-2 indicating the
 *   users preferred country</dd>
 *
 * <dt>clipFullPage</dt>
 *   <dd>Boolean flag set to true if the user wants to clip full pages by
 *   default when they use the web clipper without a selection.</dd>
 *
 * <dt>twitterUserName</dt>
 *   <dd>The username of the account of someone who has chosen to enable
 *   Twittering into Evernote.  This value is subject to change, since users
 *   may change their Twitter user name.</dd>
 *
 * <dt>twitterId</dt>
 *   <dd>The unique identifier of the user's Twitter account if that user
 *   has chosen to enable Twittering into Evernote.</dd>
 *
 * <dt>groupName</dt>
 *   <dd>A name identifier used to identify a particular set of branding and
 *    light customization.</dd>
 *
 * <dt>recognitionLanguage</dt>
 *   <dd>a 2 character language codes based on:
 *       http://ftp.ics.uci.edu/pub/ietf/http/related/iso639.txt
 *       If set, this is used to determine the language that should be used
 *       when processing images and PDF files to find text.
 *       If not set, then the 'preferredLanguage' will be used.
 *   </dd>
 *
 * <dt>educationalInstitution</dt>
 *   <dd>a flag indicating that the user is part of an educational institution which
 *   makes them eligible for discounts on bulk purchases
 *   </dd>
 *
 * <dt>businessAddress</dt>
 *   <dd>A string recording the business address of a Sponsored Account user who has requested invoicing.
 *   </dd>
 *
 * <dt>hideSponsorBilling</dt>
 *   <dd>A flag indicating whether to hide the billing information on a sponsored
 *       account owner's settings page
 *   </dd>
 *
 * <dt>useEmailAutoFiling</dt>
 *   <dd>A flag indicating whether the user chooses to allow Evernote to automatically
 *       file and tag emailed notes
 *   </dd>
 *
 * <dt>reminderEmailConfig</dt>
 *   <dd>Configuration state for whether or not the user wishes to receive
 *       reminder e-mail.  This setting applies to both the reminder e-mail sent
 *       for personal reminder notes and for the reminder e-mail sent for reminder
 *       notes in the user's business notebooks that the user has configured for
 *       e-mail notifications.
 *   </dd>
 *
 * <dt>emailAddressLastConfirmed</dt>
 *   <dd>If set, this contains the time at which the user last confirmed that the
 *       configured email address for this account is correct and up-to-date. If this is
 *       unset that indicates that the user's email address is unverified.
 *   </dd>
 *
 * <dt>passwordUpdated</dt>
 *   <dd>If set, this contains the time at which the user's password last changed. This
 *       will be unset for users created before the addition of this field who have not
 *       changed their passwords since the addition of this field.
 *   </dd>
 *
 * <dt>shouldLogClientEvent</dt>
 *   <dd>If set to True, the server will record LogRequest send from clients of this
 *        user as ClientEventLog.
 *   </dd>
 *
 * <dt>optOutMachineLearning</dt>
 *   <dd>If set to True, no Machine Learning nor human review will be done to this
 *        user's note contents.
 *   </dd>
 *   </dl>
 */
struct UserAttributes {
  1:  optional  string defaultLocationName,
  2:  optional  double defaultLatitude,
  3:  optional  double defaultLongitude,
  4:  optional  bool preactivation,
  5:  optional  list<string> viewedPromotions,
  6:  optional  string incomingEmailAddress,
  7:  optional  list<string> recentMailedAddresses,
  9:  optional  string comments,
  11: optional  Timestamp dateAgreedToTermsOfService,
  12: optional  i32 maxReferrals,
  13: optional  i32 referralCount,
  14: optional  string refererCode,
  15: optional  Timestamp sentEmailDate,
  16: optional  i32 sentEmailCount,
  17: optional  i32 dailyEmailLimit,
  18: optional  Timestamp emailOptOutDate,
  19: optional  Timestamp partnerEmailOptInDate,
  20: optional  string preferredLanguage,
  21: optional  string preferredCountry,
  22: optional  bool clipFullPage,
  23: optional  string twitterUserName,
  24: optional  string twitterId,
  25: optional  string groupName,
  26: optional  string recognitionLanguage,
  28: optional  string referralProof,
  29: optional  bool educationalDiscount,
  30: optional  string businessAddress,
  31: optional  bool hideSponsorBilling,
  33: optional  bool useEmailAutoFiling,
  34: optional  ReminderEmailConfig reminderEmailConfig,
  35: optional  Timestamp emailAddressLastConfirmed,
  36: optional  Timestamp passwordUpdated,
  37: optional  bool salesforcePushEnabled,
  38: optional  bool shouldLogClientEvent,
  39: optional  bool optOutMachineLearning
}

/**
 * A structure holding the optional attributes associated with users
 * in a business.
 *
 * <dl>
 *  <dt>title</dt>
 *  <dd>Free form text of this user's title in the business</dd>
 *
 *  <dt>location</dt>
 *  <dd>City, State (for US) or City / Province for other countries</dd>
 *
 *  <dt>department</dt>
 *  <dd>Free form text of the department this user belongs to.</dd>
 *
 *  <dt>mobilePhone</dt>
 *  <dd>User's mobile phone number. Stored as plain text without any formatting.</dd>
 *
 *  <dt>linkedInProfileUrl</dt>
 *  <dd>URL to user's public LinkedIn profile page. This should only contain
 *  the portion relative to the base LinkedIn URL. For example: "/pub/john-smith/".
 *  </dd>
 *
 *  <dt>workPhone</dt>
 *  <dd>User's work phone number. Stored as plain text without any formatting.</dd>
 *
 *  <dt>companyStartDate</dt>
 *  <dd>The date on which the user started working at their company.</dd>
 * </dl>
 */
struct BusinessUserAttributes {
  1: optional string title,
  2: optional string location,
  3: optional string department,
  4: optional string mobilePhone,
  5: optional string linkedInProfileUrl,
  6: optional string workPhone,
  7: optional Timestamp companyStartDate
}

/**
 * This represents the bookkeeping information for the user's subscription.
 *
 *<dl>
 * <dt>uploadLimitEnd</dt>
 *   <dd>The date and time when the current upload limit
 *   expires.  At this time, the monthly upload count reverts to 0 and a new
 *   limit is imposed.  This date and time is exclusive, so this is effectively
 *   the start of the new month.
 *   </dd>
 * <dt>uploadLimitNextMonth</dt>
 *   <dd> When uploadLimitEnd is reached, the service
 *   will change uploadLimit to uploadLimitNextMonth. If a premium account is
 *   canceled, this mechanism will reset the quota appropriately.
 *   </dd>
 * <dt>premiumServiceStatus</dt>
 *   <dd>Indicates the phases of a premium account
 *   during the billing process.
 *   </dd>
 * <dt>premiumOrderNumber</dt>
 *   <dd>The order number used by the commerce system to
 *   process recurring payments
 *   </dd>
 * <dt>premiumServiceStart</dt>
 *   <dd>The start date when this premium promotion
 *   began (this number will get overwritten if a premium service is canceled
 *   and then re-activated).
 *   </dd>
 * <dt>premiumCommerceService</dt>
 *   <dd>The commerce system used (paypal, Google
 *   checkout, etc)
 *   </dd>
 * <dt>premiumServiceSKU</dt>
 *   <dd>The code associated with the purchase eg. monthly
 *   or annual purchase. Clients should interpret this value and localize it.
 *   </dd>
 * <dt>lastSuccessfulCharge</dt>
 *   <dd>Date the last time the user was charged.
 *   Null if never charged.
 *   </dd>
 * <dt>lastFailedCharge</dt>
 *   <dd>Date the last time a charge was attempted and
 *   failed.
 *   </dd>
 * <dt>lastFailedChargeReason</dt>
 *   <dd>Reason provided for the charge failure
 *   </dd>
 * <dt>nextPaymentDue</dt>
 *   <dd>The end of the billing cycle. This could be in the
 *   past if there are failed charges.
 *   </dd>
 * <dt>premiumLockUntil</dt>
 *   <dd>An internal variable to manage locking operations
 *   on the commerce variables.
 *   </dd>
 * <dt>updated</dt>
 *   <dd>The date any modification where made to this record.
 *   </dd>
 * <dt>premiumSubscriptionNumber</dt>
 *   <dd>The number number identifying the
 *   recurring subscription used to make the recurring charges.
 *   </dd>
 * <dt>lastRequestedCharge</dt>
 *   <dd>Date charge last attempted</dd>
 * <dt>currency</dt>
 *   <dd>ISO 4217 currency code</dd>
 * <dt>unitPrice</dt>
 *   <dd>charge in the smallest unit of the currency (e.g. cents for USD)</dd>
 * <dt>businessId</dt>
 *   <dd><i>DEPRECATED:</i>See BusinessUserInfo.</dd>
 * <dt>businessName</dt>
 *   <dd><i>DEPRECATED:</i>See BusinessUserInfo.</dd>
 * <dt>businessRole</dt>
 *   <dd><i>DEPRECATED:</i>See BusinessUserInfo.</dd>
 * <dt>unitDiscount</dt>
 *   <dd>discount per seat in negative amount and smallest unit of the currency (e.g.
 *       cents for USD)</dd>
 * <dt>nextChargeDate</dt>
 *   <dd>The next time the user will be charged, may or may not be the same as
 *       nextPaymentDue</dd>
 * </dl>
 */
struct Accounting {
  2:  optional  Timestamp          uploadLimitEnd,
  3:  optional  i64                uploadLimitNextMonth,
  4:  optional  PremiumOrderStatus premiumServiceStatus,
  5:  optional  string             premiumOrderNumber,
  6:  optional  string             premiumCommerceService,
  7:  optional  Timestamp          premiumServiceStart,
  8:  optional  string             premiumServiceSKU,
  9:  optional  Timestamp          lastSuccessfulCharge,
  10: optional  Timestamp          lastFailedCharge,
  11: optional  string             lastFailedChargeReason,
  12: optional  Timestamp          nextPaymentDue,
  13: optional  Timestamp          premiumLockUntil,
  14: optional  Timestamp          updated,
  16: optional  string             premiumSubscriptionNumber,
  17: optional  Timestamp          lastRequestedCharge,
  18: optional  string             currency,
  19: optional  i32                unitPrice,
  20: optional  i32                businessId,
  21: optional  string             businessName,
  22: optional  BusinessUserRole   businessRole,
  23: optional  i32                unitDiscount,
  24: optional  Timestamp          nextChargeDate,
  25: optional  i32                availablePoints
}

/**
 * This structure is used to provide information about an Evernote Business
 * membership, for members who are part of a business.
 *
 * <dl>
 * <dt>businessId</dt>
 *   <dd>The ID of the Evernote Business account that the user is a member of.
 * <dt>businessName</dt>
 *   <dd>The human-readable name of the Evernote Business account that the user
 *       is a member of.</dd>
 * <dt>role</dt>
 *   <dd>The role of the user within the Evernote Business account that
 *       they are a member of.</dd>
 * <dt>email</dt>
 *   <dd>An e-mail address that will be used by the service in the context of your
 *       Evernote Business activities.  For example, this e-mail address will be used
 *       when you e-mail a business note, when you update notes in the account of
 *       your business, etc.  The business e-mail cannot be used for identification
 *       purposes such as for logging into the service.
 *   </dd>
 * <dt>updated</dt>
 *   <dd>Last time the business user or business user attributes were updated.</dd>
 * </dl>
 */
struct BusinessUserInfo {
  1:  optional  i32              businessId,
  2:  optional  string           businessName,
  3:  optional  BusinessUserRole role,
  4:  optional  string           email,
  5:  optional  Timestamp        updated
}

/**
 * This structure is used to provide account limits that are in effect for this user.
 *<dl>
 * <dt>userMailLimitDaily</dt>
 *   <dd>The number of emails of any type that can be sent by a user from the
 *       service per day.  If an email is sent to two different recipients, this
 *       counts as two emails.
 *   </dd>
 * <dt>noteSizeMax</dt>
 *   <dd>Maximum total size of a Note that can be added.  The size of a note is
 *       calculated as:
 *       ENML content length (in Unicode characters) plus the sum of all resource
 *       sizes (in bytes).
 *   </dd>
 * <dt>resourceSizeMax</dt>
 *   <dd>Maximum size of a resource, in bytes
 *   </dd>
 * <dt>userLinkedNotebookMax</dt>
 *   <dd>Maximum number of linked notebooks per account.
 *   </dd>
 * <dt>uploadLimit</dt>
 *   <dd>The number of bytes that can be uploaded to the account
 *   in the current month.  For new notes that are created, this is the length
 *   of the note content (in Unicode characters) plus the size of each resource
 *   (in bytes).  For edited notes, this is the the difference between the old
 *   length and the new length (if this is greater than 0) plus the size of
 *   each new resource.
 *   </dd> 
 * <dt>userNoteCountMax</dt>
 *   <dd>Maximum number of Notes per user</dd> 
 * <dt>userNotebookCountMax</dt>
 *   <dd>Maximum number of Notebooks per user</dd> 
 * <dt>userTagCountMax</dt>
 *   <dd>Maximum number of Tags per account</dd> 
 * <dt>noteTagCountMax</dt>
 *   <dd>Maximum number of Tags per Note</dd> 
 * <dt>userSavedSearchesMax</dt>
 *   <dd>Maximum number of SavedSearches per account</dd>
 * <dt>noteResourceCountMax</dt>
 *   <dd>The maximum number of Resources per Note</dd>
 * </dl>
 */
struct AccountLimits {
  1:  optional i32 userMailLimitDaily,
  2:  optional i64 noteSizeMax,
  3:  optional i64 resourceSizeMax,
  4:  optional i32 userLinkedNotebookMax,
  5:  optional i64 uploadLimit,
  6:  optional i32 userNoteCountMax,
  7:  optional i32 userNotebookCountMax,
  8:  optional i32 userTagCountMax,
  9:  optional i32 noteTagCountMax,
  10: optional i32 userSavedSearchesMax,
  11: optional i32 noteResourceCountMax
}

/**
 * This represents the information about a single user account.
 *<dl>
 * <dt>id</dt>
 *   <dd>The unique numeric identifier for the account, which will not
 *   change for the lifetime of the account.
 *   </dd>
 *
 * <dt>username</dt>
 *   <dd>The name that uniquely identifies a single user account. This name
 *   may be presented by the user, along with their password, to log into
 *   their account.
 *   May only contain a-z, 0-9, or '-', and may not start or end with the '-'
 *   <br/>
 *   Length:  EDAM_USER_USERNAME_LEN_MIN - EDAM_USER_USERNAME_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_USER_USERNAME_REGEX
 *   </dd>
 *
 * <dt>email</dt>
 *   <dd>The email address registered for the user.  Must comply with
 *   RFC 2821 and RFC 2822.<br/>
 *   Third party applications that authenticate using OAuth do not have
 *   access to this field.
 *   Length:  EDAM_EMAIL_LEN_MIN - EDAM_EMAIL_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_EMAIL_REGEX
 *   </dd>
 *
 * <dt>name</dt>
 *   <dd>The printable name of the user, which may be a combination
 *   of given and family names.  This is used instead of separate "first"
 *   and "last" names due to variations in international name format/order.
 *   May not start or end with a whitespace character.  May contain any
 *   character but carriage return or newline (Unicode classes Zl and Zp).
 *   <br/>
 *   Length:  EDAM_USER_NAME_LEN_MIN - EDAM_USER_NAME_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_USER_NAME_REGEX
 *   </dd>
 *
 * <dt>timezone</dt>
 *   <dd>The zone ID for the user's default location.  If present,
 *   this may be used to localize the display of any timestamp for which no
 *   other timezone is available.
 *   The format must be encoded as a standard zone ID such as
 *   "America/Los_Angeles" or "GMT+08:00"
 *   <br/>
 *   Length:  EDAM_TIMEZONE_LEN_MIN - EDAM_TIMEZONE_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_TIMEZONE_REGEX
 *   </dd>
 *
 * <dt>serviceLevel</dt>
 *   <dd>The level of service the user currently receives. This will always be populated
 *       for users retrieved from the Evernote service.
 *   </dd>
 *
 * <dt>created</dt>
 *   <dd>The date and time when this user account was created in the
 *   service.
 *   </dd>
 *
 * <dt>updated</dt>
 *   <dd>The date and time when this user account was last modified
 *   in the service.
 *   </dd>
 *
 * <dt>deleted</dt>
 *   <dd>If the account has been deleted from the system (e.g. as
 *   the result of a legal request by the user), the date and time of the
 *   deletion will be represented here.  If not, this value will not be set.
 *   </dd>
 *
 * <dt>active</dt>
 *   <dd>If the user account is available for login and
 *   synchronization, this flag will be set to true.
 *   </dd>
 *
 * <dt>shardId</dt>
 *   <dd>DEPRECATED - Client applications should have no need to use this field.
 *   </dd>
 *
 * <dt>attributes</dt>
 *   <dd>If present, this will contain a list of the attributes
 *   for this user account.
 *   </dd>
 *
 * <dt>accounting</dt>
 *   <dd>Bookkeeping information for the user's subscription.
 *   </dd>
 *
 * <dt>businessUserInfo</dt>
 *   <dd>If present, this will contain a set of business information
 *   relating to the user's business membership.  If not present, the
 *   user is not currently part of a business.
 *   </dd>
 *
 * <dt>photoUrl</dt>
 *   <dd>The URL of the photo that represents this User. This field is filled in by the
 *   service and is read-only to clients. If <code>photoLastUpdated</code> is
 *   not set, this url will point to a placeholder user photo generated by the
 *   service.</dd>
 *
 * <dt>photoLastUpdated</dt>
 *   <dd>The time at which the photo at 'photoUrl' was last updated by this User. This
 *   field will be null if the User never set a profile photo. This field is filled in by
 *   the service and is read-only to clients.</dd>
 *
 * <dt>accountLimits</dt>
 *   <dd>Account limits applicable for this user.</dd>
 */
struct User {
  1:  optional  UserID id,
  2:  optional  string username,
  3:  optional  string email,
  4:  optional  string name,
  6:  optional  string timezone,
  7:  optional  PrivilegeLevel privilege,
  21: optional  ServiceLevel serviceLevel,
  9:  optional  Timestamp created,
  10: optional  Timestamp updated,
  11: optional  Timestamp deleted,
  13: optional  bool active,
  14: optional  string shardId,
  15: optional  UserAttributes attributes,
  16: optional  Accounting accounting,
  18: optional  BusinessUserInfo businessUserInfo,
  19: optional  string photoUrl,
  20: optional  Timestamp photoLastUpdated,
  22: optional  AccountLimits accountLimits
}


/**
 * A structure that represents contact information. Note this does not necessarily correspond to
 * an Evernote user.
 *
 * <dl>
 * <dt>name</dt>
 * <dd>The displayable name of this contact. This field is filled in by the service and
 *     is read-only to clients.
 * </dd>
 * <dt>id</dt>
 * <dd>A unique identifier for this ContactType.
 * </dd>
 * <dt>type</dt>
 * <dd>What service does this contact come from?
 * </dd>
 * <dt>photoUrl</dt>
 * <dd>A URL of a profile photo representing this Contact. This field is filled in by the
 *     service and is read-only to clients.
 * </dd>
 * <dt>photoLastUpdated</dt>
 * <dd>timestamp when the profile photo at 'photoUrl' was last updated.
 *     This field will be null if the user has never set a profile photo.
 *     This field is filled in by the service and is read-only to clients.
 * </dd>
 * <dt>messagingPermit</dt>
 * <dd>This field will only be filled by the service when it is giving a Contact record
 *     to a client, and that client does not normally have enough permission to send a
 *     new message to the person represented through this Contact. In that case, this
 *     whole Contact record could be used to send a new Message to the Contact, and the
 *     service will inspect this permit to confirm that operation was allowed.
 * </dd>
 * <dt>messagingPermitExpires</dt>
 * <dd>If this field is set, then this (whole) Contact record may be used in calls to
 *     sendMessage until this time. After that time, those calls may be rejected by the
 *     service if the caller does not have direct permission to initiate a message with
 *     the represented Evernote user.
 * </dd>
 * </dl>
 */
struct Contact {
  1: optional string name,
  2: optional string id,
  3: optional ContactType type,
  4: optional string photoUrl,
  5: optional Timestamp photoLastUpdated,
  6: optional binary messagingPermit,
  7: optional Timestamp messagingPermitExpires
}

/**
 * An object that represents the relationship between a Contact that possibly
 * belongs to an Evernote User.
 *
 * <dl>
 *  <dt>id</dt>
 *  <dd>The unique identifier for this mapping.
 *  </dd>
 *
 *  <dt>contact<dt>
 *  <dd>The Contact that can be used to address this Identity. May be unset.
 *  </dd>
 *
 *  <dt>userId</dt>
 *  <dd>The Evernote User id that is connected to the Contact. May be unset
 *      if this identity has not yet been claimed, or the caller is not
 *      connected to this identity.
 *  </dd>
 *
 *  <dt>deactivated</dt>
 *  <dd>Indicates that the contact for this identity is no longer active and
 *  should not be used when creating new threads using Destination.recipients,
 *  unless you know of another Identity instance with the same contact information
 *  that is active.  If you are connected to the user (see userConnected), you
 *  can still create threads using their Evernote-type contact.</dd>
 *
 *  <dt>sameBusiness</dt>
 *  <dd>Does this Identity belong to someone who is in the same business as the
 *      caller?
 *  </dd>
 *
 *  <dt>blocked</dt>
 *  <dd>Has the caller blocked the Evernote user this Identity represents?
 *  </dd>
 *
 *  <dt>userConnected</dt>
 *  <dd>Indicates that the caller is "connected" to the user of this
 *  identity via this identity.  When you have a connection via an
 *  identity, you should always create new threads using the
 *  Evernote-type contact (see ContactType) using the userId field
 *  from a connected Identity.  On the Evernote service, the
 *  Evernote-type contact is the most durable. Phone numbers and
 *  e-mail addresses can get re-assigned but your Evernote account
 *  user ID will remain the same.  A connection exists when both of
 *  you are in the same business or the user has replied to a thread
 *  that you are on.  When connected, you will also get to see more
 *  information about the user who has claimed the identity.  Note
 *  that you are never connected to yourself since you won't be
 *  sending messages to yourself, but you will obviously see your own
 *  profile information.
 *  </dd>
 *
 *  <dt>eventId</dt>
 *  <dd>A server-assigned sequence number for the events in the messages
 *  subsystem.
 *  </dd>
 * </dl>
 */
struct Identity {
  1: required IdentityID id,
  2: optional Contact contact,
  3: optional UserID userId,
  4: optional bool deactivated,
  5: optional bool sameBusiness,
  6: optional bool blocked,
  7: optional bool userConnected,
  8: optional MessageEventID eventId
}

/**
 * A tag within a user's account is a unique name which may be organized
 * a simple hierarchy.
 *<dl>
 * <dt>guid</dt>
 *   <dd>The unique identifier of this tag. Will be set by the service,
 *   so may be omitted by the client when creating the Tag.
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>name</dt>
 *   <dd>A sequence of characters representing the tag's identifier.
 *   Case is preserved, but is ignored for comparisons.
 *   This means that an account may only have one tag with a given name, via
 *   case-insensitive comparison, so an account may not have both "food" and
 *   "Food" tags.
 *   May not contain a comma (','), and may not begin or end with a space.
 *   <br/>
 *   Length:  EDAM_TAG_NAME_LEN_MIN - EDAM_TAG_NAME_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_TAG_NAME_REGEX
 *   </dd>
 *
 * <dt>parentGuid</dt>
 *   <dd>If this is set, then this is the GUID of the tag that
 *   holds this tag within the tag organizational hierarchy.  If this is
 *   not set, then the tag has no parent and it is a "top level" tag.
 *   Cycles are not allowed (e.g. a->parent->parent == a) and will be
 *   rejected by the service.
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>updateSequenceNum</dt>
 *   <dd>A number identifying the last transaction to
 *   modify the state of this object.  The USN values are sequential within an
 *   account, and can be used to compare the order of modifications within the
 *   service.
 *   </dd>
 * </dl>
 */
struct Tag {
  1:  optional  Guid guid,
  2:  optional  string name,
  3:  optional  Guid parentGuid,
  4:  optional  i32 updateSequenceNum
}


/**
 * A structure that wraps a map of name/value pairs whose values are not
 * always present in the structure in order to reduce space when obtaining
 * batches of entities that contain the map.
 *
 * When the server provides the client with a LazyMap, it will fill in either
 * the keysOnly field or the fullMap field, but never both, based on the API
 * and parameters.
 *
 * When a client provides a LazyMap to the server as part of an update to
 * an object, the server will only update the LazyMap if the fullMap field is
 * set. If the fullMap field is not set, the server will not make any changes
 * to the map.
 *
 * Check the API documentation of the individual calls involving the LazyMap
 * for full details including the constraints of the names and values of the
 * map.
 *
 * <dl>
 * <dt>keysOnly</dt>
 *   <dd>The set of keys for the map.  This field is ignored by the
 *       server when set.
 *   </dd>
 *
 * <dt>fullMap</dt>
 *   <dd>The complete map, including all keys and values.
 *   </dd>
 * </dl>
 */
struct LazyMap {
  1:  optional  set<string> keysOnly,
  2:  optional  map<string, string> fullMap
}


/**
 * Structure holding the optional attributes of a Resource
 * <dl>
 * <dt>sourceURL</dt>
 *   <dd>the original location where the resource was hosted
 *   <br/>
 *    Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>timestamp</dt>
 *   <dd>the date and time that is associated with this resource
 *   (e.g. the time embedded in an image from a digital camera with a clock)
 *   </dd>
 *
 * <dt>latitude</dt>
 *   <dd>the latitude where the resource was captured
 *   </dd>
 *
 * <dt>longitude</dt>
 *   <dd>the longitude where the resource was captured
 *   </dd>
 *
 * <dt>altitude</dt>
 *   <dd>the altitude where the resource was captured
 *   </dd>
 *
 * <dt>cameraMake</dt>
 *   <dd>information about an image's camera, e.g. as embedded in
 *   the image's EXIF data
 *   <br/>
 *   Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>cameraModel</dt>
 *   <dd>information about an image's camera, e.g. as embedded
 *   in the image's EXIF data
 *   <br/>
 *   Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>clientWillIndex</dt>
 *   <dd>if true, then the original client that submitted
 *   the resource plans to submit the recognition index for this resource at a
 *   later time.
 *   </dd>
 *
 * <dt>recoType</dt>
 *   <dd>DEPRECATED - this field is no longer set by the service, so should
 *     be ignored.
 *   </dd>
 *
 * <dt>fileName</dt>
 *   <dd>if the resource came from a source that provided an
 *   explicit file name, the original name will be stored here.  Many resources
 *   come from unnamed sources, so this will not always be set.
 *   </dd>
 *
 * <dt>attachment</dt>
 *   <dd>this will be true if the resource should be displayed as an attachment,
 *   or false if the resource should be displayed inline (if possible).
 *   </dd>
 *
 * <dt>applicationData</dt>
 * <dd>Provides a location for applications to store a relatively small
 * (4kb) blob of data associated with a Resource that is not visible to the user
 * and that is opaque to the Evernote service. A single application may use at most
 * one entry in this map, using its API consumer key as the map key. See the
 * documentation for LazyMap for a description of when the actual map values
 * are returned by the service.
 * <p>To safely add or modify your application's entry in the map, use
 * NoteStore.setResourceApplicationDataEntry. To safely remove your application's
 * entry from the map, use NoteStore.unsetResourceApplicationDataEntry.</p>
 * Minimum length of a name (key): EDAM_APPLICATIONDATA_NAME_LEN_MIN
 * <br/>
 * Sum max size of key and value: EDAM_APPLICATIONDATA_ENTRY_LEN_MAX
 * <br/>
 * Syntax regex for name (key): EDAM_APPLICATIONDATA_NAME_REGEX
 * </dd>
 *
 * </dl>
 */
struct ResourceAttributes {
  1:  optional  string sourceURL,
  2:  optional  Timestamp timestamp,
  3:  optional  double latitude,
  4:  optional  double longitude,
  5:  optional  double altitude,
  6:  optional  string cameraMake,
  7:  optional  string cameraModel,
  8:  optional  bool clientWillIndex,
  9:  optional  string recoType,
  10: optional  string fileName,
  11: optional  bool attachment,
  12: optional  LazyMap applicationData
}


/**
 * Every media file that is embedded or attached to a note is represented
 * through a Resource entry.
 * <dl>
 * <dt>guid</dt>
 *   <dd>The unique identifier of this resource.  Will be set whenever
 *   a resource is retrieved from the service, but may be null when a client
 *   is creating a resource.
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>noteGuid</dt>
 *   <dd>The unique identifier of the Note that holds this
 *   Resource. Will be set whenever the resource is retrieved from the service,
 *   but may be null when a client is creating a resource.
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>data</dt>
 *   <dd>The contents of the resource.
 *   Maximum length:  The data.body is limited to EDAM_RESOURCE_SIZE_MAX_FREE
 *   for free accounts and EDAM_RESOURCE_SIZE_MAX_PREMIUM for premium accounts.
 *   </dd>
 *
 * <dt>mime</dt>
 *   <dd>The MIME type for the embedded resource.  E.g. "image/gif"
 *   <br/>
 *   Length:  EDAM_MIME_LEN_MIN - EDAM_MIME_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_MIME_REGEX
 *   </dd>
 *
 * <dt>width</dt>
 *   <dd>If set, this contains the display width of this resource, in
 *   pixels.
 *   </dd>
 *
 * <dt>height</dt>
 *   <dd>If set, this contains the display height of this resource,
 *   in pixels.
 *   </dd>
 *
 * <dt>duration</dt>
 *   <dd>DEPRECATED: ignored.
 *   </dd>
 *
 * <dt>active</dt>
 *   <dd>If the resource is active or not.
 *   </dd>
 *
 * <dt>recognition</dt>
 *   <dd>If set, this will hold the encoded data that provides
 *   information on search and recognition within this resource.
 *   </dd>
 *
 * <dt>attributes</dt>
 *   <dd>A list of the attributes for this resource.
 *   </dd>
 *
 * <dt>updateSequenceNum</dt>
 *   <dd>A number identifying the last transaction to
 *   modify the state of this object. The USN values are sequential within an
 *   account, and can be used to compare the order of modifications within the
 *   service.
 *   </dd>
 *
 * <dt>alternateData</dt>
 *   <dd>Some Resources may be assigned an alternate data format by the service
 *   which may be more appropriate for indexing or rendering than the original
 *   data provided by the user.  In these cases, the alternate data form will
 *   be available via this Data element.  If a Resource has no alternate form,
 *   this field will be unset.</dd>
 * </dl>
 */
struct Resource {
  1:  optional  Guid guid,
  2:  optional  Guid noteGuid,
  3:  optional  Data data,
  4:  optional  string mime,
  5:  optional  i16 width,
  6:  optional  i16 height,
  7:  optional  i16 duration,
  8:  optional  bool active,
  9:  optional  Data recognition,
  11: optional  ResourceAttributes attributes,
  12: optional  i32 updateSequenceNum,
  13: optional  Data alternateData
}


/**
 * The list of optional attributes that can be stored on a note.
 * <dl>
 * <dt>subjectDate</dt>
 *   <dd>time that the note refers to
 *   </dd>
 *
 * <dt>latitude</dt>
 *   <dd>the latitude where the note was taken
 *   </dd>
 *
 * <dt>longitude</dt>
 *   <dd>the longitude where the note was taken
 *   </dd>
 *
 * <dt>altitude</dt>
 *   <dd>the altitude where the note was taken
 *   </dd>
 *
 * <dt>author</dt>
 *   <dd>the author of the content of the note
 *   <br/>
 *   Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>source</dt>
 *   <dd>the method that the note was added to the account, if the
 *   note wasn't directly authored in an Evernote desktop client.
 *   <br/>
 *   Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>sourceURL</dt>
 *   <dd>the original location where the resource was hosted. For web clips,
 *   this will be the URL of the page that was clipped.
 *   <br/>
 *   Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>sourceApplication</dt>
 *   <dd>an identifying string for the application that
 *   created this note.  This string does not have a guaranteed syntax or
 *   structure -- it is intended for human inspection and tracking.
 *   <br/>
 *   Length:  EDAM_ATTRIBUTE_LEN_MIN - EDAM_ATTRIBUTE_LEN_MAX
 *   </dd>
 *
 * <dt>shareDate</dt>
 *  <dd>The date and time when this note was directly shared via its own URL.
 *  This is only set on notes that were individually shared - it is independent
 *  of any notebook-level sharing of the containing notebook. This field
 *  is treated as "read-only" for clients; the server will ignore changes
 *  to this field from an external client.
 *  </dd>
 *
 * <dt>reminderOrder</dt>
 * <dd>The set of notes with this parameter set are considered
 * "reminders" and are to be treated specially by clients to give them
 * higher UI prominence within a notebook.  The value is used to sort
 * the reminder notes within the notebook with higher values
 * representing greater prominence.  Outside of the context of a
 * notebook, the value of this parameter is undefined.  The value is
 * not intended to be compared to the values of reminder notes in
 * other notebooks.  In order to allow clients to place a note at a
 * higher precedence than other notes, you should never set a value
 * greater than the current time (as defined for a Timetstamp). To
 * place a note at higher precedence than existing notes, set the
 * value to the current time as defined for a timestamp (milliseconds
 * since the epoch).  Synchronizing clients must remember the time when
 * the update was performed, using the local clock on the client,
 * and use that value when they later upload the note to the service.
 * Clients must not set the reminderOrder to the reminderTime as the
 * reminderTime could be in the future.  Those two fields are never
 * intended to be related.  The correct value for reminderOrder field
 * for new notes is the "current" time when the user indicated that
 * the note is a reminder.  Clients may implement a separate
 * "sort by date" feature to show notes ordered by reminderTime.
 * Whenever a reminderDoneTime or reminderTime is set but a
 * reminderOrder is not set, the server will fill in the current
 * server time for the reminderOrder field.</dd>
 *
 * <dt>reminderDoneTime</dt>
 * <dd>The date and time when a user dismissed/"marked done" the reminder
 * on the note.  Users typically do not manually set this value directly
 * as it is set to the time when the user dismissed/"marked done" the
 * reminder.</dd>
 *
 * <dt>reminderTime</dt>
 * <dd>The date and time a user has selected to be reminded of the note.
 * A note with this value set is known as a "reminder" and the user can
 * be reminded, via e-mail or client-specific notifications, of the note
 * when the time is reached or about to be reached.  When a user sets
 * a reminder time on a note that has a reminder done time, and that
 * reminder time is in the future, then the reminder done time should be
 * cleared.  This should happen regardless of any existing reminder time
 * that may have previously existed on the note.</dd>
 *
 * <dt>placeName</dt>
 * <dd>Allows the user to assign a human-readable location name associated
 * with a note. Users may assign values like 'Home' and 'Work'. Place
 * names may also be populated with values from geonames database
 * (e.g., a restaurant name). Applications are encouraged to normalize values
 * so that grouping values by place name provides a useful result. Applications
 * MUST NOT automatically add place name values based on geolocation without
 * confirmation from the user; that is, the value in this field should be
 * more useful than a simple automated lookup based on the note's latitude
 * and longitude.</dd>
 *
 * <dt>contentClass</dt>
 * <dd>The class (or type) of note. This field is used to indicate to
 * clients that special structured information is represented within
 * the note such that special rules apply when making
 * modifications. If contentClass is set and the client
 * application does not specifically support the specified class,
 * the client MUST treat the note as read-only. In this case, the
 * client MAY modify the note's notebook and tags via the
 * Note.notebookGuid and Note.tagGuids fields.  The client MAY also
 * modify the reminderOrder field as well as the reminderTime and
 * reminderDoneTime fields.
 * <p>Applications should set contentClass only when they are creating notes
 * that contain structured information that needs to be maintained in order
 * for the user to be able to use the note within that application.
 * Setting contentClass makes a note read-only in other applications, so
 * there is a trade-off when an application chooses to use contentClass.
 * Applications that set contentClass when creating notes must use a contentClass
 * string of the form <i>CompanyName.ApplicationName</i> to ensure uniqueness.</p>
 * Length restrictions: EDAM_NOTE_CONTENT_CLASS_LEN_MIN, EDAM_NOTE_CONTENT_CLASS_LEN_MAX
 * <br/>
 * Regex: EDAM_NOTE_CONTENT_CLASS_REGEX
 * </dd>
 *
 * <dt>applicationData</dt>
 * <dd>Provides a location for applications to store a relatively small
 * (4kb) blob of data that is not meant to be visible to the user and
 * that is opaque to the Evernote service. A single application may use at most
 * one entry in this map, using its API consumer key as the map key. See the
 * documentation for LazyMap for a description of when the actual map values
 * are returned by the service.
 * <p>To safely add or modify your application's entry in the map, use
 * NoteStore.setNoteApplicationDataEntry. To safely remove your application's
 * entry from the map, use NoteStore.unsetNoteApplicationDataEntry.</p>
 * Minimum length of a name (key): EDAM_APPLICATIONDATA_NAME_LEN_MIN
 * <br/>
 * Sum max size of key and value: EDAM_APPLICATIONDATA_ENTRY_LEN_MAX
 * <br/>
 * Syntax regex for name (key): EDAM_APPLICATIONDATA_NAME_REGEX
 * </dd>
 *
 * <dt>creatorId</dt>
 * <dd>The numeric user ID of the user who originally created the note.</dd>
 *
 * <dt>lastEditedBy</dt>
 * <dd>An indication of who made the last change to the note.  If you are
 * accessing the note via a shared notebook to which you have modification
 * rights, or if you are the owner of the notebook to which the note belongs,
 * then you have access to the value.  In this case, the value will be
 * unset if the owner of the notebook containing the note was the last to
 * make the modification, else it will be a string describing the
 * guest who made the last edit.  If you do not have access to this value,
 * it will be left unset.  This field is read-only by clients.  The server
 * will ignore all values set by clients into this field.</dd>
 *
 * <dt>lastEditorId</dt>
 * <dd>The numeric user ID of the user described in lastEditedBy.</dd>
 *
 * <dt>classifications</dt>
 * <dd>A map of classifications applied to the note by clients or by the
 * Evernote service. The key is the string name of the classification type,
 * and the value is a constant that begins with CLASSIFICATION_.</dd>
 *
 * <dt>sharedWithBusiness</dt>
 * <dd>When this flag is set on a business note, any user in that business
 * may view the note if they request it by GUID. This field is read-only by
 * clients. The server will ignore all values set by clients into this field.
 *
 * To share a note with the business, use NoteStore.shareNoteWithBusiness and
 * to stop sharing a note with the business, use NoteStore.stopSharingNoteWithBusiness.
 * </dd>
 *
 * <dt>conflictSourceNoteGuid</dt>
 * <dd>If set, this specifies the GUID of a note that caused a sync conflict
 * resulting in the creation of a duplicate note. The duplicated note contains
 * the user's changes that could not be applied as a result of the sync conflict,
 * and uses the conflictSourceNoteGuid field to specify the note that caused the
 * conflict. This allows clients to provide a customized user experience for note
 * conflicts.
 * </dd>
 *
 * <dt>noteTitleQuality</dt>
 * <dd>If set, this specifies that the note's title was automatically generated
 * and indicates the likelihood that the generated title is useful for display to
 * the user. If not set, the note's title was manually entered by the user.
 *
 * Clients MUST set this attribute to one of the following values when the
 * corresponding note's title was not manually entered by the user:
 * EDAM_NOTE_TITLE_QUALITY_UNTITLED, EDAM_NOTE_TITLE_QUALITY_LOW,
 * EDAM_NOTE_TITLE_QUALITY_MEDIUM or EDAM_NOTE_TITLE_QUALITY_HIGH.
 *
 * When a user edits a note's title, clients MUST unset this value.
 * </dd>
 * </dl>
 */
struct NoteAttributes {
  1:  optional  Timestamp subjectDate,
  10: optional  double latitude,
  11: optional  double longitude,
  12: optional  double altitude,
  13: optional  string author,
  14: optional  string source,
  15: optional  string sourceURL,
  16: optional  string sourceApplication,
  17: optional  Timestamp shareDate,
  18: optional  i64 reminderOrder,
  19: optional  Timestamp reminderDoneTime,
  20: optional  Timestamp reminderTime,
  21: optional  string placeName,
  22: optional  string contentClass,
  23: optional  LazyMap applicationData,
  24: optional  string lastEditedBy,
  26: optional  map<string, string> classifications,
  27: optional  UserID creatorId,
  28: optional  UserID lastEditorId,
  29: optional  bool sharedWithBusiness,
  30: optional  Guid conflictSourceNoteGuid,
  31: optional  i32 noteTitleQuality
}


/**
 * Represents a relationship between a note and a single share invitation recipient. The recipient
 * is identified via an Identity, and has a given privilege that specifies what actions they may
 * take on the note.
 *
 * <dl>
 *   <dt>sharerUserID</dt>
 *   <dd>The user ID of the user who shared the note with the recipient.</dd>
 *
 *   <dt>recipientIdentity</dt>
 *   <dd>The identity of the recipient of the share. For a given note, there may be only one
 *     SharedNote per recipient identity. Only recipientIdentity.id is guaranteed to be set.
 *     Other fields on the Identity may or my not be set based on the requesting user's
 *     relationship with the recipient.</dd>
 *
 *   <dt>privilege</dt>
 *   <dd>The privilege level that the share grants to the recipient.</dd>
 *
 *   <dt>serviceCreated</dt>
 *   <dd>The time at which the share was created.</dd>
 *
 *   <dt>serviceUpdated</dt>
 *   <dd>The time at which the share was last updated.</dd>
 *
 *   <dt>serviceAssigned</dt>
 *   <dd>The time at which the share was assigned to a specific recipient user ID.</dd>
 * </dl>
 */
struct SharedNote {
  1: optional UserID sharerUserID,
  2: optional Identity recipientIdentity,
  3: optional SharedNotePrivilegeLevel privilege,
  4: optional Timestamp serviceCreated,
  5: optional Timestamp serviceUpdated,
  6: optional Timestamp serviceAssigned
}

/**
 * This structure captures information about the operations that cannot be performed on a given
 * note that has been shared with a recipient via a SharedNote. The following operations are
 * <b>never</b> allowed based on SharedNotes, and as such are left out of the NoteRestrictions
 * structure for brevity:
 *
 * <ul>
 *   <li>Expunging a note (NoteStore.expungeNote)</li>
 *   <li>Moving a note to the trash (Note.active)</li>
 *   <li>Updating a note's notebook (Note.notebookGuid)</li>
 *   <li>Updating a note's tags (Note.tagGuids, Note.tagNames)</li>
 *   <li>Updating a note's attributes (Note.attributes)</li>
 *   <li>Sharing a note with the business (NoteStore.shareNoteWithBusiness</li>
 *   <li>Getting a note's version history (NoteStore.listNoteVersions,
 *     NoteStore.getNoteVersion)</li>
 * </ul>
 *
 * When a client has permission to update a note's title or content, it may also update the
 * Note.updated timestamp.
 *
 * <b>This structure reflects only the privileges / restrictions conveyed by the SharedNote.</b>
 * It does not incorporate privileges conveyed by a potential SharedNotebook to the same
 * recipient. As such, the actual permissions that the recipient has on the note may differ from
 * the permissions expressed in this structure.
 *
 * For example, consider a user with read-only access to a shared notebook, and a read-write share
 * of a specific note in the notebook. The note restrictions would contain noUpdateTitle = false,
 * while the notebook restrictions would contain noUpdateNotes = true. In this case, the user is
 * allowed to update the note title based on the note restrictions.
 *
 * Alternatively, consider a user with read-write access to a shared notebook, and a read-only
 * share of a specific note in that notebook. The note restrictions would contain
 * noUpdateTitle = true, while the notebook restrictions would contain noUpdateNotes = false. In
 * this case, the user would have full edit permissions on the note based on the notebook
 * restrictions.
 *
 * <dl>
 *   <dt>noUpdateTitle</dt>
 *   <dd>The client may not update the note's title (Note.title).</dd>
 *
 *   <dt>noUpdateContent<dt>
 *   <dd>The client may not update the note's content. Content includes Note.content
 *     and Note.resources, as well as the related fields Note.contentHash and
 *     Note.contentLength.</dd>
 *
 *   <dt>noEmail</dt>
 *   <dd>The client may not email the note (NoteStore.emailNote).</dd>
 *
 *   <dt>noShare</dt>
 *   <dd>The client may not share the note with specific recipients
 *     (NoteStore.createOrUpdateSharedNotes).</dd>
 *
 *   <dt>noSharePublicly</dt>
 *   <dd>The client may not make the note public (NoteStore.shareNote).</dd>
 * </dl>
 */
struct NoteRestrictions {
  1: optional bool noUpdateTitle,
  2: optional bool noUpdateContent,
  3: optional bool noEmail,
  4: optional bool noShare,
  5: optional bool noSharePublicly
}

/**
 * Represents the owner's account related limits on a Note.
 * The field uploaded represents the total number of bytes that have been uploaded
 * to this account and is taken from the SyncState struct. All other fields
 * represent account related limits and are taken from the AccountLimits struct.
 * <p />
 * See SyncState and AccountLimits struct field definitions for more details.
 */
struct NoteLimits {
  1: optional i32 noteResourceCountMax,
  2: optional i64 uploadLimit,
  3: optional i64 resourceSizeMax,
  4: optional i64 noteSizeMax,
  5: optional i64 uploaded
}

/**
 * Represents a single note in the user's account.
 *
 * <dl>
 * <dt>guid</dt>
 *   <dd>The unique identifier of this note.  Will be set by the
 *   server, but will be omitted by clients calling NoteStore.createNote()
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>title</dt>
 *   <dd>The subject of the note.  Can't begin or end with a space.
 *   <br/>
 *   Length:  EDAM_NOTE_TITLE_LEN_MIN - EDAM_NOTE_TITLE_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_NOTE_TITLE_REGEX
 *   </dd>
 *
 * <dt>content</dt>
 *   <dd>The XHTML block that makes up the note.  This is
 *   the canonical form of the note's contents, so will include abstract
 *   Evernote tags for internal resource references.  A client may create
 *   a separate transformed version of this content for internal presentation,
 *   but the same canonical bytes should be used for transmission and
 *   comparison unless the user chooses to modify their content.
 *   <br/>
 *   Length:  EDAM_NOTE_CONTENT_LEN_MIN - EDAM_NOTE_CONTENT_LEN_MAX
 *   </dd>
 *
 * <dt>contentHash</dt>
 *   <dd>The binary MD5 checksum of the UTF-8 encoded content
 *   body. This will always be set by the server, but clients may choose to omit
 *   this when they submit a note with content.
 *   <br/>
 *   Length:  EDAM_HASH_LEN (exactly)
 *   </dd>
 *
 * <dt>contentLength</dt>
 *   <dd>The number of Unicode characters in the content of
 *   the note.  This will always be set by the service, but clients may choose
 *   to omit this value when they submit a Note.
 *   </dd>
 *
 * <dt>created</dt>
 *   <dd>The date and time when the note was created in one of the
 *   clients.  In most cases, this will match the user's sense of when
 *   the note was created, and ordering between notes will be based on
 *   ordering of this field.  However, this is not a "reliable" timestamp
 *   if a client has an incorrect clock, so it cannot provide a true absolute
 *   ordering between notes.  Notes created directly through the service
 *   (e.g. via the web GUI) will have an absolutely ordered "created" value.
 *   </dd>
 *
 * <dt>updated</dt>
 *   <dd>The date and time when the note was last modified in one of
 *   the clients.  In most cases, this will match the user's sense of when
 *   the note was modified, but this field may not be absolutely reliable
 *   due to the possibility of client clock errors.
 *   </dd>
 *
 * <dt>deleted</dt>
 *   <dd>If present, the note is considered "deleted", and this
 *   stores the date and time when the note was deleted by one of the clients.
 *   In most cases, this will match the user's sense of when the note was
 *   deleted, but this field may be unreliable due to the possibility of
 *   client clock errors.
 *   </dd>
 *
 * <dt>active</dt>
 *   <dd>If the note is available for normal actions and viewing,
 *   this flag will be set to true.
 *   </dd>
 *
 * <dt>updateSequenceNum</dt>
 *   <dd>A number identifying the last transaction to
 *   modify the state of this note (including changes to the note's attributes
 *   or resources).  The USN values are sequential within an account,
 *   and can be used to compare the order of modifications within the service.
 *   </dd>
 *
 * <dt>notebookGuid</dt>
 *   <dd>The unique identifier of the notebook that contains
 *   this note.  If no notebookGuid is provided on a call to createNote(), the
 *   default notebook will be used instead.
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>tagGuids</dt>
 *   <dd>A list of the GUID identifiers for tags that are applied to this note.
 *   This may be provided in a call to createNote() to unambiguously declare
 *   the tags that should be assigned to the new note.  Alternately, clients
 *   may pass the names of desired tags via the 'tagNames' field during
 *   note creation.
 *   If the list of tags are omitted on a call to createNote(), then
 *   the server will assume that no changes have been made to the resources.
 *   Maximum:  EDAM_NOTE_TAGS_MAX tags per note
 *   </dd>
 *
 * <dt>resources</dt>
 *   <dd>The list of resources that are embedded within this note.
 *   If the list of resources are omitted on a call to updateNote(), then
 *   the server will assume that no changes have been made to the resources.
 *   The binary contents of the resources must be provided when the resource
 *   is first sent to the service, but it will be omitted by the service when
 *   the Note is returned in the future.
 *   Maximum:  EDAM_NOTE_RESOURCES_MAX resources per note
 *   </dd>
 *
 * <dt>attributes</dt>
 *   <dd>A list of the attributes for this note.
 *   If the list of attributes are omitted on a call to updateNote(), then
 *   the server will assume that no changes have been made to the resources.
 *   </dd>
 *
 * <dt>tagNames</dt>
 *   <dd>May be provided by clients during calls to createNote() as an
 *   alternative to providing the tagGuids of existing tags.  If any tagNames
 *   are provided during createNote(), these will be found, or created if they
 *   don't already exist.  Created tags will have no parent (they will be at
 *   the top level of the tag panel).
 *   </dd>
 *
 * <dt>sharedNotes</dt>
 *   <dd>The list of recipients with whom this note has been shared. This field will be unset if
 *     the caller has access to the note via the containing notebook, but does not have activity
 *     feed permission for that notebook. This field is read-only. Clients may not make changes to
 *     a note's sharing state via this field.
 *   </dd>
 *
 *   <dt>restrictions</dt>
 *   <dd>If this field is set, the user has note-level permissions that may differ from their
 *     notebook-level permissions. In this case, the restrictions structure specifies
 *     a set of restrictions limiting the actions that a user may take on the note based
 *     on their note-level permissions. If this field is unset, then there are no
 *     note-specific restrictions. However, a client may still be limited based on the user's
 *     notebook permissions.</dd>
 * </dl>
 */
struct Note {
  1:  optional  Guid guid,
  2:  optional  string title,
  3:  optional  string content,
  4:  optional  binary contentHash,
  5:  optional  i32 contentLength,
  6:  optional  Timestamp created,
  7:  optional  Timestamp updated,
  8:  optional  Timestamp deleted,
  9:  optional  bool active,
  10: optional  i32 updateSequenceNum,
  11: optional  string notebookGuid,
  12: optional  list<Guid> tagGuids,
  13: optional  list<Resource> resources,
  14: optional  NoteAttributes attributes,
  15: optional  list<string> tagNames,
  16: optional  list<SharedNote> sharedNotes,
  17: optional  NoteRestrictions restrictions,
  18: optional  NoteLimits limits
}


/**
 * If a Notebook has been opened to the public, the Notebook will have a
 * reference to one of these structures, which gives the location and optional
 * description of the externally-visible public Notebook.
 * <dl>
 * <dt>uri</dt>
 *   <dd>If this field is present, then the notebook is published for
 *   mass consumption on the Internet under the provided URI, which is
 *   relative to a defined base publishing URI defined by the service.
 *   This field can only be modified via the web service GUI ... publishing
 *   cannot be modified via an offline client.
 *   <br/>
 *   Length:  EDAM_PUBLISHING_URI_LEN_MIN - EDAM_PUBLISHING_URI_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_PUBLISHING_URI_REGEX
 *   </dd>
 *
 * <dt>order</dt>
 *   <dd>When the notes are publicly displayed, they will be sorted
 *   based on the requested criteria.
 *   </dd>
 *
 * <dt>ascending</dt>
 *   <dd>If this is set to true, then the public notes will be
 *   displayed in ascending order (e.g. from oldest to newest).  Otherwise,
 *   the notes will be displayed in descending order (e.g. newest to oldest).
 *   </dd>
 *
 * <dt>publicDescription</dt>
 *   <dd>This field may be used to provide a short
 *   description of the notebook, which may be displayed when (e.g.) the
 *   notebook is shown in a public view.  Can't begin or end with a space.
 *   <br/>
 *   Length:  EDAM_PUBLISHING_DESCRIPTION_LEN_MIN -
 *            EDAM_PUBLISHING_DESCRIPTION_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_PUBLISHING_DESCRIPTION_REGEX
 *   </dd>
 *
 * </dl>
 */
struct Publishing {
  1:  optional  string uri,
  2:  optional  NoteSortOrder order,
  3:  optional  bool ascending,
  4:  optional  string publicDescription
}

/**
 * If a Notebook contained in an Evernote Business account has been published
 * the to business library, the Notebook will have a reference to one of these
 * structures, which specifies how the Notebook will be represented in the
 * library.
 *
 * <dl>
 * <dt>notebookDescription</dt>
 *   <dd>A short description of the notebook's content that will be displayed
 *       in the business library user interface. The description may not begin
 *       or end with whitespace.
 *   <br/>
 *   Length: EDAM_BUSINESS_NOTEBOOK_DESCRIPTION_LEN_MIN -
 *           EDAM_BUSINESS_NOTEBOOK_DESCRIPTION_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_BUSINESS_NOTEBOOK_DESCRIPTION_REGEX
 *   </dd>
 *
 * <dt>privilege</dt>
 *   <dd>The privileges that will be granted to users who join the notebook through
 *       the business library.
 *   </dd>
 *
 * <dt>recommended</dt>
 *   <dd>Whether the notebook should be "recommended" when displayed in the business
 *       library user interface.
 *   </dd>
 * </dl>
 */
struct BusinessNotebook {
  1:  optional  string notebookDescription,
  2:  optional  SharedNotebookPrivilegeLevel privilege,
  3:  optional  bool recommended
}


/**
 * A structure defining the scope of a SavedSearch.
 *
 * <dl>
 *   <dt>includeAccount</dt>
 *   <dd>The search should include notes from the account that contains the SavedSearch.</dd>
 *
 *   <dt>includePersonalLinkedNotebooks</dt>
 *   <dd>The search should include notes within those shared notebooks
 *   that the user has joined that are NOT business notebooks.</dd>
 *
 *   <dt>includeBusinessLinkedNotebooks</dt>
 *   <dd>The search should include notes within those shared notebooks
 *   that the user has joined that are business notebooks in the business that
 *   the user is currently a member of.</dd>
 * </dl>
 */
struct SavedSearchScope {
  1:  optional bool includeAccount,
  2:  optional bool includePersonalLinkedNotebooks,
  3:  optional bool includeBusinessLinkedNotebooks
}


/**
 * A named search associated with the account that can be quickly re-used.
 * <dl>
 * <dt>guid</dt>
 *   <dd>The unique identifier of this search.  Will be set by the
 *   service, so may be omitted by the client when creating.
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>name</dt>
 *   <dd>The name of the saved search to display in the GUI.  The
 *   account may only contain one search with a given name (case-insensitive
 *   compare). Can't begin or end with a space.
 *   <br/>
 *   Length:  EDAM_SAVED_SEARCH_NAME_LEN_MIN - EDAM_SAVED_SEARCH_NAME_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_SAVED_SEARCH_NAME_REGEX
 *   </dd>
 *
 * <dt>query</dt>
 *   <dd>A string expressing the search to be performed.
 *   <br/>
 *   Length:  EDAM_SAVED_SEARCH_QUERY_LEN_MIN - EDAM_SAVED_SEARCH_QUERY_LEN_MAX
 *   </dd>
 *
 * <dt>format</dt>
 *   <dd>The format of the query string, to determine how to parse
 *   and process it.
 *   </dd>
 *
 * <dt>updateSequenceNum</dt>
 *   <dd>A number identifying the last transaction to
 *   modify the state of this object.  The USN values are sequential within an
 *   account, and can be used to compare the order of modifications within the
 *   service.
 *   </dd>
 *
 * <dt>scope</dt>
 *   <dd><p>Specifies the set of notes that should be included in the search, if
 *    possible.</p>
 *    <p>Clients are expected to search as much of the desired scope as possible,
 *    with the understanding that a given client may not be able to cover the full
 *    specified scope. For example, when executing a search that includes notes in both
 *    the owner's account and business notebooks, a mobile client may choose to only
 *    search within the user's account because it is not capable of searching both
 *    scopes simultaneously. When a search across multiple scopes is not possible,
 *    a client may choose which scope to search based on the current application
 *    context. If a client cannot search any of the desired scopes, it should refuse
 *    to execute the search.</p>
 *    </dd>
 * </dl>
 */
struct SavedSearch {
  1:  optional  Guid guid,
  2:  optional  string name,
  3:  optional  string query,
  4:  optional  QueryFormat format,
  5:  optional  i32 updateSequenceNum,
  6:  optional  SavedSearchScope scope
}

/**
 * Settings meant for the recipient of a shared notebook, such as
 * for indicating which types of notifications the recipient wishes
 * for reminders, etc.
 *
 * The reminderNotifyEmail and reminderNotifyInApp fields have a
 * 3-state read value but a 2-state write value.  On read, it is
 * possible to observe "unset", true, or false.  The initial state is
 * "unset".  When you choose to set a value, you may set it to either
 * true or false, but you cannot unset the value.  Once one of these
 * members has a true/false value, it will always have a true/false
 * value.
 *
 * <dl>
 * <dt>reminderNotifyEmail</dt>
 * <dd>Indicates that the user wishes to receive daily e-mail notifications
 *     for reminders associated with the notebook. This may be true only for
 *     business notebooks that belong to the business of which the user is a
 *     member. You may only set this value on a notebook in your business.</dd>
 * <dt>reminderNotifyInApp</dt>
 * <dd>Indicates that the user wishes to receive notifications for
 *     reminders by applications that support providing such
 *     notifications.  The exact nature of the notification is defined
 *     by the individual applications.</dd>
 * </dl>
 **/
struct SharedNotebookRecipientSettings {
 1:  optional bool reminderNotifyEmail,
 2:  optional bool reminderNotifyInApp
}

/**
 * This enumeration defines the possible states that a notebook can be in for a recipient.
 * It encompasses the "inMyList" boolean and default notebook status.
 *
 * <dl>
 * <dt>NOT_IN_MY_LIST</dt>
 * <dd>The notebook is not in the recipient's list (not "joined").</dd>
 * <dt>IN_MY_LIST</dt>
 * <dd>The notebook is in the recipient's notebook list (formerly, we would say
 *     that the recipient has "joined" the notebook)</dd>
 * <dt>IN_MY_LIST_AND_DEFAULT_NOTEBOOK</dt>
 * <dd>The same as IN_MY_LIST and this notebook is the user's default notebook.</dd>
 * </dl>
 */
enum RecipientStatus {
  NOT_IN_MY_LIST = 1,
  IN_MY_LIST = 2,
  IN_MY_LIST_AND_DEFAULT_NOTEBOOK = 3,
}

/**
 * Settings meant for the recipient of a notebook share.
 *
 * Some of these fields have a 3-state read value but a 2-state write value.
 * On read, it is possible to observe "unset", true, or false. The initial
 * state is "unset". When you choose to set a value, you may set it to either
 * true or false, but you cannot unset the value. Once one of these members
 * has a true/false value, it will always have a true/false value.
 *
 * <dl>
 * <dt>reminderNotifyEmail</dt>
 * <dd>Indicates that the user wishes to receive daily e-mail notifications
 *     for reminders associated with the notebook. This may be
 *     true only for business notebooks that belong to the business of
 *     which the user is a member. You may only set this value on a
 *     notebook in your business. This value will initially be unset.</dd>
 * <dt>reminderNotifyInApp</dt>
 * <dd>Indicates that the user wishes to receive notifications for
 *     reminders by applications that support providing such
 *     notifications.  The exact nature of the notification is defined
 *     by the individual applications. This value will initially be unset.</dd>
 * </dl>
 * <dt>inMyList</dt>
 * <dd>DEPRECATED: Use recipientStatus instead.
 *     The notebook is on the recipient's notebook list (formerly, we would say
 *     that the recipient has "joined" the notebook)</dd>
 * <dt>recipientStatus</dt>
 * <dd>The notebook is on/off the recipient's notebook list (formerly, we would say
 *     that the recipient has "joined" the notebook) and perhaps also their
 *     default notebook</dd>
 * <dt>stack</dt>
 * <dd>The stack the recipient has put this notebook into. See Notebook.stack
 * for a definition. Every recipient can have their own stack value for the same
 * notebook.</dd>
 * </dl>
 **/
struct NotebookRecipientSettings {
 1:  optional bool reminderNotifyEmail,
 2:  optional bool reminderNotifyInApp,
 3:  optional bool inMyList,
 4:  optional string stack,
 5:  optional RecipientStatus recipientStatus,
}

/**
 * Shared notebooks represent a relationship between a notebook and a single
 * share invitation recipient.
 * <dl>
 * <dt>id</dt>
 * <dd>The primary identifier of the share, which is not globally unique.</dd>
 *
 * <dt>userId</dt>
 * <dd>The user id of the owner of the notebook.</dd>
 *
 * <dt>notebookGuid</dt>
 * <dd>The GUID of the notebook that has been shared.</dd>
 *
 * <dt>email</dt>
 * <dd>A string containing a display name for the recipient of the share. This may
 *     be an email address, a phone number, a full name, or some other descriptive
 *     string This field is read-only to clients. It will be filled in by the service
 *     when returning shared notebooks.
 * </dd>
 *
 * <dt>recipientIdentityId</dt>
 * <dd>The IdentityID of the share recipient. If present, only the user who has
 *     claimed that identity may access this share.
 * </dd>
 *
 * <dt>notebookModifiable</dt>
 * <dd>DEPRECATED</dd>
 *
 * <dt>serviceCreated</dt>
 * <dd>The date that the owner first created the share with the specific email
 *   address.</dd>
 *
 * <dt>serviceUpdated</dt>
 * <dd>The date the shared notebook was last updated on the service.  This
 *     will be updated when authenticateToSharedNotebook is called the first
 *     time with a shared notebook (i.e. when the username is bound to that
 *     shared notebook), and also when the SharedNotebook privilege is updated
 *     as part of a shareNotebook(...) call, as well as on any calls to
 *     updateSharedNotebook(...).
 * </dd>
 *
 * <dt>username</dt>
 * <dd>DEPRECATED. The username of the user who can access this share. This
 *     value is read-only to clients. It will be filled in by the service when
 *     returning shared notebooks.
 * </dd>
 *
 * <dt>privilege</dt>
 * <dd>The privilege level granted to the notebook, activity stream, and
 *     invitations.  See the corresponding enumeration for details.
 * </dd>
 *
 * <dt>recipientSettings</dt>
 * <dd>Settings intended for use only by the recipient of this shared
 *     notebook.  You should skip setting this value unless you want
 *     to change the value contained inside the structure, and only if
 *     you are the recipient.</dd>
 *
 * <dt>globalId</dt>
 * <dd>An immutable, opaque string that acts as a globally unique
 *     identifier for this shared notebook record.  You can use this field to
 *     match linked notebook and shared notebook records as well as to
 *     create new LinkedNotebook records.  This field replaces the deprecated
 *     shareKey field.
 * </dd>
 *
 * <dt>sharerUserId</dt>
 * <dd>The user id of the user who shared a notebook via this shared notebook
 *     instance. This may not be the same as userId, since a user with full
 *     access to a notebook may have created a new share for that notebook. For
 *     Business, this represents the user who shared the business notebook. This
 *     field is currently unset for a SharedNotebook created by joining a
 *     notebook that has been published to the business.
 * </dd>
 *
 * <dt>recipientUsername</dt>
 * <dd>The username of the user who can access this share. This is the username
 *     for the user with the id in recipientUserId. This value can be set
 *     by clients when calling shareNotebook(...), and that will result in the
 *     created SharedNotebook being assigned to a user. This value is always set
 *     if serviceAssigned is set.
 * </dd>
 *
 * <dt>recipientUserId</dt>
 * <dd>The id of the user who can access this share. This is the id for the user
 *     with the username in recipientUsername. This value is read-only and set
 *     by the service. Value set by clients will be ignored. This field may be unset
 *     for unjoined notebooks and is always set if serviceAssigned is set. Clients should
 *     prefer this field over recipientUsername unless they need to use usernames
 *     directly.
 * </dd>
 *
 * <dt>serviceAssigned</dt>
 * <dd>The date this SharedNotebook was assigned (i.e. has been associated with an
 *     Evernote user whose user ID is set in recipientUserId). Unset if the SharedNotebook
 *     is not assigned. This field is a read-only value that is set by the service.
 * </dd>
 * </dl>
 */
struct SharedNotebook {
  1:  optional i64 id,
  2:  optional UserID userId,
  3:  optional Guid notebookGuid,
  4:  optional string email,
  18: optional IdentityID recipientIdentityId,
  5:  optional bool notebookModifiable,  // deprecated
  7:  optional Timestamp serviceCreated,
 10:  optional Timestamp serviceUpdated,
  8:  optional string globalId, // rename from shareKey
  9:  optional string username, // deprecated
 11:  optional SharedNotebookPrivilegeLevel privilege,
 13:  optional SharedNotebookRecipientSettings recipientSettings,
 14:  optional UserID sharerUserId,
 15:  optional string recipientUsername,
 17:  optional UserID recipientUserId,
 16:  optional Timestamp serviceAssigned
}

/**
 * This enumeration defines the possible types of canMoveToContainer outcomes.
 * <p />
 * An outdated client is expected to signal a "Cannot Move, Please Upgrade To Learn Why"
 * like response to the user if an unknown enumeration value is received.
 * <dl>
 * <dt>CAN_BE_MOVED</dt>
 * <dd>Can move Notebook to Workspace.</dd>
 * <dt>INSUFFICIENT_ENTITY_PRIVILEGE</dt>
 * <dd>Can not move Notebook to Workspace, because either:
 *  a) Notebook not in Workspace and insufficient privilege on Notebook
 *  or b) Notebook in Workspace and membership on Workspace with insufficient privilege
 *  for move</dd>
 * <dt>INSUFFICIENT_CONTAINER_PRIVILEGE</dt>
 * <dd>Notebook in Workspace and no membership on Workspace.
 * </dd>
 * </dl>
 */
enum CanMoveToContainerStatus {
  CAN_BE_MOVED = 1,
  INSUFFICIENT_ENTITY_PRIVILEGE = 2,
  INSUFFICIENT_CONTAINER_PRIVILEGE = 3
}

/**
 * Specifies if the client can move a Notebook to a Workspace.
 */
struct CanMoveToContainerRestrictions {
  1:  optional CanMoveToContainerStatus canMoveToContainer
}

/**
 * This structure captures information about the types of operations
 * that cannot be performed on a given notebook with a type of
 * authenticated access and credentials.  The values filled into this
 * structure are based on then-current values in the server database
 * for shared notebooks and notebook publishing records, as well as
 * information related to the authentication token.  Information from
 * the authentication token includes the application that is accessing
 * the server, as defined by the permissions granted by consumer (api)
 * key, and the method used to obtain the token, for example via
 * authenticateToSharedNotebook, authenticateToBusiness, etc.  Note
 * that changes to values in this structure that are the result of
 * shared notebook or publishing record changes are communicated to
 * the client via a change in the notebook USN during sync.  It is
 * important to use the same access method, parameters, and consumer
 * key in order obtain correct results from the sync engine.
 *
 * The server has the final say on what is allowed as values may
 * change between calls to obtain NotebookRestrictions instances
 * and to operate on data on the service.
 *
 * If the following are set and true, then the given restriction is
 * in effect, as accessed by the same authentication token from which
 * the values were obtained.
 *
 * <dl>
 * <dt>noReadNotes</dt>
 *   <dd>The client is not able to read notes from the service and
 *   the notebook is write-only.
 *   </dd>
 * <dt>noCreateNotes</dt>
 *   <dd>The client may not create new notes in the notebook.
 *   </dd>
 * <dt>noUpdateNotes</dt>
 *   <dd>The client may not update notes currently in the notebook.
 *   </dd>
 * <dt>noExpungeNotes</dt>
 *   <dd>The client may not expunge notes currently in the notebook.
 *   </dd>
 * <dt>noShareNotes</dt>
 *   <dd>The client may not share notes in the notebook via the
 *   shareNote or createOrUpdateSharedNotes methods.
 *   </dd>
 * <dt>noEmailNotes</dt>
 *   <dd>The client may not e-mail notes by guid via the Evernote
 *   service by using the emailNote method.  Email notes by value
 *   by populating the note parameter instead.
 *   </dd>
 * <dt>noSendMessageToRecipients</dt>
 *   <dd>The client may not send messages to the share recipients of
 *   the notebook.
 *   </dd>
 * <dt>noUpdateNotebook</dt>
 *   <dd>The client may not update the Notebook object itself, for
 *   example, via the updateNotebook method.
 *   </dd>
 * <dt>noExpungeNotebook</dt>
 *   <dd>The client may not expunge the Notebook object itself, for
 *   example, via the expungeNotebook method.
 *   </dd>
 * <dt>noSetDefaultNotebook</dt>
 *   <dd>The client may not set this notebook to be the default notebook.
 *   The caller should leave Notebook.defaultNotebook unset.
 *   </dd>
 * <dt>noSetNotebookStack</dt>
 *   <dd>If the client is able to update the Notebook, the Notebook.stack
 *   value may not be set.
 *   </dd>
 * <dt>noPublishToPublic</dt>
 *   <dd>The client may not publish the notebook to the public.
 *   For example, business notebooks may not be shared publicly.
 *   </dd>
 * <dt>noPublishToBusinessLibrary</dt>
 *   <dd>The client may not publish the notebook to the business library.
 *   </dd>
 * <dt>noCreateTags</dt>
 *   <dd>The client may not complete an operation that results in a new tag
 *   being created in the owner's account.
 *   </dd>
 * <dt>noUpdateTags</dt>
 *   <dd>The client may not update tags in the owner's account.
 *   </dd>
 * <dt>noExpungeTags</dt>
 *   <dd>The client may not expunge tags in the owner's account.
 *   </dd>
 * <dt>noSetParentTag</dt>
 *   <dd>If the client is able to create or update tags in the owner's account,
 *   then they will not be able to set the parent tag.  Leave the value unset.
 *   </dd>
 * <dt>noCreateSharedNotebooks</dt>
 *   <dd>The client is unable to create shared notebooks for the notebook.
 *   </dd>
 * <dt>updateWhichSharedNotebookRestrictions</dt>
 *   <dd>Restrictions on which shared notebook instances can be updated.  If the
 *   value is not set or null, then the client can update any of the shared notebooks
 *   associated with the notebook on which the NotebookRestrictions are defined.
 *   See the enumeration for further details.
 *   </dd>
 * <dt>expungeWhichSharedNotebookRestrictions</dt>
 *   <dd>Restrictions on which shared notebook instances can be expunged.  If the
 *   value is not set or null, then the client can expunge any of the shared notebooks
 *   associated with the notebook on which the NotebookRestrictions are defined.
 *   See the enumeration for further details.
 *   </dd>
 * <dt>noShareNotesWithBusiness</dt>
 *   <dd>The client may not share notes in the notebook via the shareNoteWithBusiness
 *   method.
 *   </dd>
 * <dt>noRenameNotebook</dt>
 *   <dd>The client may not rename this notebook.</dd>
 * <dt>noSetInMyList</dt>
 *   <dd>clients may not change the NotebookRecipientSettings.inMyList settings for
 *   this notebook.</dd>
 * <dt>noSetContact</dt>
 *   <dd>The contact for this notebook may not be changed.</dd>
 * </dl>
 * <dt>canMoveToContainerRestrictions</dt>
 *   <dd>Specifies if the client can move this notebook to a container and if not,
 *   the reason why.</dd>
 * <dt>noCanMoveNote</dt>
 *   <dd>If set, the client cannot move a Note into or out of the Notebook.</dd>
 * </dl>
 */
struct NotebookRestrictions {
  1:  optional bool noReadNotes,
  2:  optional bool noCreateNotes,
  3:  optional bool noUpdateNotes,
  4:  optional bool noExpungeNotes,
  5:  optional bool noShareNotes,
  6:  optional bool noEmailNotes,
  7:  optional bool noSendMessageToRecipients,
  8:  optional bool noUpdateNotebook,
  9:  optional bool noExpungeNotebook,
  10: optional bool noSetDefaultNotebook,
  11: optional bool noSetNotebookStack,
  12: optional bool noPublishToPublic,
  13: optional bool noPublishToBusinessLibrary,
  14: optional bool noCreateTags,
  15: optional bool noUpdateTags,
  16: optional bool noExpungeTags,
  17: optional bool noSetParentTag,
  18: optional bool noCreateSharedNotebooks,
  19: optional SharedNotebookInstanceRestrictions updateWhichSharedNotebookRestrictions,
  20: optional SharedNotebookInstanceRestrictions expungeWhichSharedNotebookRestrictions,
  21: optional bool noShareNotesWithBusiness,
  22: optional bool noRenameNotebook,
  23: optional bool noSetInMyList,
  24: optional bool noChangeContact,
  26: optional CanMoveToContainerRestrictions canMoveToContainerRestrictions,
  27: optional bool noSetReminderNotifyEmail,
  28: optional bool noSetReminderNotifyInApp,
  29: optional bool noSetRecipientSettingsStack,
  30: optional bool noCanMoveNote
}

/**
 * A unique container for a set of notes.
 * <dl>
 * <dt>guid</dt>
 *   <dd>The unique identifier of this notebook.
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>name</dt>
 *   <dd>A sequence of characters representing the name of the
 *   notebook.  May be changed by clients, but the account may not contain two
 *   notebooks with names that are equal via a case-insensitive comparison.
 *   Can't begin or end with a space.
 *   <br/>
 *   Length:  EDAM_NOTEBOOK_NAME_LEN_MIN - EDAM_NOTEBOOK_NAME_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_NOTEBOOK_NAME_REGEX
 *   </dd>
 *
 * <dt>updateSequenceNum</dt>
 *   <dd>A number identifying the last transaction to
 *   modify the state of this object.  The USN values are sequential within an
 *   account, and can be used to compare the order of modifications within the
 *   service.
 *   </dd>
 *
 * <dt>defaultNotebook</dt>
 *   <dd>If true, this notebook should be used for new notes
 *   whenever the user has not (or cannot) specify a desired target notebook.
 *   For example, if a note is submitted via SMTP email.
 *   The service will maintain at most one defaultNotebook per account.
 *   If a second notebook is created or updated with defaultNotebook set to
 *   true, the service will automatically update the prior notebook's
 *   defaultNotebook field to false.  If the default notebook is deleted
 *   (i.e. "active" set to false), the "defaultNotebook" field will be
 *   set to false by the service.  If the account has no default notebook
 *   set, the service will use the most recent notebook as the default.
 *   </dd>
 *
 * <dt>serviceCreated</dt>
 *   <dd>The time when this notebook was created on the
 *   service. This will be set on the service during creation, and the service
 *   will provide this value when it returns a Notebook to a client.
 *   The service will ignore this value if it is sent by clients.
 *   </dd>
 *
 * <dt>serviceUpdated</dt>
 *   <dd>The time when this notebook was last modified on the
 *   service.  This will be set on the service during creation, and the service
 *   will provide this value when it returns a Notebook to a client.
 *   The service will ignore this value if it is sent by clients.
 *   </dd>
 *
 * <dt>publishing</dt>
 *   <dd>If the Notebook has been opened for public access, then this will point to the set of
 *   publishing information for the Notebook (URI, description, etc.). A Notebook cannot be
 *   published without providing this information, but it will persist for later use if publishing
 *   is ever disabled on the Notebook. Clients that do not wish to change the publishing behavior
 *   of a Notebook should not set this value when calling NoteStore.updateNotebook().
 *   Note that this structure is never populated for business notebooks, see the businessNotebook
 *   field.
 *   </dd>
 *
 * <dt>published</dt>
 *   <dd>If this is set to true, then the Notebook will be
 *   accessible either to the public, or for business users to their business,
 *   via the 'publishing' or 'businessNotebook' specifications, which must also be set. If this is
 *   set to false, the Notebook will not be available to the public (or business).
 *   Clients that do not wish to change the publishing behavior of a Notebook
 *   should not set this value when calling NoteStore.updateNotebook().
 *   </dd>
 *
 * <dt>stack</dt>
 *   <dd>If this is set, then the notebook is visually contained within a stack
 *   of notebooks with this name.  All notebooks in the same account with the
 *   same 'stack' field are considered to be in the same stack.
 *   Notebooks with no stack set are "top level" and not contained within a
 *   stack.
 *   </dd>
 *
 * <dt>sharedNotebookIds</dt>
 *   <dd><i>DEPRECATED</i> - replaced by sharedNotebooks.</dd>
 *
 * <dt>sharedNotebooks</dt>
 *   <dd>The list of recipients to whom this notebook has been shared
 *   (one SharedNotebook object per recipient email address). This field will
 *   be unset if you do not have permission to access this data. If you are
 *   accessing the notebook as the owner or via a shared notebook that is
 *   modifiable, then you have access to this data and the value will be set.
 *   This field is read-only. Clients may not make changes to shared notebooks
 *   via this field.
 *   </dd>
 *
 * <dt>businessNotebook</dt>
 *   <dd>If the notebook is part of a business account and has been shared with the entire
 *   business, this will contain sharing information. The presence or absence of this field
 *   is not a reliable test of whether a given notebook is in fact a business notebook - the
 *   field is only used when a notebook is or has been shared with the entire business.
 *   </dd>
 *
 * <dt>contact</dt>
 *   <dd>Intended for use with Business accounts, this field identifies the user who
 *   has been designated as the "contact".  For notebooks created in business
 *   accounts, the server will automatically set this value to the user who created
 *   the notebook unless Notebook.contact.username has been set, in which that value
 *   will be used.  When updating a notebook, it is common to leave Notebook.contact
 *   field unset, indicating that no change to the value is being requested and that
 *   the existing value, if any, should be preserved.
 *   </dd>
 *
 * <dt>recipientSettings</dt>
 *   <dd>This represents the preferences/settings that a recipient has set for this
 *   notebook. These are intended to be changed only by the recipient, and each
 *   recipient has their own recipient settings.
 *   </dd>
 * </dl>
 */
struct Notebook {
  1:  optional  Guid guid,
  2:  optional  string name,
  5:  optional  i32 updateSequenceNum,
  6:  optional  bool defaultNotebook,
  7:  optional  Timestamp serviceCreated,
  8:  optional  Timestamp serviceUpdated,
  10: optional  Publishing publishing,
  11: optional  bool published,
  12: optional  string stack,
  13: optional  list<i64> sharedNotebookIds,
  14: optional  list<SharedNotebook> sharedNotebooks,
  15: optional  BusinessNotebook businessNotebook,
  16: optional  User contact,
  17: optional  NotebookRestrictions restrictions,
  18: optional  NotebookRecipientSettings recipientSettings
}

/**
 * A link in a user's account that refers them to a public or
 * individual shared notebook in another user's account.
 *
 * <dl>
 * <dt>shareName</dt>
 * <dd>The display name of the shared notebook. The link owner can change this.</dd>
 *
 * <dt>username</dt>
 * <dd>The username of the user who owns the shared or public notebook.</dd>
 *
 * <dt>shardId</dt>
 * <dd>The shard ID of the notebook if the notebook is not public.</dt>
 *
 * <dt>uri</dt>
 * <dd>The identifier of the public notebook.</dd>
 *
 * <dt>guid</dt>
 *   <dd>The unique identifier of this linked notebook.  Will be set whenever
 *   a linked notebook is retrieved from the service, but may be null when a client
 *   is creating a linked notebook.
 *   <br/>
 *   Length:  EDAM_GUID_LEN_MIN - EDAM_GUID_LEN_MAX
 *   <br/>
 *   Regex:  EDAM_GUID_REGEX
 *   </dd>
 *
 * <dt>updateSequenceNum</dt>
 *   <dd>A number identifying the last transaction to
 *   modify the state of this object.  The USN values are sequential within an
 *   account, and can be used to compare the order of modifications within the
 *   service.
 *   </dd>
 *
 * <dt>noteStoreUrl</dt>
 *   <dd>
 *   This field will contain the full URL that clients should use to make
 *   NoteStore requests to the server shard that contains that notebook's data.
 *   I.e. this is the URL that should be used to create the Thrift HTTP client
 *   transport to send messages to the NoteStore service for the account.
 *   </dd>
 *
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
 *
 * <dt>stack</dt>
 *   <dd>If this is set, then the notebook is visually contained within a stack
 *   of notebooks with this name.  All notebooks in the same account with the
 *   same 'stack' field are considered to be in the same stack.
 *   Notebooks with no stack set are "top level" and not contained within a
 *   stack.  The link owner can change this and this field is for the benefit
 *   of the link owner.
 *   </dd>
 *
 * <dt>businessId</dt>
 *   <dd>If set, this will be the unique identifier for the business that owns
 *   the notebook to which the linked notebook refers.</dd>
 *
 * <dt>sharedNotebookGlobalId</dt>
 *   <dd>The globally unique identifier (globalId) of the shared notebook that
 *   corresponds to the share key, or the GUID of the Notebook that the linked notebook
 *   refers to. This field must be filled in with the SharedNotebook.globalId or
 *   Notebook.GUID value when creating new LinkedNotebooks. This field replaces the
 *   deprecated "shareKey" field.
 *   </dd>
 * </dl>
 */
struct LinkedNotebook {
  2:  optional string shareName,
  3:  optional string username,
  4:  optional string shardId,
  5:  optional string sharedNotebookGlobalId, // rename from shareKey
  6:  optional string uri,
  7:  optional Guid guid,
  8:  optional i32 updateSequenceNum,
  9:  optional string noteStoreUrl,
  10: optional string webApiUrlPrefix,
  11: optional string stack,
  12: optional i32 businessId
}

/**
 * A structure that describes a notebook or a user's relationship with
 * a notebook. NotebookDescriptor is expected to remain a lighter-weight
 * structure when compared to Notebook.
 * <dl>
 * <dt>guid</dt>
 *   <dd>The unique identifier of the notebook.
 *   </dd>
 *
 * <dt>notebookDisplayName</dt>
 *   <dd>A sequence of characters representing the name of the
 *   notebook.
 *   </dd>
 *
 * <dt>contactName</dt>
 *   <dd>The User.name value of the notebook's "contact".
 *   </dd>
 *
 * <dt>hasSharedNotebook</dt>
 *   <dd>Whether a SharedNotebook record exists between the calling user and this
 *   notebook.
 *   </dd>
 *
 * <dt>joinedUserCount</dt>
 *   <dd>The number of users who have joined this notebook.
 *   </dd>
 *
 * </dl>
 */
struct NotebookDescriptor {
  1: optional Guid guid,
  2: optional string notebookDisplayName,
  3: optional string contactName,
  4: optional bool hasSharedNotebook,
  5: optional i32 joinedUserCount
}

/**
 * This structure represents profile information for a user in a business.
 *
 * <dl>
 * <dt>id</dt>
 * <dd>The numeric identifier that uniquely identifies a user.</dd>
 *
 * <dt>name</dt>
 * <dd>The full name of the user.</dd>
 *
 * <dt>email</dt>
 * <dd>The user's business email address. If the user has not registered their business
 *   email address, this field will be empty.
 * </dd>
 *
 * <dt>username</dt>
 * <dd>The user's Evernote username.</dd>
 *
 * <dt>attributes</dt>
 * <dd>The user's business specific attributes.</dd>
 *
 * <dt>joined</dt>
 * <dd>The time when the user joined the business</dd>
 *
 * <dt>photoLastUpdated</dt>
 * <dd>The time when the user's profile photo was most recently updated</dd>
 *
 * <dt>photoUrl</dt>
 * <dd>A URL identifying a copy of the user's current profile photo</dd>
 *
 * <dt>role</dt>
 * <dd>The BusinessUserRole for the user</dd>
 *
 * <dt>status</dt>
 * <dd>The BusinessUserStatus for the user</dd>
 *
 * </dl>
 */
struct UserProfile {
  1: optional UserID id,
  2: optional string name,
  3: optional string email,
  4: optional string username,
  5: optional BusinessUserAttributes attributes,
  6: optional Timestamp joined
  7: optional Timestamp photoLastUpdated,
  8: optional string photoUrl,
  9: optional BusinessUserRole role,
  10: optional BusinessUserStatus status
}

/**
 * This enumeration defines the possible types of related content.
 *
 * NEWS_ARTICLE: This related content is a news article
 * PROFILE_PERSON: This match refers to the profile of an individual person
 * PROFILE_ORGANIZATION: This match refers to the profile of an organization
 * REFERENCE_MATERIAL: This related content is material from reference works
 */
enum RelatedContentType {
  NEWS_ARTICLE = 1,
  PROFILE_PERSON = 2,
  PROFILE_ORGANIZATION = 3,
  REFERENCE_MATERIAL = 4,
}

/**
 * This enumeration defines the possible ways to access related content.
 *
 * NOT_ACCESSIBLE: The content is not accessible given the user's privilege level, but
 *     still worth showing as a snippet. The content url may point to a webpage that
 *     explains why not, or explains how to access that content.
 *
 * DIRECT_LINK_ACCESS_OK: The content is accessible directly, and no additional login is
 *     required.
 *
 * DIRECT_LINK_LOGIN_REQUIRED: The content is accessible directly, but an additional login
 *     is required.
 *
 * DIRECT_LINK_EMBEDDED_VIEW: The content is accessible directly, and should be shown in
 *     an embedded web view.
 *     If the URL refers to a secured location under our control (for example,
 *     https://www.evernote.com/*), the client may include user-specific authentication
 *     credentials with the request.
 */
enum RelatedContentAccess {
  NOT_ACCESSIBLE = 0,
  DIRECT_LINK_ACCESS_OK = 1,
  DIRECT_LINK_LOGIN_REQUIRED = 2,
  DIRECT_LINK_EMBEDDED_VIEW = 3,
}

/**
 * An external image that can be shown with a related content snippet,
 * usually either a JPEG or PNG image. It is up to the client which image(s) are shown,
 * depending on available screen real estate, resolution and aspect ratio.
 *
 * <dl>
 *  <dt>url</dt>
 *    <dd>The external URL of the image</dd>
 *  <dt>width</dt>
 *    <dd>The width of the image, in pixels.</dd>
 *  <dt>height</dt>
 *    <dd>The height of the image, in pixels.</dd>
 *  <dt>pixelRatio</dt>
 *    <dd>the pixel ratio (usually either 1.0, 1.5 or 2.0)</dd>
 *  <dt>fileSize</dt>
 *    <dd>the size of the image file, in bytes</dd>
 * </dl>
 */
struct RelatedContentImage {
  1: optional string url,
  2: optional i32 width,
  3: optional i32 height,
  4: optional double pixelRatio,
  5: optional i32 fileSize
}

/**
 * A structure identifying one snippet of related content (some information that is not
 * part of an Evernote account but might still be relevant to the user).
 *
 * <dl>
 *
 * <dt>contentId</dt>
 * <dd>An identifier that uniquely identifies the content.</dd>
 *
 * <dt>title</dt>
 * <dd>The main title to show.</dd>
 *
 * <dt>url</dt>
 * <dd>The URL the client can use to retrieve the content.</dd>
 *
 * <dt>sourceId</dt>
 * <dd>An identifier that uniquely identifies the source.</dd>
 *
 * <dt>sourceUrl</dt>
 * <dd>A URL the client can access to know more about the source.</dd>
 *
 * <dt>sourceFaviconUrl</dt>
 * <dd>The favicon URL of the source which the content belongs to.</dd>
 * </dl>
 *
 * <dt>sourceName</dt>
 * <dd>A human-readable name of the source that provided this content.</dd>
 *
 * <dt>date</dt>
 * <dd>A timestamp telling the user about the recency of the content.</dd>
 *
 * <dt>teaser</dt>
 * <dd>A teaser text to show to the user; usually the first few sentences of the content,
 *     excluding the title.</dd>
 *
 * <dt>thumbnails</dt>
 * <dd>A list of thumbnails the client can show in the snippet.</dd>
 *
 * <dt>contentType</dt>
 * <dd>The type of this related content.</dd>
 *
 * <dt>accessType</dt>
 * <dd>An indication of how this content can be accessed. This type influences the
 *     semantics of the <code>url</code> parameter.</dd>
 *
 * <dt>visibleUrl</dt>
 * <dd>If set, the client should show this URL to the user, instead of the URL that was
 *     used to retrieve the content. This URL should be used when opening the content
 *     in an external browser window, or when sharing with another person.</dd>
 *
 * <dt>clipUrl</dt>
 * <dd>If set, the client should use this URL for clipping purposes, instead of the URL
 *     that was used to retrieve the content. The clipUrl may directly point to an .enex
 *     file, for example.</dd>
 *
 * <dt>contact</dt>
 * <dd>If set, the client may use this Contact for messaging purposes. This will typically
 *     only be set for user profiles.</dd>
 *
 * <dt>authors</dt>
 * <dd>For News articles only. A list of names of the article authors, if available.</dd>
 *
 * </dl>
 */
struct RelatedContent {
  1: optional string contentId,
  2: optional string title,
  3: optional string url,
  4: optional string sourceId,
  5: optional string sourceUrl,
  6: optional string sourceFaviconUrl,
  7: optional string sourceName,
  8: optional Timestamp date,
  9: optional string teaser,
  10: optional list<RelatedContentImage> thumbnails,
  11: optional RelatedContentType contentType,
  12: optional RelatedContentAccess accessType,
  13: optional string visibleUrl,
  14: optional string clipUrl,
  15: optional Contact contact,
  16: optional list<string> authors,
}

/**
 * A structure describing an invitation to join a business account.
 *
 * <dl>
 *   <dt>businessId</dt>
 *     <dd>
 *       The ID of the business to which the invitation grants access.
 *     </dd>
 *
 *   <dt>email</dt>
 *     <dd>
 *       The email address that was invited to join the business.
 *     </dd>
 *
 *   <dt>role</dt>
 *     <dd>
 *       The role to grant the user after the invitation is accepted.
 *     </dd>
 *
 *   <dt>status</dt>
 *     <dd>
 *       The status of the invitation.
 *     </dd>
 *
 *   <dt>requesterId</dt>
 *     <dd>
 *       For invitations that were initially requested by a non-admin member of the business,
 *       this field specifies the user ID of the requestor. For all other invitations, this field
 *       will be unset.
 *     </dd>
 *   <dt>fromWorkChat</dt>
 *     <dd>
 *       If this invitation was created implicitly via a WorkChat, this field
 *       will be true.
 *     </dd>
 *   <dt>created</dt>
 *     <dd>
 *       The timestamp at which this invitation was created.
 *     </dd>
 *   <dt>mostRecentReminder</dt>
 *     <dd>
 *       The timestamp at which the most recent reminder was sent.
 *     </dd>
 * </dl>
 */
struct BusinessInvitation {
  1: optional i32 businessId,
  2: optional string email,
  3: optional BusinessUserRole role,
  4: optional BusinessInvitationStatus status,
  5: optional UserID requesterId,
  6: optional bool fromWorkChat,
  7: optional Timestamp created,
  8: optional Timestamp mostRecentReminder
}

/**
 *
 */
enum UserIdentityType {
  EVERNOTE_USERID = 1,
  EMAIL = 2,
  IDENTITYID = 3
}

/**
 * A structure that holds user identifying information such as an
 * email address, Evernote user ID, or an identifier from a 3rd party
 * service.  An instance consists of a type and a value, where the
 * value will be stored in one of the value fields depending upon the
 * data type required for the identity type.
 *
 * When used with shared notebook invitations, a UserIdentity
 * identifies a particular person who may not (yet) have an Evernote
 * UserID UserIdentity but who has (almost) unique access to the
 * service endpoint described by the UserIdentity.  For example, an
 * e-mail UserIdentity can identify the person who receives e-mail at
 * the given address, and who can therefore read the share key that
 * has a cryptographic signature from the Evernote service. With the
 * share key, this person can supply their Evernote UserID via an
 * authentication token to join the notebook
 * (authenticateToSharedNotebook), at which time we have associated
 * the e-mail UserIdentity with an Evernote UserID UserIdentity. Note
 * that using shared notebook records, the relationship between
 * Evernote UserIDs and e-mail addresses is many to many.
 *
 * Note that the identifier may not directly identify a
 * particular Evernote UserID UserIdentity without further
 * verification.  For example, an e-mail UserIdentity may be
 * associated with an invitation to join a notebook (via a shared
 * notebook record), but until a user uses a share key, that was sent
 * to that e-mail address, to join the notebook, we do not know an
 * Evernote UserID UserIdentity ID to match the e-mail address.
 */
struct UserIdentity {
  1: optional UserIdentityType type,
  2: optional string stringIdentifier,
  3: optional i64 longIdentifier
}
