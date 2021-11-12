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
 * This file contains the EDAM protocol interface for operations to access and modify the contents
 * of an Evernote user account such as Notes, Notebooks and Tags.
 */

include "UserStore.thrift"
include "Types.thrift"
include "Errors.thrift"
include "Limits.thrift"

namespace as3 com.evernote.edam.notestore
namespace java com.evernote.edam.notestore
namespace csharp Evernote.EDAM.NoteStore
namespace py evernote.edam.notestore
namespace cpp evernote.edam
namespace rb Evernote.EDAM.NoteStore
namespace php EDAM.NoteStore
namespace cocoa EDAM
namespace perl EDAMNoteStore
namespace go edam

/**
 * This structure encapsulates the information about the state of the
 * user's account for the purpose of "state based" synchronization.
 * <dl>
 * <dt>currentTime</dt>
 *   <dd>
 *   The server's current date and time.
 *   </dd>
 * <dt>fullSyncBefore</dt>
 *   <dd>
 *   The cutoff date and time for client caches to be
 *   updated via incremental synchronization.  Any clients that were last
 *   synched with the server before this date/time must do a full resync of all
 *   objects.  This cutoff point will change over time as archival data is
 *   deleted or special circumstances on the service require resynchronization.
 *   </dd>
 * <dt>updateCount</dt>
 *   <dd>
 *   Indicates the total number of transactions that have
 *   been committed within the account.  This reflects (for example) the
 *   number of discrete additions or modifications that have been made to
 *   the data in this account (tags, notes, resources, etc.).
 *   This number is the "high water mark" for Update Sequence Numbers (USN)
 *   within the account.
 *   </dd>
 * <dt>uploaded</dt>
 *   <dd>
 *   The total number of bytes that have been uploaded to
 *   this account in the current monthly period.  This can be compared against
 *   Accounting.uploadLimit (from the UserStore) to determine how close the user
 *   is to their monthly upload limit.
 *   This value may not be present if the SyncState has been retrieved by
 *   a caller that only has read access to the account.
 *   </dd>
 * <dt>userLastUpdated</dt>
 *   <dd>
 *   The last time when a user's account level information was changed. This value
 *   is the latest time when a modification was made to any of the following:
 *   accounting information (billing, quota, premium status, etc.), user attributes
 *   and business user information (business name, business user attributes, etc.) if
 *   the user is in a business.
 *   Clients who need to maintain account information about a User should watch this
 *   field for updates rather than polling UserStore.getUser for updates. Here is the
 *   basic flow that clients should follow:
 *   <ol>
 *     <li>Call NoteStore.getSyncState to retrieve the SyncState object</li>
 *     <li>Compare SyncState.userLastUpdated to previously stored value:
 *         if (SyncState.userLastUpdated > previousValue)
 *           call UserStore.getUser to get the latest User object;
 *         else
 *           do nothing;</li>
 *     <li>Update previousValue = SyncState.userLastUpdated</li>
 *   </ol>
 *   </dd>
 * <dt>userMaxMessageEventId</dt>
 *   <dd>
 *   The greatest MessageEventID for this user's account. Clients that do a full
 *   sync should store this value locally and compare their local copy to the
 *   value returned by getSyncState to determine if they need to sync with
 *   MessageStore. This value will be omitted if the user has never sent or
 *   received a message.
 *   </dd>
 * </dl>
 */
struct SyncState {
  1:  required  Types.Timestamp currentTime,
  2:  required  Types.Timestamp fullSyncBefore,
  3:  required  i32 updateCount,
  4:  optional  i64 uploaded,
  5:  optional  Types.Timestamp userLastUpdated,
  6:  optional  Types.MessageEventID userMaxMessageEventId,
}

/**
 * This structure is given out by the NoteStore when a client asks to
 * receive the current state of an account.  The client asks for the server's
 * state one chunk at a time in order to allow clients to retrieve the state
 * of a large account without needing to transfer the entire account in
 * a single message.
 *
 * The server always gives SyncChunks using an ascending series of Update
 * Sequence Numbers (USNs).
 *
 *<dl>
 * <dt>currentTime</dt>
 *   <dd>
 *   The server's current date and time.
 *   </dd>
 *
 * <dt>chunkHighUSN</dt>
 *   <dd>
 *   The highest USN for any of the data objects represented
 *   in this sync chunk.  If there are no objects in the chunk, this will not be
 *   set.
 *   </dd>
 *
 * <dt>updateCount</dt>
 *   <dd>
 *   The total number of updates that have been performed in
 *   the service for this account.  This is equal to the highest USN within the
 *   account at the point that this SyncChunk was generated.  If updateCount
 *   and chunkHighUSN are identical, that means that this is the last chunk
 *   in the account ... there is no more recent information.
 *   </dd>
 *
 * <dt>notes</dt>
 *   <dd>
 *   If present, this is a list of non-expunged notes that
 *   have a USN in this chunk.  This will include notes that are "deleted"
 *   but not expunged (i.e. in the trash).  The notes will include their list
 *   of tags and resources, but the note content, resource content, resource
 *   recognition data and resource alternate data will not be supplied.
 *   </dd>
 *
 * <dt>notebooks</dt>
 *   <dd>
 *   If present, this is a list of non-expunged notebooks that
 *   have a USN in this chunk.
 *   </dd>
 *
 * <dt>tags</dt>
 *   <dd>
 *   If present, this is a list of the non-expunged tags that have a
 *   USN in this chunk.
 *   </dd>
 *
 * <dt>searches</dt>
 *   <dd>
 *   If present, this is a list of non-expunged searches that
 *   have a USN in this chunk.
 *   </dd>
 *
 * <dt>resources</dt>
 *   <dd>
 *   If present, this is a list of the non-expunged resources
 *   that have a USN in this chunk.  This will include the metadata for each
 *   resource, but not its binary contents or recognition data, which must be
 *   retrieved separately.
 *   </dd>
 *
 * <dt>expungedNotes</dt>
 *   <dd>
 *   If present, the GUIDs of all of the notes that were
 *   permanently expunged in this chunk.
 *   </dd>
 *
 * <dt>expungedNotebooks</dt>
 *   <dd>
 *   If present, the GUIDs of all of the notebooks that
 *   were permanently expunged in this chunk.  When a notebook is expunged,
 *   this implies that all of its child notes (and their resources) were
 *   also expunged.
 *   </dd>
 *
 * <dt>expungedTags</dt>
 *   <dd>
 *   If present, the GUIDs of all of the tags that were
 *   permanently expunged in this chunk.
 *   </dd>
 *
 * <dt>expungedSearches</dt>
 *   <dd>
 *   If present, the GUIDs of all of the saved searches
 *   that were permanently expunged in this chunk.
 *   </dd>
 *
 * <dt>linkedNotebooks</dt>
 *   <dd>
 *   If present, this is a list of non-expunged LinkedNotebooks that
 *   have a USN in this chunk.
 *   </dd>
 *
 * <dt>expungedLinkedNotebooks</dt>
 *   <dd>
 *   If present, the GUIDs of all of the LinkedNotebooks
 *   that were permanently expunged in this chunk.
 *   </dd>
 */
struct SyncChunk {
  1:  required  Types.Timestamp currentTime,
  2:  optional  i32 chunkHighUSN,
  3:  required  i32 updateCount,
  4:  optional  list<Types.Note> notes,
  5:  optional  list<Types.Notebook> notebooks,
  6:  optional  list<Types.Tag> tags,
  7:  optional  list<Types.SavedSearch> searches,
  8:  optional  list<Types.Resource> resources,
  9:  optional  list<Types.Guid> expungedNotes,
  10: optional  list<Types.Guid> expungedNotebooks,
  11: optional  list<Types.Guid> expungedTags,
  12: optional  list<Types.Guid> expungedSearches,
  13: optional  list<Types.LinkedNotebook> linkedNotebooks,
  14: optional  list<Types.Guid> expungedLinkedNotebooks
}

/**
 * This structure is used with the 'getFilteredSyncChunk' call to provide
 * fine-grained control over the data that's returned when a client needs
 * to synchronize with the service. Each flag in this structure specifies
 * whether to include one class of data in the results of that call.
 *
 *<dl>
 * <dt>includeNotes</dt>
 *   <dd>
 *   If true, then the server will include the SyncChunks.notes field
 *   </dd>
 *
 * <dt>includeNoteResources</dt>
 *   <dd>
 *   If true, then the server will include the 'resources' field on all of
 *   the Notes that are in SyncChunk.notes.
 *   If 'includeNotes' is false, then this will have no effect.
 *   </dd>
 *
 * <dt>includeNoteAttributes</dt>
 *   <dd>
 *   If true, then the server will include the 'attributes' field on all of
 *   the Notes that are in SyncChunks.notes.
 *   If 'includeNotes' is false, then this will have no effect.
 *   </dd>
 *
 * <dt>includeNotebooks</dt>
 *   <dd>
 *   If true, then the server will include the SyncChunks.notebooks field
 *   </dd>
 *
 * <dt>includeTags</dt>
 *   <dd>
 *   If true, then the server will include the SyncChunks.tags field
 *   </dd>
 *
 * <dt>includeSearches</dt>
 *   <dd>
 *   If true, then the server will include the SyncChunks.searches field
 *   </dd>
 *
 * <dt>includeResources</dt>
 *   <dd>
 *   If true, then the server will include the SyncChunks.resources field.
 *   Since the Resources are also provided with their Note
 *   (in the Notes.resources list), this is primarily useful for clients that
 *   want to watch for changes to individual Resources due to recognition data
 *   being added.
 *   </dd>
 *
 * <dt>includeLinkedNotebooks</dt>
 *   <dd>
 *   If true, then the server will include the SyncChunks.linkedNotebooks field.
 *   </dd>
 *
 * <dt>includeExpunged</dt>
 *   <dd>
 *   If true, then the server will include the 'expunged' data for any type
 *   of included data.  For example, if 'includeTags' and 'includeExpunged'
 *   are both true, then the SyncChunks.expungedTags field will be set with
 *   the GUIDs of tags that have been expunged from the server.
 *   </dd>
 *
 * <dt>includeNoteApplicationDataFullMap</dt>
 *   <dd>
 *   If true, then the values for the applicationData map will be filled
 *   in, assuming notes and note attributes are being returned.  Otherwise,
 *   only the keysOnly field will be filled in.
 *   </dd>
 *
 * <dt>includeResourceApplicationDataFullMap</dt>
 *   <dd>
 *   If true, then the fullMap values for the applicationData map will be
 *   filled in, assuming resources and resource attributes are being returned
 *   (includeResources is true).  Otherwise, only the keysOnly field will be
 *   filled in.
 *   </dd>
 *
 * <dt>includeNoteResourceApplicationDataFullMap</dt>
 *   <dd>
 *   If true, then the fullMap values for the applicationData map will be
 *   filled in for resources found inside of notes, assuming resources are
 *   being returned in notes (includeNoteResources is true).  Otherwise,
 *   only the keysOnly field will be filled in.
 *   </dd>
 *
 * <dt>omitSharedNotebooks<dt>
 *   <dd>
 *   Normally, if 'includeNotebooks' is true, then the SyncChunks will
 *   include Notebooks that may include a set of SharedNotebook
 *   invitations via Notebook.sharedNotebookIds and Notebook.sharedNotebooks.
 *   However, if omitSharedNotebooks is set to true, then the Notebooks
 *   will omit those two fields and leave them unset. This should be used
 *   by clients who want to know their own set of Notebooks (and the
 *   associated permissions via Notebook.recipientSettings), and who
 *   do not need to know the full set of other people who can also see
 *   that same notebook.
 *   </dd>
 *
 * <dt>requireNoteContentClass</dt>
 *   <dd>
 *   If set, then only send notes whose content class matches this value.
 *   The value can be a literal match or, if the last character is an
 *   asterisk, a prefix match.
 *   </dd>
 *
 * <dt>notebookGuids</dt>
 *   <dd>
 *   If set, then restrict the returned notebooks, notes, and
 *   resources to those associated with one of the notebooks whose
 *   GUID is provided in this list.  If not set, then no filtering on
 *   notebook GUID will be performed.  If you set this field, you may
 *   not also set includeExpunged else an EDAMUserException with an
 *   error code of DATA_CONFLICT will be thrown.  You only need to set
 *   this field if you want to restrict the returned entities more
 *   than what your authentication token allows you to access.  For
 *   example, there is no need to set this field for single notebook
 *   tokens such as for shared notebooks.  You can use this field to
 *   synchronize a newly discovered business notebook while
 *   incrementally synchronizing a business account, in which case you
 *   will only need to consider setting includeNotes,
 *   includeNotebooks, includeNoteAttributes, includeNoteResources,
 *   and maybe some of the "FullMap" fields.
 *   </dd>
 *
 * <dt>includeSharedNotes</dt>
 *   <dd>
 *   If true, then the service will include the sharedNotes field on all
 *   notes that are in SyncChunk.notes. If 'includeNotes' is false, then
 *   this will have no effect.
 *   </dd>
 * </dl>
 */
struct SyncChunkFilter {
  1:  optional  bool includeNotes,
  2:  optional  bool includeNoteResources,
  3:  optional  bool includeNoteAttributes,
  4:  optional  bool includeNotebooks,
  5:  optional  bool includeTags,
  6:  optional  bool includeSearches,
  7:  optional  bool includeResources,
  8:  optional  bool includeLinkedNotebooks,
  9:  optional  bool includeExpunged,
  10: optional  bool includeNoteApplicationDataFullMap,
  12: optional  bool includeResourceApplicationDataFullMap,
  13: optional  bool includeNoteResourceApplicationDataFullMap,
  17: optional  bool includeSharedNotes,
  16: optional  bool omitSharedNotebooks,
  11: optional  string requireNoteContentClass,
  15: optional  set<string> notebookGuids
}


/**
 * A list of criteria that are used to indicate which notes are desired from
 * the account.  This is used in queries to the NoteStore to determine
 * which notes should be retrieved.
 *
 *<dl>
 * <dt>order</dt>
 *   <dd>
 *   The NoteSortOrder value indicating what criterion should be
 *   used to sort the results of the filter.
 *   </dd>
 *
 * <dt>ascending</dt>
 *   <dd>
 *   If true, the results will be ascending in the requested
 *   sort order.  If false, the results will be descending.
 *   </dd>
 *
 * <dt>words</dt>
 *   <dd>
 *   If present, a search query string that will filter the set of notes to be returned.
 *   Accepts the full search grammar documented in the Evernote API Overview.
 *   </dd>
 *
 * <dt>notebookGuid</dt>
 *   <dd>
 *   If present, the Guid of the notebook that must contain
 *   the notes.
 *   </dd>
 *
 * <dt>tagGuids</dt>
 *   <dd>
 *   If present, the list of tags (by GUID) that must be present
 *   on the notes.
 *   </dd>
 *
 * <dt>timeZone</dt>
 *   <dd>
 *   The zone ID for the user, which will be used to interpret
 *   any dates or times in the queries that do not include their desired zone
 *   information.
 *   For example, if a query requests notes created "yesterday", this
 *   will be evaluated from the provided time zone, if provided.
 *   The format must be encoded as a standard zone ID such as
 *   "America/Los_Angeles".
 *   </dd>
 *
 * <dt>inactive</dt>
 *   <dd>
 *   If true, then only notes that are not active (i.e. notes in
 *   the Trash) will be returned. Otherwise, only active notes will be returned.
 *   There is no way to find both active and inactive notes in a single query.
 *   </dd>
 *
 * <dt>emphasized</dt>
 *   <dd>
 *   If present, a search query string that may or may not influence the notes
 *   to be returned, both in terms of coverage as well as of order. Think of it
 *   as a wish list, not a requirement.
 *   Accepts the full search grammar documented in the Evernote API Overview.
 *   </dd>
 *
 * <dt>includeAllReadableNotebooks</dt>
 *   <dd>
 *   If true, then the search will include all business notebooks that are readable
 *   by the user. A business authentication token must be supplied for
 *   this option to take effect when calling search APIs.
 *   </dd>
 *
 * <dt>includeAllReadableWorkspaces</dt>
 *   <dd>
 *   If true, then the search will include all workspaces that are readable
 *   by the user. A business authentication token must be supplied for
 *   this option to take effect when calling search APIs.
 *   </dd>
 *
 * <dt>context</dt>
 *   <dd>
 *   Specifies the context to consider when determining result ranking.
 *   Clients must leave this value unset unless they wish to explicitly specify a known
 *   non-default context.
 *   </dd>
 *
 * <dt>rawWords</dt>
 *   <dd>
 *   If present, the raw user query input.
 *   Accepts the full search grammar documented in the Evernote API Overview.
 *   </dd>
 *
 * <dt>searchContextBytes</dt>
 *   <dd>
 *   Specifies the correlating information about the current search session, in byte array.
 *   If this request is not for the first page of search results, the client should populate
 *   this field with the value of searchContextBytes from the NotesMetadataList of the
 *   original search response.
 *   </dd>
 * </dl>
 */
struct NoteFilter {
  // 1: optional  Types.NoteSortOrder order,
  1: optional  i32 order,  // Should be one of the NoteSortOrder values
  2: optional  bool ascending,
  3: optional  string words,
  4: optional  Types.Guid notebookGuid,
  5: optional  list<Types.Guid> tagGuids,
  6: optional  string timeZone,
  7: optional  bool inactive,
  8: optional  string emphasized,
  9: optional  bool includeAllReadableNotebooks,
  15: optional bool includeAllReadableWorkspaces,
  10: optional string context,
  11: optional string rawWords,
  12: optional binary searchContextBytes,
}

/**
 * A small structure for returning a list of notes out of a larger set.
 *
 *<dl>
 * <dt>startIndex</dt>
 *   <dd>
 *   The starting index within the overall set of notes.  This
 *   is also the number of notes that are "before" this list in the set.
 *   </dd>
 *
 * <dt>totalNotes</dt>
 *   <dd>
 *   The number of notes in the larger set.  This can be used
 *   to calculate how many notes are "after" this note in the set.
 *   (I.e.  remaining = totalNotes - (startIndex + notes.length)  )
 *   </dd>
 *
 * <dt>notes</dt>
 *   <dd>
 *   The list of notes from this range.  The Notes will include all
 *   metadata (attributes, resources, etc.), but will not include the ENML
 *   content of the note or the binary contents of any resources.
 *   </dd>
 *
 * <dt>stoppedWords</dt>
 *   <dd>
 *   If the NoteList was produced using a text based search
 *   query that included words that are not indexed or searched by the service,
 *   this will include a list of those ignored words.
 *   </dd>
 *
 * <dt>searchedWords</dt>
 *   <dd>
 *   If the NoteList was produced using a text based search
 *   query that included viable search words or quoted expressions, this will
 *   include a list of those words.  Any stopped words will not be included
 *   in this list.
 *   </dd>
 *
 * <dt>updateCount</dt>
 *   <dd>
 *   Indicates the total number of transactions that have
 *   been committed within the account.  This reflects (for example) the
 *   number of discrete additions or modifications that have been made to
 *   the data in this account (tags, notes, resources, etc.).
 *   This number is the "high water mark" for Update Sequence Numbers (USN)
 *   within the account.
 *   </dd>
 *
 * <dt>searchContextBytes</dt>
 *   <dd>
 *   Specifies the correlating information about the current search session, in byte array.
 *   </dd>
 * </dl>
 *
 * <dt>debugInfo</dt>
 *   <dd>
 *   Depends on the value of <code>context</code> in NoteFilter, this field
 *   may contain debug information if the service decides to do so.
 *   </dd>
 *
 */
struct NoteList {
  1: required  i32 startIndex,
  2: required  i32 totalNotes,
  3: required  list<Types.Note> notes,
  4: optional  list<string> stoppedWords,
  5: optional  list<string> searchedWords,
  6: optional  i32 updateCount,
  7: optional  binary searchContextBytes,
  8: optional  string debugInfo
}

/**
 * This structure is used in the set of results returned by the
 * findNotesMetadata function.  It represents the high-level information about
 * a single Note, without some of the larger deep structure.  This allows
 * for the information about a list of Notes to be returned relatively quickly
 * with less marshalling and data transfer to remote clients.
 * Most fields in this structure are identical to the corresponding field in
 * the Note structure, with the exception of:
 *
 * <dl>
 * <dt>largestResourceMime</dt>
 *   <dd>If set, then this will contain the MIME type of the largest Resource
 *   (in bytes) within the Note.  This may be useful, for example, to choose
 *   an appropriate icon or thumbnail to represent the Note.
 *   </dd>
 *
 * <dt>largestResourceSize</dt>
 *  <dd>If set, this will contain the size of the largest Resource file, in
 *  bytes, within the Note.  This may be useful, for example, to decide whether
 *  to ask the server for a thumbnail to represent the Note.
 *  </dd>
 * </dl>
 */
struct NoteMetadata {
  1:  required  Types.Guid guid,
  2:  optional  string title,
  5:  optional  i32 contentLength,
  6:  optional  Types.Timestamp created,
  7:  optional  Types.Timestamp updated,
  8:  optional  Types.Timestamp deleted,
  10: optional  i32 updateSequenceNum,
  11: optional  string notebookGuid,
  12: optional  list<Types.Guid> tagGuids,
  14: optional  Types.NoteAttributes attributes,
  20: optional  string largestResourceMime,
  21: optional  i32 largestResourceSize
}

/**
 * This structure is returned from calls to the findNotesMetadata function to
 * give the high-level metadata about a subset of Notes that are found to
 * match a specified NoteFilter in a search.
 *
 *<dl>
 * <dt>startIndex</dt>
 *   <dd>
 *   The starting index within the overall set of notes.  This
 *   is also the number of notes that are "before" this list in the set.
 *   </dd>
 *
 * <dt>totalNotes</dt>
 *   <dd>
 *   The number of notes in the larger set.  This can be used
 *   to calculate how many notes are "after" this note in the set.
 *   (I.e.  remaining = totalNotes - (startIndex + notes.length)  )
 *   </dd>
 *
 * <dt>notes</dt>
 *   <dd>
 *   The list of metadata for Notes in this range.  The set of optional fields
 *   that are set in each metadata structure will depend on the
 *   NotesMetadataResultSpec provided by the caller when the search was
 *   performed.  Only the 'guid' field will be guaranteed to be set in each
 *   Note.
 *   </dd>
 *
 * <dt>stoppedWords</dt>
 *   <dd>
 *   If the NoteList was produced using a text based search
 *   query that included words that are not indexed or searched by the service,
 *   this will include a list of those ignored words.
 *   </dd>
 *
 * <dt>searchedWords</dt>
 *   <dd>
 *   If the NoteList was produced using a text based search
 *   query that included viable search words or quoted expressions, this will
 *   include a list of those words.  Any stopped words will not be included
 *   in this list.
 *   </dd>
 *
 * <dt>updateCount</dt>
 *   <dd>
 *   Indicates the total number of transactions that have
 *   been committed within the account.  This reflects (for example) the
 *   number of discrete additions or modifications that have been made to
 *   the data in this account (tags, notes, resources, etc.).
 *   This number is the "high water mark" for Update Sequence Numbers (USN)
 *   within the account.
 *   </dd>
 *
 * <dt>searchContextBytes</dt>
 *   <dd>
 *   Specifies the correlating information about the current search session, in byte array.
 *   </dd>
 *
 * <dt>debugInfo</dt>
 *   <dd>
 *   Depends on the value of <code>context</code> in NoteFilter, this field
 *   may contain debug information if the service decides to do so.
 *   </dd>
 *
 * </dl>
 */
struct NotesMetadataList {
  1:  required  i32 startIndex,
  2:  required  i32 totalNotes,
  3:  required  list<NoteMetadata> notes,
  4:  optional  list<string> stoppedWords,
  5:  optional  list<string> searchedWords,
  6:  optional  i32 updateCount,
  7:  optional  binary searchContextBytes,
  9:  optional  string debugInfo
}

/**
 * This structure is provided to the findNotesMetadata function to specify
 * the subset of fields that should be included in each NoteMetadata element
 * that is returned in the NotesMetadataList.
 * Each field on this structure is a boolean flag that indicates whether the
 * corresponding field should be included in the NoteMetadata structure when
 * it is returned.  For example, if the 'includeTitle' field is set on this
 * structure when calling findNotesMetadata, then each NoteMetadata in the
 * list should have its 'title' field set.
 * If one of the fields in this spec is not set, then it will be treated as
 * 'false' by the server, so the default behavior is to include nothing in
 * replies (but the mandatory GUID)
 */
struct NotesMetadataResultSpec {
  2:  optional  bool includeTitle,
  5:  optional  bool includeContentLength,
  6:  optional  bool includeCreated,
  7:  optional  bool includeUpdated,
  8:  optional  bool includeDeleted,
  10: optional  bool includeUpdateSequenceNum,
  11: optional  bool includeNotebookGuid,
  12: optional  bool includeTagGuids,
  14: optional  bool includeAttributes,
  20: optional  bool includeLargestResourceMime,
  21: optional  bool includeLargestResourceSize
}

/**
 * A data structure representing the number of notes for each notebook
 * and tag with a non-zero set of applicable notes.
 *
 *<dl>
 * <dt>notebookCounts</dt>
 *   <dd>
 *   A mapping from the Notebook GUID to the number of
 *   notes (from some selection) that are in the corresponding notebook.
 *   </dd>
 *
 * <dt>tagCounts</dt>
 *   <dd>
 *   A mapping from the Tag GUID to the number of notes (from some
 *   selection) that have the corresponding tag.
 *   </dd>
 *
 * <dt>trashCount</dt>
 *   <dd>
 *   If this is set, then this is the number of notes that are in the trash.
 *   If this is not set, then the number of notes in the trash hasn't been
 *   reported.  (I.e. if there are no notes in the trash, this will be set
 *   to 0.)
 *   </dd>
 * </dl>
 */
struct NoteCollectionCounts {
  1: optional  map<Types.Guid, i32> notebookCounts,
  2: optional  map<Types.Guid, i32> tagCounts,
  3: optional  i32 trashCount
}

/**
 * This structure is provided to the getNoteWithResultSpec function to specify the subset of
 * fields that should be included in the Note that is returned. This allows clients to request
 * the minimum set of information that they require when retrieving a note, reducing the size
 * of the response and improving the response time.
 *
 * If one of the fields in this spec is not set, then it will be treated as 'false' by the service,
 * so that the default behavior is to include none of the fields below in the Note.
 *
 * <dl>
 *   <dt>includeContent</dt>
 *   <dd>If true, the Note.content field will be populated with the note's ENML contents.</dd>
 *
 *   <dt>includeResourcesData</dt>
 *   <dd>If true, any Resource elements will include the binary contents of their 'data' field's
 *     body.</dd>
 *
 *   <dt>includeResourcesRecognition</dt>
 *   <dd>If true, any Resource elements will include the binary contents of their 'recognition'
 *     field's body if recognition data is available.</dd>
 *
 *   <dt>includeResourcesAlternateData</dt>
 *   <dd>If true, any Resource elements will include the binary contents of their 'alternateData'
 *     field's body, if an alternate form is available.</dd>
 *
 *   <dt>includeSharedNotes</dt>
 *   <dd>If true, the Note.sharedNotes field will be populated with the note's shares.</dd>
 *
 *   <dt>includeNoteAppDataValues</dt>
 *   <dd>If true, the Note.attributes.applicationData.fullMap field will be populated.</dd>
 *
 *   <dt>includeResourceAppDataValues</dt>
 *   <dd>If true, the Note.resource.attributes.applicationData.fullMap field will be populated.</dd>
 *
 *   <dt>includeAccountLimits</dt>
 *   <dd>If true, the Note.limits field will be populated with the note owner's account limits.</dd>
 * </dl>
 */
struct NoteResultSpec {
  1: optional bool includeContent,
  2: optional bool includeResourcesData,
  3: optional bool includeResourcesRecognition,
  4: optional bool includeResourcesAlternateData,
  5: optional bool includeSharedNotes,
  6: optional bool includeNoteAppDataValues,
  7: optional bool includeResourceAppDataValues,
  8: optional bool includeAccountLimits
}

/**
 * Parameters that must be given to the NoteStore emailNote call. These allow
 * the caller to specify the note to send, the recipient addresses, etc.
 *
 * <dl>
 *  <dt>guid</dt>
 *    <dd>
 *      If set, this must be the GUID of a note within the user's account that
 *      should be retrieved from the service and sent as email.  If not set,
 *      the 'note' field must be provided instead.
 *    </dd>
 *
 *  <dt>note</dt>
 *    <dd>
 *      If the 'guid' field is not set, this field must be provided, including
 *      the full contents of the note note (and all of its Resources) to send.
 *      This can be used for a Note that as not been created in the service,
 *      for example by a local client with local notes.
 *    </dd>
 *
 *  <dt>toAddresses</dt>
 *    <dd>
 *      If provided, this should contain a list of the SMTP email addresses
 *      that should be included in the "To:" line of the email.
 *      Callers must specify at least one "to" or "cc" email address.
 *    </dd>
 *
 *  <dt>ccAddresses</dt>
 *    <dd>
 *      If provided, this should contain a list of the SMTP email addresses
 *      that should be included in the "Cc:" line of the email.
 *      Callers must specify at least one "to" or "cc" email address.
 *    </dd>
 *
 *  <dt>subject</dt>
 *    <dd>
 *      If provided, this should contain the subject line of the email that
 *      will be sent.  If not provided, the title of the note will be used
 *      as the subject of the email.
 *    </dd>
 *
 *  <dt>message</dt>
 *    <dd>
 *      If provided, this is additional personal text that should be included
 *      into the email as a message from the owner to the recipient(s).
 *    </dd>
 * </dl>
 */
struct NoteEmailParameters {
  1:  optional  string guid,
  2:  optional  Types.Note note,
  3:  optional  list<string> toAddresses,
  4:  optional  list<string> ccAddresses,
  5:  optional  string subject,
  6:  optional  string message
}

/**
 * Identifying information about previous versions of a note that are backed up
 * within Evernote's servers.  Used in the return value of the listNoteVersions
 * call.
 *
 * <dl>
 *  <dt>updateSequenceNum</dt>
 *  <dd>
 *    The update sequence number for the Note when it last had this content.
 *    This serves to uniquely identify each version of the note, since USN
 *    values are unique within an account for each update.
 *  </dd>
 *  <dt>updated</dt>
 *  <dd>
 *    The 'updated' time that was set on the Note when it had this version
 *    of the content.  This is the user-modifiable modification time on the
 *    note, so it's not reliable for guaranteeing the order of various
 *    versions.  (E.g. if someone modifies the note, then changes this time
 *    manually into the past and then updates the note again.)
 *  </dd>
 *  <dt>saved</dt>
 *  <dd>
 *    A timestamp that holds the date and time when this version of the note
 *    was backed up by Evernote's servers.
 *  </dd>
 *  <dt>title</dt>
 *  <dd>
 *    The title of the note when this particular version was saved.  (The
 *    current title of the note may differ from this value.)
 *  </dd>
 *  <dt>lastEditorId</dt>
 *  <dd>
 *    The ID of the user who made the change to this version of the note. This will be
 *    unset if the note version was edited by the owner of the account.
 *  </dd>
 * </dl>
 */
struct NoteVersionId {
  1:  required  i32 updateSequenceNum,
  2:  required  Types.Timestamp updated,
  3:  required  Types.Timestamp saved,
  4:  required  string title,
  5:  optional  Types.UserID lastEditorId
}

/**
 * A description of the thing for which we are searching for related
 * entities.
 *
 * You must specify either <em>noteGuid</em> or <em>plainText</em>, but
 * not both. <em>filter</em> and <em>referenceUri</em> are optional.
 *
 * <dl>
 * <dt>noteGuid</dt>
 * <dd>The GUID of an existing note in your account for which related
 *     entities will be found.</dd>
 *
 * <dt>plainText</dt>
 * <dd>A string of plain text for which to find related entities.
 *     You should provide a text block with a number of characters between
 *     EDAM_RELATED_PLAINTEXT_LEN_MIN and EDAM_RELATED_PLAINTEXT_LEN_MAX.
 *     </dd>
 *
 * <dt>filter</dt>
 * <dd>The list of criteria that will constrain the notes being considered
 *     related.
 *     Please note that some of the parameters may be ignored, such as
 *     <em>order</em> and <em>ascending</em>.
 * </dd>
 *
 * <dt>referenceUri</dt>
 * <dd>A URI string specifying a reference entity, around which "relatedness"
 *     should be based. This can be an URL pointing to a web page, for example.
 * </dd>
 *
 * <dt>context</dt>
 * <dd>Specifies the context to consider when determining related results.
 *     Clients must leave this value unset unless they wish to explicitly specify a known
 *     non-default context.
 * </dd>
 *
 * <dt>cacheKey</dt>
 * <dd>If set and non-empty, this is an indicator for the server whether it is actually
 *     necessary to perform a new findRelated call at all. Cache Keys are opaque strings
 *     which are returned by the server as part of "RelatedResult" in response
 *     to a "NoteStore.findRelated" query. Cache Keys are inherently query specific.
 *
 *     If set to an empty string, this indicates that the server should generate a cache
 *     key in the response as part of "RelatedResult".
 *
 *     If not set, the server will not attempt to generate a cache key at all.
 * </dd>
 * </dl>
 */
struct RelatedQuery {
  1: optional string noteGuid,
  2: optional string plainText,
  3: optional NoteFilter filter,
  4: optional string referenceUri,
  5: optional string context,
  6: optional string cacheKey
}

/**
 * The result of calling findRelated().  The contents of the notes,
 * notebooks, and tags fields will be in decreasing order of expected
 * relevance.  It is possible that fewer results than requested will be
 * returned even if there are enough distinct entities in the account
 * in cases where the relevance is estimated to be low.
 *
 * <dl>
 * <dt>notes</dt>
 * <dd>If notes have been requested to be included, this will be the
 *     list of notes.</dd>
 *
 * <dt>notebooks</dt>
 * <dd>If notebooks have been requested to be included, this will be the
 *     list of notebooks.</dd>
 *
 * <dt>tags</dt>
 * <dd>If tags have been requested to be included, this will be the list
 *     of tags.</dd>
 *
 * <dt>containingNotebooks</dt>
 * <dd>If <code>includeContainingNotebooks</code> is set to <code>true</code>
 *     in the RelatedResultSpec, return the list of notebooks to
 *     to which the returned related notes belong. The notebooks in this
 *     list will occur once per notebook GUID and are represented as
 *     NotebookDescriptor objects.</dd>
 *
 * <dt>experts</dt>
 * <dd>If experts have been requested to be included, this will return
 *  a list of users within your business who have knowledge about the specified query.
 * </dd>
 *
 * <dt>relatedContent</dt>
 * <dd>If related content has been requested to be included, this will be the list of
 *  related content snippets.
 * </dd>
 *
 * <dt>cacheKey</dt>
 * <dd>If set and non-empty, this cache key may be used in subsequent
 *     "NoteStore.findRelated" calls (via "RelatedQuery") to re-use previous
 *     responses that were cached on the client-side, instead of actually performing
 *     another search.
 *
 *     If set to an empty string, this indicates that the server could not determine
 *     a specific key for this response, but the client should nevertheless remove
 *     any previously cached result for this request.
 *
 *     If unset/null, it is up to the client whether to re-use cached results or to
 *     use the server's response.
 *
 *     If set to the exact non-empty cache key that was specified in
 *     "RelatedQuery.cacheKey", this indicates that the server decided that cached results
 *     could be reused.
 *
 *     Depending on the cache key specified in the query, the "RelatedResult" may only be
 *     partially filled. For each set field, the client should replace the corresponding
 *     part in the previously cached result with the new partial result.
 *
 *     For example, for a specific query that has both "RelatedResultSpec.maxNotes" and
 *     "RelatedResultSpec.maxRelatedContent" set to positive values, the server may decide
 *     that the previously requested and cached <em>Related Content</em> are unchanged,
 *     but new results for <em>Related Notes</em> are available. The
 *     response will have a new cache key and have "RelatedResult.notes" set, but have
 *     "RelatedResult.relatedContent" unset (not just empty, but really unset).
 *
 *     In this situation, the client should replace any cached notes with the newly
 *     returned "RelatedResult.notes", but it can re-use the previously cached entries for
 *     "RelatedResult.relatedContent". List fields that are set, but empty indicate that
 *     no results could be found; the cache should be updated correspondingly.
 * </dd>
 *
 * <dt>cacheExpires</dt>
 * <dd> If set, clients should reuse this response for any situations where the same input
 *      parameters are applicable for up to this many seconds after receiving this result.
 *
 *      After this time has passed, the client may request a new result from the service,
 *      but it should supply the stored cacheKey to the service when checking for an
 *      update.
 * </dd>
 *
 * </dl>
 */
struct RelatedResult {
  1: optional list<Types.Note> notes,
  2: optional list<Types.Notebook> notebooks,
  3: optional list<Types.Tag> tags,
  4: optional list<Types.NotebookDescriptor> containingNotebooks,
  5: optional string debugInfo,
  6: optional list<Types.UserProfile> experts,
  7: optional list<Types.RelatedContent> relatedContent,
  8: optional string cacheKey,
  9: optional i32 cacheExpires
}

/**
 * A description of the thing for which the service will find related
 * entities, via findRelated(), together with a description of what
 * type of entities and how many you are seeking in the
 * RelatedResult.
 *
 * <dl>
 * <dt>maxNotes</dt>
 * <dd>Return notes that are related to the query, but no more than
 *     this many.  Any value greater than EDAM_RELATED_MAX_NOTES
 *     will be silently capped.  If you do not set this field, then
 *     no notes will be returned.</dd>
 *
 * <dt>maxNotebooks</dt>
 * <dd>Return notebooks that are related to the query, but no more than
 *     this many.  Any value greater than EDAM_RELATED_MAX_NOTEBOOKS
 *     will be silently capped.  If you do not set this field, then
 *     no notebooks will be returned.</dd>
 *
 * <dt>maxTags</dt>
 * <dd>Return tags that are related to the query, but no more than
 *     this many.  Any value greater than EDAM_RELATED_MAX_TAGS
 *     will be silently capped.  If you do not set this field, then
 *     no tags will be returned.</dd>
 * </dl>
 *
 * <dt>writableNotebooksOnly</dt>
 * <dd>Require that all returned related notebooks are writable.
 *     The user will be able to create notes in all returned notebooks.
 *     However, individual notes returned may still belong to notebooks
 *     in which the user lacks the ability to create notes.</dd>
 * </dl>
 *
 * <dt>includeContainingNotebooks</dt>
 * <dd>If set to <code>true</code>, return the containingNotebooks field
 *     in the RelatedResult, which will contain the list of notebooks to
 *     to which the returned related notes belong.</dd>
 * </dl>
 *
 * <dt>includeDebugInfo</dt>
 * <dd>If set to <code>true</code>, indicate that debug information should
 *     be returned in the 'debugInfo' field of RelatedResult. Note that the call may
 *     be slower if this flag is set.</dd>
 *
 * <dt>maxExperts</dt>
 * <dd>This can only be used when making a findRelated call against a business.
 *  Find users within your business who have knowledge about the specified query.
 *  No more than this many users will be returned. Any value greater than
 *  EDAM_RELATED_MAX_EXPERTS will be silently capped.
 * </dd>
 *
 * <dt>maxRelatedContent</dt>
 * <dd>Return snippets of related content that is related to the query, but no more than
 *  this many. Any value greater than EDAM_RELATED_MAX_RELATED_CONTENT will be silently
 *  capped. If you do not set this field, then no related content will be returned.</dd>
 * </dl>
 *
 * <dt>relatedContentTypes</dt>
 * <dd>Specifies the types of Related Content that should be returned.</dd>
 * </dl>
 */
struct RelatedResultSpec {
  1: optional i32 maxNotes,
  2: optional i32 maxNotebooks,
  3: optional i32 maxTags,
  4: optional bool writableNotebooksOnly,
  5: optional bool includeContainingNotebooks,
  6: optional bool includeDebugInfo,
  7: optional i32 maxExperts,
  8: optional i32 maxRelatedContent,
  9: optional set<Types.RelatedContentType> relatedContentTypes,
}

/**
 * The result of a call to updateNoteIfUsnMatches, which optionally updates a note
 * based on the current value of the note's update sequence number on the service.
 *
 * <dl>
 * <dt>note</dt>
 * <dd>Either the current state of the note if <tt>updated</tt> is false or the
 * result of updating the note as would be done via the <tt>updateNote</tt> method.
 * If the note was not updated, you will receive a Note that does not include note
 * content, resources data, resources recognition data, or resources alternate data.
 * You can check for updates to these large objects by checking the Data.bodyHash
 * values and downloading accordingly.</dd>
 *
 * <dt>updated</dt>
 * <dd>Whether or not the note was updated by the operation.</dd>
 * </dl>
 */
struct UpdateNoteIfUsnMatchesResult {
  1: optional Types.Note note,
  2: optional bool updated
}

/*
 * This structure is used by the service to communicate to clients, via
 * getShareRelationships, which privilege levels are assignable to the
 * target of a share relationship.
 *
 * <dl>
 * <dt>noSetReadOnly</dt>
 * <dd>This value is true if the user is not allowed to set the privilege
 * level to READ_ONLY.</dd>
 *
 * <dt>noSetReadPlusActivity</dt>
 * <dd>This value is true if the user is not allowed to set the privilege
 * level to READ_NOTEBOOK_PLUS_ACTIVITY.</dd>
 *
 * <dt>noSetModify</dt>
 * <dd>This value is true if the user is not allowed to set the
 * privilege level to MODIFY_NOTEBOOK_PLUS_ACTIVITY.</dd>
 *
 * <dt>noSetFullAccess</dt>
 * <dd>This value is true if the user is not allowed to set the
 * privilege level to FULL_ACCESS, or BUSINESS_FULL_ACCESS if the
 * notebook is a business notebook</dd>
 * </dl>
 */
struct ShareRelationshipRestrictions {
  1:  optional  bool noSetReadOnly,
  2:  optional  bool noSetReadPlusActivity,
  3:  optional  bool noSetModify,
  4:  optional  bool noSetFullAccess
}

/**
 * Privilege levels for accessing shared notebooks.
 *
 * READ_NOTEBOOK: Recipient is able to read the contents of the shared notebook
 *   but does not have access to information about other recipients of the
 *   notebook or the activity stream information.
 *
 * READ_NOTEBOOK_PLUS_ACTIVITY: Recipient has READ_NOTEBOOK rights and can also
 *   access information about other recipients and the activity stream.
 *
 * MODIFY_NOTEBOOK_PLUS_ACTIVITY: Recipient has rights to read and modify the contents
 *   of the shared notebook, including the right to move notes to the trash and to create
 *   notes in the notebook.  The recipient can also access information about other
 *   recipients and the activity stream.
 *
 * FULL_ACCESS: Recipient has full rights to the shared notebook and recipient lists,
 *   including privilege to revoke and create invitations and to change privilege
 *   levels on invitations for individuals. If the user is a member of the same group,
 *   (e.g. the same business) as the shared notebook, they will additionally be granted
 *   permissions to update the publishing status of the notebook.
 */
enum ShareRelationshipPrivilegeLevel {
  READ_NOTEBOOK                  = 0,
  READ_NOTEBOOK_PLUS_ACTIVITY    = 10,
  MODIFY_NOTEBOOK_PLUS_ACTIVITY  = 20,
  FULL_ACCESS                    = 30,
}

/**
 * Describes an invitation to a person to use their Evernote
 * credentials to become a member of a notebook.
 *
 * <dl>
 * <dt>displayName</dt>
 * <dd>The string that clients should show to users to represent this
 * invitation.</dd>
 *
 * <dt>recipientUserIdentity</dt>
 * <dd>Identifies the recipient of the invitation. The user identity
 * type can be either EMAIL, EVERNOTE or IDENTITYID. If the
 * invitation was created using the classic notebook sharing APIs it will be EMAIL. If it
 * was created using the new identity-based notebook sharing APIs it will either be
 * EVERNOTE or IDENTITYID, depending on whether we can map the identity to an Evernote
 * user at the time of creation.
 * </dd>
 *
 * <dt>privilege</dt>
 * <dd>The privilege level at which the member will be joined, if it
 * turns out that the member is not already joined at a higher level.
 * Note that the <tt>identity</tt> field may not uniquely identify an
 * Evernote User ID, and so we won't know until the invitation is
 * redeemed whether or not the recipient already has privilege.</dd>
 *
 * <dt>sharerUserId</dt>
 * <dd>The user id of the user who most recently shared this notebook
 * to this identity. This field is used by the service to convey information
 * to the user, so clients should treat it as read-only.</dd>
 * </dl>
 */
struct InvitationShareRelationship {
  1: optional string displayName,
  2: optional Types.UserIdentity recipientUserIdentity,
  3: optional ShareRelationshipPrivilegeLevel privilege,
  5: optional Types.UserID sharerUserId
}

/**
 * Describes the association between a Notebook and an Evernote User who is
 * a member of that notebook.
 *
 * <dl>
 * <dt>displayName</dt>
 * <dd>The string that clients should show to users to represent this
 * member.</dd>
 *
 * <dt>recipientUserId</dt>
 * <dd>The Evernote User ID of the recipient of this notebook share.
 * </dd>
 *
 * <dt>bestPrivilege</dt>
 * <dd>The privilege at which the member can access the notebook,
 * which is the best privilege granted either individually or to a
 * group to which a member belongs, such as a business.  This field is
 * used by the service to convey information to the user, so clients
 * should treat it as read-only.</dd>
 *
 * <dt>individualPrivilege</dt>
 * <dd>The individually granted privilege for the member, which does
 * not take GROUP privileges into account.  This value may be unset if
 * only a group-assigned privilege has been granted to the member.
 * This value can be managed by others with sufficient rights using
 * the manageNotebookShares method.  The valid values that clients
 * should present to users for selection are given via the the
 * 'restrictions' field.</dd>
 *
 * <dt>restrictions</dt>
 * <dd>The restrictions on which privileges may be individually
 * assigned to the recipient of this share relationship.</dd>
 *
 * <dt>sharerUserId</dt>
 * <dd>The user id of the user who most recently shared the notebook
 * to this user. This field is currently unset for a MemberShareRelationship
 * created by joining a notebook that has been published to the business
 * (MemberShareRelationships where the individual privilege is unset).
 * This field is used by the service to convey information to the user, so
 * clients should treat it as read-only.
 * </dd>
 * </dl>
 */
struct MemberShareRelationship {
  1: optional string displayName,
  2: optional Types.UserID recipientUserId,
  3: optional ShareRelationshipPrivilegeLevel bestPrivilege,
  4: optional ShareRelationshipPrivilegeLevel individualPrivilege,
  5: optional ShareRelationshipRestrictions restrictions,
  6: optional Types.UserID sharerUserId
}

/**
 * Captures a collection of share relationships for a notebook, for
 * example, as returned by the getNotebookShares method.  The share
 * relationships fall into two broad categories: members, and
 * invitations that can be used to become members.
 *
 * <dl>
 * <dt>invitations</dt>
 * <dd>A list of open invitations that can be redeemed into
 * memberships to the notebook.</dd>
 *
 * <dt>memberships</dt>
 * <dd>A list of memberships of the notebook.  A member is identified
 * by their Evernote UserID and has rights to access the
 * notebook.</dd>
 *
 * <dt>invitationRestrictions</dt>
 * <dd>The restrictions on what privileges may be granted to invitees
 * to this notebook. These restrictions may be specific to the calling
 * user or to the notebook itself. They represent the
 * union of all possible invite cases, so it is possible that once the
 * recipient of the invitation has been identified by the service, such
 * as by a business auto-join, the actual assigned privilege may change.
 * </dd>
 * </dl>
 */
struct ShareRelationships {
  1: optional list<InvitationShareRelationship> invitations,
  2: optional list<MemberShareRelationship> memberships,
  3: optional ShareRelationshipRestrictions invitationRestrictions
}

/**
 * A structure that captures parameters used by clients to manage the
 * shares for a given notebook via the manageNotebookShares method.
 *
 * <dl>
 * <dt>notebookGuid</dt>
 * <dd>The GUID of the notebook whose shares are being managed.</dd>
 *
 * <dt>inviteMessage</dt>
 * <dd>If the service sends a message to invitees, this parameter will
 * be used to form the actual message that is sent.</dd>
 *
 * <dt>membershipsToUpdate</dt>
 * <dd>The list of existing memberships to update.  This field is not
 * intended to be the full set of memberships for the notebook and
 * should only include those already-existing memberships that you
 * actually want to change.  If you want to remove shares, see the
 * unshares fields.  If you want to create a membership,
 * i.e. auto-join a business user, you can do this via the
 * invitationsToCreateOrUpdate field using an Evernote UserID of a
 * fellow business member (the created invitation is automatically
 * joined by the service, so the client is creating an
 * invitation, not a membership).</dd>
 *
 * <dt>invitationsToCreateOrUpdate</dt>
 * <dd>The list of invitations to update, as matched by the identity
 * field of the InvitationShareRelationship instances, or to create if
 * an existing invitation does not exist.  This field is not intended
 * to be the full set of invitations on the notebook and should only
 * include those invitations that you wish to create or update.  Note
 * that your invitation could convert into a membership via a
 * service-supported auto-join operation.  This happens, for example,
 * when you use an invitation with an Evernote UserID type for a
 * recipient who is a member of the business to which the notebook
 * belongs.  Note that to discover the user IDs for business members,
 * the sharer must also be part of the business.</dd>
 *
 * <dt>unshares</dt>
 * <dd>The list of share relationships to expunge from the service.
 * If the user identity is for an Evernote UserID, then matching invitations or
 * memberships will be removed. If it's an e-mail, then e-mail based shared notebook
 * invitations will be removed. If it's for an Identity ID, then any invitations that
 * match the identity (by identity ID or user ID or e-mail for legacy invitations) will be
 * removed.</dd>
 * </dl>
 */
struct ManageNotebookSharesParameters {
  1: optional string notebookGuid,
  2: optional string inviteMessage,
  3: optional list<MemberShareRelationship> membershipsToUpdate,
  4: optional list<InvitationShareRelationship> invitationsToCreateOrUpdate,
  5: optional list<Types.UserIdentity> unshares
}

/**
 * A structure to capture certain errors that occurred during a call
 * to manageNotebookShares.  That method can be run best-effort,
 * meaning that some change requests can be applied while others fail.
 * Note that some errors such as system errors will still fail the
 * entire transaction regardless of running best effort.  When some
 * change requests do not succeed, the error conditions are captured
 * in instances of this class, captured by the identity of the share
 * relationship and one of the exception fields.
 *
 * <dl>
 * <dt>userIdentity</dt>
 * <dd>The identity of the share relationship whose update encountered
 * an error.</dd>
 *
 * <dt>userException</dt>
 * <dd>If the error is represented as an EDAMUserException that would
 * have otherwise been thrown without best-effort execution.  Only one
 * exception field will be set.</dd>
 *
 * <dt>notFoundException</dt>
 * <dd>If the error is represented as an EDAMNotFoundException that would
 * have otherwise been thrown without best-effort execution.  Only one
 * exception field will be set.</dd>
 * </dl>
 */
struct ManageNotebookSharesError {
  1: optional Types.UserIdentity userIdentity,
  2: optional Errors.EDAMUserException userException,
  3: optional Errors.EDAMNotFoundException notFoundException
}

/**
 * The return value of a call to the manageNotebookShares method.
 *
 * <dl>
 * <dt>errors</dt>
 * <dd>If the method completed without throwing exceptions, some errors
 * might still have occurred, and in that case, this field will contain
 * the list of those errors the occurred.
 * </dd>
 * </dl>
 */
struct ManageNotebookSharesResult {
  1: optional list<ManageNotebookSharesError> errors
}

/**
 * A structure used to share a note with one or more recipients at a given privilege.
 *
 * <dl>
 *   <dt>noteGuid</dt>
 *   <dd>The GUID of the note.</dd>
 *
 *   <dt>recipientThreadId</dt>
 *   <dd>The recipients of the note share specified as a messaging thread ID. If you
 *       have an existing messaging thread to share the note with, specify its ID
 *       here instead of recipientContacts in order to properly support defunct
 *       identities. The sharer must be a participant of the thread. Either this
 *       field or recipientContacts must be set.</dd>
 *
 *   <dt>recipientContacts</dt>
 *   <dd>The recipients of the note share specified as a list of contacts. This should
 *       only be set if the sharing takes place before the thread is created. Use
 *       recipientThreadId instead when sharing with an existing thread. Either this
 *       field or recipientThreadId must be set.</dd>
 *
 *   <dt>privilege</dt>
 *   <dd>The privilege level to be granted.</dd>
 * </dl>
 */
struct SharedNoteTemplate {
  1: optional Types.Guid noteGuid,
  4: optional Types.MessageThreadID recipientThreadId,
  2: optional list<Types.Contact> recipientContacts,
  3: optional Types.SharedNotePrivilegeLevel privilege
}

/**
 * A structure used to share a notebook with one or more recipients at a given privilege.
 *
 * <dl>
 *   <dt>notebookGuid</dt>
 *   <dd>The GUID of the notebook.</dd>
 *
 *   <dt>recipientThreadId</dt>
 *   <dd>The recipients of the notebook share specified as a messaging thread ID. If you
 *       have an existing messaging thread to share the note with, specify its ID
 *       here instead of recipientContacts in order to properly support defunct
 *       identities. The sharer must be a participant of the thread. Either this field
 *       or recipientContacts must be set.</dd>
 *
 *   <dt>recipientContacts</dt>
 *   <dd>The recipients of the notebook share specified as a list of contacts. This should
 *       only be set if the sharing takes place before the thread is created. Use
 *       recipientThreadId instead when sharing with an existing thread. Either this
 *       field or recipientThreadId must be set.</dd>
 *
 *   <dt>privilege</dt>
 *   <dd>The privilege level to be granted.</dd>
 * </dl>
 */
struct NotebookShareTemplate {
  1: optional Types.Guid notebookGuid,
  4: optional Types.MessageThreadID recipientThreadId,
  2: optional list<Types.Contact> recipientContacts,
  3: optional Types.SharedNotebookPrivilegeLevel privilege
}

/**
 * A structure containing the results of a call to createOrUpdateNotebookShares.
 *
 * <dl>
 *   <dt>updateSequenceNum</dt>
 *   <dd>The USN of the notebook after the call.</dd>
 *
 *   <dt>matchingShares</dt>
 *   <dd>A list of SharedNotebook records that match the desired recipients. These
 *       records may have been either created or updated by the call to
 *       createOrUpdateNotebookShares, or they may have been at the desired privilege
 *       privilege level prior to the call.</dd>
 * </dl>
 */
struct CreateOrUpdateNotebookSharesResult {
  1: optional i32 updateSequenceNum,
  2: optional list<Types.SharedNotebook> matchingShares
}

/**
 * This structure is used by the service to communicate to clients, via
 * getNoteShareRelationships, which privilege levels are assignable to the
 * target of a note share relationship.
 *
 * <dl>
 * <dt>noSetReadNote</dt>
 * <dd>This value is true if the user is not allowed to set the privilege
 * level to SharedNotePrivilegeLevel.READ_NOTE.</dd>
 *
 * <dt>noSetModifyNote</dt>
 * <dd>This value is true if the user is not allowed to set the privilege
 * level to SharedNotePrivilegeLevel.MODIFY_NOTE.</dd>
 *
 * <dt>noSetFullAccess</dt>
 * <dd>This value is true if the user is not allowed to set the
 * privilege level to SharedNotePrivilegeLevel.FULL_ACCESS.</dd>
 * </dl>
 */
struct NoteShareRelationshipRestrictions {
  1: optional bool noSetReadNote,
  2: optional bool noSetModifyNote,
  3: optional bool noSetFullAccess
}

/**
 * Describes the association between a Note and an Evernote User who is
 * a member of that note.
 *
 * <dl>
 * <dt>displayName</dt>
 * <dd>The string that clients should show to users to represent this
 * member.</dd>
 *
 * <dt>recipientUserId</dt>
 * <dd>The Evernote UserID of the user who is a member to the note.</dd>
 *
 * <dt>privilege</dt>
 * <dd>The privilege at which the member can access the note,
 * which is the best privilege granted to the user across all of their
 * individual shares for this note. This field is used by the service
 * to convey information to the user, so clients should treat it as
 * read-only.</dd>
 *
 * <dt>restrictions</dt>
 * <dd>The restrictions on which privileges may be individually
 * assigned to the recipient of this share relationship. This field
 * is used by the service to convey information to the user, so
 * clients should treat it as read-only.</dd>
 *
 * <dt>sharerUserId</dt>
 * <dd>The user id of the user who most recently shared the note with
 * this user. This field is used by the service to convey information
 * to the user, so clients should treat it as read-only.</dd>
 * </dl>
 */
struct NoteMemberShareRelationship {
 1: optional string displayName,
 2: optional Types.UserID recipientUserId,
 3: optional Types.SharedNotePrivilegeLevel privilege,
 4: optional NoteShareRelationshipRestrictions restrictions,
 5: optional Types.UserID sharerUserId
}

/**
 * Describes an invitation to a person to use their Evernote credentials
 * to gain access to a note belonging to another user.
 *
 * <dl>
 * <dt>displayName</dt>
 * <dd>The string that clients should show to users to represent this
 * invitation.</dd>
 *
 * <dt>recipientIdentityId</dt>
 * <dd>Identifies the identity of the invitation recipient. Once the
 * identity has been claimed by an Evernote user and they have accessed
 * the note at least once, the invitation will be used up and will no
 * longer be returned by the service to clients. Instead, that recipient
 * will be included in the list of NoteMemberShareRelationships.</dd>
 *
 * <dt>privilege</dt>
 * <dd>The privilege level that the recipient will be granted when they
 * accept this invitation. If the user already has a higher privilege to
 * access this note then this will not affect the recipient's privileges.</dd>
 *
 * <dt>sharerUserId</dt>
 * <dd>The user id of the user who most recently shared this note to this
 * recipient. This field is used by the service to convey information
 * to the user, so clients should treat it as read-only.</dd>
 */
struct NoteInvitationShareRelationship {
 1: optional string displayName,
 2: optional Types.IdentityID recipientIdentityId,
 3: optional Types.SharedNotePrivilegeLevel privilege,
 5: optional Types.UserID sharerUserId
}

/**
 * Captures a collection of share relationships for a single note,
 * for example, as returned by the getNoteShares method. The share
 * relationships fall into two broad categories: members, and
 * invitations that can be used to become members.
 *
 * <dl>
 * <dt>invitations</dt>
 * <dd>A list of open invitations that can be redeemed into
 * memberships to the note.</dd>
 *
 * <dt>memberships</dt>
 * <dd>A list of memberships of the noteb. A member is identified
 * by their Evernote UserID and has rights to access the
 * note.</dd>
 *
 * <dt>restrictions</dt>
 * <dd>The restrictions on which privileges may be assigned to the recipient
 * of an open invitation. These restrictions only apply to invitations;
 * restrictions on memberships are specified on the NoteMemberShareRelationship.
 * This field is used by the service to convey information to the user, so
 * clients should treat it as read-only.</dd>
 *
 * </dl>
 */
struct NoteShareRelationships {
  1: optional list<NoteInvitationShareRelationship> invitations,
  2: optional list<NoteMemberShareRelationship> memberships,
  3: optional NoteShareRelationshipRestrictions invitationRestrictions
}

/**
 * Captures parameters used by clients to manage the shares for a given
 * note via the manageNoteShares function. This is used only to manage
 * the existing memberships and invitations for a note. To invite a new
 * recipient, use NoteStore.createOrUpdateSharedNotes.
 *
 * The only field of an existing membership or invitation that can be
 * updated by this function is the share privilege.
 *
 * <dl>
 *   <dt>noteGuid</dt>
 *   <dd>The GUID of the note whose shares are being managed.</dd>
 *
 *   <dt>membershipsToUpdate</dt>
 *   <dd>A list of existing memberships to update. This field is not
 *     meant to be the full set of memberships for the note. Clients
 *     should only include those existing memberships that they wish
 *     to modify. To remove an existing membership, see the unshares
 *     field.</dd>
 *
 *   <dt>invitationsToUpdate</dt>
 *   <dd>The list of outstanding invitations to update, as matched by the
 *     identity field of the NoteInvitationShareRelatioship instances.
 *     This field is not meant to be the full set of invitations for the
 *     note. Clients should only include those existing invitations that
 *     they wish to modify.</dd>
 *
 *   <dt>membershipsToUnshare</dt>
 *   <dd>A list of existing memberships to expunge from the service.</dd>
 *
 *   <dt>invitationsToUnshare</dt>
 *   <dd>A list of outstanding invitations to expunge from the service.</dd>
 * </dl>
 */
struct ManageNoteSharesParameters {
 1: optional string noteGuid,
 2: optional list<NoteMemberShareRelationship> membershipsToUpdate,
 3: optional list<NoteInvitationShareRelationship> invitationsToUpdate,
 4: optional list<Types.UserID> membershipsToUnshare
 5: optional list<Types.IdentityID> invitationsToUnshare
}

/**
 * Captures errors that occur during a call to manageNoteShares. That
 * function can be run best-effort, meaning that some change requests can
 * be applied while others fail. Note that some errors such as system
 * exceptions may still cause the entire call to fail.
 *
 * Only one of the two ID fields will be set on a given error.
 *
 * Only one of the two exception fields will be set on a given error.
 *
 * <dl>
 *   <dt>identityID</dt>
 *   <dd>The identity ID of an outstanding invitation that was not updated
 *     due to the error.</dd>
 *
 *   <dt>userID</dt>
 *   <dd>The user ID of an existing membership that was not updated due
 *     to the error.</dd>
 *
 *   <dt>userException</dt>
 *   <dd>If the error is represented as an EDAMUserException that would
 *     have otherwise been thrown without best-effort execution.</dd>
 *
 *   <dt>notFoundException</dt>
 *   <dd>If the error is represented as an EDAMNotFoundException that
 *     would have otherwise been thrown without best-effort execution.
 *     The identifier field of the exception will be either "Identity.id"
 *     or "User.id", indicating that no existing share could be found for
 *     the specified recipient.</dd>
 * </dl>
 */
struct ManageNoteSharesError {
 1: optional Types.IdentityID identityID,
 2: optional Types.UserID userID,
 3: optional Errors.EDAMUserException userException,
 4: optional Errors.EDAMNotFoundException notFoundException
}

/**
 * The return value of a call to the manageNoteShares function.
 *
 * <dl>
 *   <dt>errors</dt>
 *   <dd>If the call succeeded without throwing an exception, some errors
 *     might still have occurred. In that case, this field will contain the
 *     list of errors.</dd>
 * </dl>
 */
struct ManageNoteSharesResult {
 1: optional list<ManageNoteSharesError> errors
}

/**
 * Service:  NoteStore
 * <p>
 * The NoteStore service is used by EDAM clients to exchange information
 * about the collection of notes in an account. This is primarily used for
 * synchronization, but could also be used by a "thin" client without a full
 * local cache.
 * </p><p>
 * Most functions take an "authenticationToken" parameter, which is the
 * value returned by the UserStore which permits access to the account.
 * </p>
 *
 * Calls which require an authenticationToken may throw an EDAMUserException
 * for the following reasons:
 *  <ul>
 *   <li>DATA_REQUIRED "authenticationToken" - token is empty</li>
 *   <li>BAD_DATA_FORMAT "authenticationToken" - token is malformed</li>
 *   <li>INVALID_AUTH "authenticationToken" - token signature is invalid</li>
 *   <li>AUTH_EXPIRED "authenticationToken" - token has expired or been revoked</li>
 *   <li>PERMISSION_DENIED "authenticationToken" - token does not grant permission
 *       to perform the requested action</li>
 *   <li>BUSINESS_SECURITY_LOGIN_REQUIRED "sso" - the user is a member of a business
 *       that requires single sign-on, and must complete SSO before accessing business
 *       content.
 * </ul>
 */
service NoteStore {

  /*========== Synchronization functions for caching clients ===========*/

  /**
   * Asks the NoteStore to provide information about the status of the user
   * account corresponding to the provided authentication token.
   */
  SyncState getSyncState(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Asks the NoteStore to provide the state of the account in order of
   * last modification.  This request retrieves one block of the server's
   * state so that a client can make several small requests against a large
   * account rather than getting the entire state in one big message.
   * This call gives fine-grained control of the data that will
   * be received by a client by omitting data elements that a client doesn't
   * need. This may reduce network traffic and sync times.
   *
   * @param afterUSN
   *   The client can pass this value to ask only for objects that
   *   have been updated after a certain point.  This allows the client to
   *   receive updates after its last checkpoint rather than doing a full
   *   synchronization on every pass.  The default value of "0" indicates
   *   that the client wants to get objects from the start of the account.
   *
   * @param maxEntries
   *   The maximum number of modified objects that should be
   *   returned in the result SyncChunk.  This can be used to limit the size
   *   of each individual message to be friendly for network transfer.
   *
   * @param filter
   *   The caller must set some of the flags in this structure to specify which
   *   data types should be returned during the synchronization.  See
   *   the SyncChunkFilter structure for information on each flag.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "afterUSN" - if negative
   *   </li>
   *   <li> BAD_DATA_FORMAT "maxEntries" - if less than 1
   *   </li>
   * </ul>
   */
  SyncChunk getFilteredSyncChunk(1: string authenticationToken,
                                 2: i32 afterUSN,
                                 3: i32 maxEntries,
                                 4: SyncChunkFilter filter)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Asks the NoteStore to provide information about the status of a linked
   * notebook that has been shared with the caller, or that is public to the
   * world.
   * This will return a result that is similar to getSyncState, but may omit
   * SyncState.uploaded if the caller doesn't have permission to write to
   * the linked notebook.
   *
   * This function must be called on the shard that owns the referenced
   * notebook.  (I.e. the shardId in /shard/shardId/edam/note must be the
   * same as LinkedNotebook.shardId.)
   *
   * @param authenticationToken
   *   This should be an authenticationToken for the guest who has received
   *   the invitation to the share.  (I.e. this should not be the result of
   *   NoteStore.authenticateToSharedNotebook)
   *
   * @param linkedNotebook
   *   This structure should contain identifying information and permissions
   *   to access the notebook in question.
   *
   * @throws EDAMUserException <ul>
   *   <li>DATA_REQUIRED "LinkedNotebook.username" - The username field must be
   *       populated with the current username of the owner of the notebook for which
   *       you are obtaining sync state.
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li>"LinkedNotebook.username" - If the LinkedNotebook.username field does not
   *       correspond to a current user on the service.
   *   </li>
   * </ul>
   *
   * @throws SystemException <ul>
   *   <li>SHARD_UNAVAILABLE - If the provided LinkedNotebook.username corresponds to a
   *       user whose account is on a shard other than that on which this method was
   *       invoked.
   *   </li>
   * </ul>
   */
  SyncState getLinkedNotebookSyncState(1: string authenticationToken,
                                       2: Types.LinkedNotebook linkedNotebook)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Asks the NoteStore to provide information about the contents of a linked
   * notebook that has been shared with the caller, or that is public to the
   * world.
   * This will return a result that is similar to getSyncChunk, but will only
   * contain entries that are visible to the caller.  I.e. only that particular
   * Notebook will be visible, along with its Notes, and Tags on those Notes.
   *
   * This function must be called on the shard that owns the referenced
   * notebook.  (I.e. the shardId in /shard/shardId/edam/note must be the
   * same as LinkedNotebook.shardId.)
   *
   * @param authenticationToken
   *   This should be an authenticationToken for the guest who has received
   *   the invitation to the share.  (I.e. this should not be the result of
   *   NoteStore.authenticateToSharedNotebook)
   *
   * @param linkedNotebook
   *   This structure should contain identifying information and permissions
   *   to access the notebook in question.  This must contain the valid fields
   *   for either a shared notebook (e.g. shareKey)
   *   or a public notebook (e.g. username, uri)
   *
   * @param afterUSN
   *   The client can pass this value to ask only for objects that
   *   have been updated after a certain point.  This allows the client to
   *   receive updates after its last checkpoint rather than doing a full
   *   synchronization on every pass.  The default value of "0" indicates
   *   that the client wants to get objects from the start of the account.
   *
   * @param maxEntries
   *   The maximum number of modified objects that should be
   *   returned in the result SyncChunk.  This can be used to limit the size
   *   of each individual message to be friendly for network transfer.
   *   Applications should not request more than 256 objects at a time,
   *   and must handle the case where the service returns less than the
   *   requested number of objects in a given request even though more
   *   objects are available on the service.
   *
   * @param fullSyncOnly
   *   If true, then the client only wants initial data for a full sync.
   *   In this case, the service will not return any expunged objects,
   *   and will not return any Resources, since these are also provided
   *   in their corresponding Notes.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "afterUSN" - if negative
   *   </li>
   *   <li> BAD_DATA_FORMAT "maxEntries" - if less than 1
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "LinkedNotebook" - if the provided information doesn't match any
   *     valid notebook
   *   </li>
   *   <li> "LinkedNotebook.uri" - if the provided public URI doesn't match any
   *     valid notebook
   *   </li>
   *   <li> "SharedNotebook.id" - if the provided information indicates a
   *      shared notebook that no longer exists
   *   </li>
   * </ul>
   */
  SyncChunk getLinkedNotebookSyncChunk(1: string authenticationToken,
                                       2: Types.LinkedNotebook linkedNotebook,
                                       3: i32 afterUSN,
                                       4: i32 maxEntries,
                                       5: bool fullSyncOnly)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /*============= General account manipulation functions ===============*/

  /**
   * Returns a list of all of the notebooks in the account.
   */
  list<Types.Notebook> listNotebooks(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Returns a list of all the notebooks in a business that the user has permission to access,
   * regardless of whether the user has joined them. This includes notebooks that have been shared
   * with the entire business as well as notebooks that have been shared directly with the user.
   *
   * @param authenticationToken A business authentication token obtained by calling
   *   UserStore.authenticateToBusiness.
   *
   * @throws EDAMUserException <ul>
   *   <li> INVALID_AUTH "authenticationToken" - if the authentication token is not a
   *     business auth token.</li>
   * </ul>
   */
  list<Types.Notebook> listAccessibleBusinessNotebooks(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Returns the current state of the notebook with the provided GUID.
   * The notebook may be active or deleted (but not expunged).
   *
   * @param guid
   *   The GUID of the notebook to be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Notebook.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Notebook" - private notebook, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Notebook.guid" - tag not found, by GUID
   *   </li>
   * </ul>
   */
  Types.Notebook getNotebook(1: string authenticationToken,
                             2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns the notebook that should be used to store new notes in the
   * user's account when no other notebooks are specified.
   */
  Types.Notebook getDefaultNotebook(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Asks the service to make a notebook with the provided name.
   *
   * @param notebook
   *   The desired fields for the notebook must be provided on this
   *   object.  The name of the notebook must be set, and either the 'active'
   *   or 'defaultNotebook' fields may be set by the client at creation.
   *   If a notebook exists in the account with the same name (via
   *   case-insensitive compare), this will throw an EDAMUserException.
   *
   * @return
   *   The newly created Notebook.  The server-side GUID will be
   *   saved in this object's 'guid' field.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Notebook.name" - invalid length or pattern</li>
   *   <li> BAD_DATA_FORMAT "Notebook.stack" - invalid length or pattern</li>
   *   <li> BAD_DATA_FORMAT "Publishing.uri" - if publishing set but bad uri</li>
   *   <li> BAD_DATA_FORMAT "Publishing.publicDescription" - if too long</li>
   *   <li> DATA_CONFLICT "Notebook.name" - name already in use</li>
   *   <li> DATA_CONFLICT "Publishing.uri" - if URI already in use</li>
   *   <li> DATA_REQUIRED "Publishing.uri" - if publishing set but uri missing</li>
   *   <li> DATA_REQUIRED "Notebook" - notebook parameter was null</li>
   *   <li> PERMISSION_DENIED "Notebook.defaultNotebook" - if the 'defaultNotebook' field
   *        is set to 'true' for a Notebook that is not owned by the user identified by
   *        the passed authenticationToken.</li>
   *   <li> LIMIT_REACHED "Notebook" - at max number of notebooks</li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Workspace.guid" - if workspaceGuid set and no Workspace exists for the GUID
   *   </li>
   * </ul>
   */
  Types.Notebook createNotebook(1: string authenticationToken,
                                2: Types.Notebook notebook)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Submits notebook changes to the service. The provided data must include the
   * notebook's guid field for identification.
   * <p />
   * The Notebook will be moved to the specified Workspace, if a non empty
   * Notebook.workspaceGuid is provided. If an empty Notebook.workspaceGuid is set and the
   * Notebook is in a Workspace, then it will be removed from the Workspace and a full
   * access SharedNotebook record will be ensured for the caller. If the caller does not
   * already have a full access share, either the privilege of an existing share will be
   * upgraded or a new share will be created. It is illegal to set a
   * Notebook.workspaceGuid on a Workspace backing Notebook.
   *
   * @param notebook
   *   The notebook object containing the requested changes.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Notebook.name" - invalid length or pattern</li>
   *   <li> BAD_DATA_FORMAT "Notebook.stack" - invalid length or pattern</li>
   *   <li> BAD_DATA_FORMAT "Publishing.uri" - if publishing set but bad uri</li>
   *   <li> BAD_DATA_FORMAT "Publishing.publicDescription" - if too long</li>
   *   <li> DATA_CONFLICT "Notebook.name" - name already in use</li>
   *   <li> DATA_CONFLICT "Publishing.uri" - if URI already in use</li>
   *   <li> DATA_REQUIRED "Publishing.uri" - if publishing set but uri missing</li>
   *   <li> DATA_REQUIRED "Notebook" - notebook parameter was null</li>
   *   <li> PERMISSION_DENIED "Notebook.defaultNotebook" - if the 'defaultNotebook' field
   *        is set to 'true' for a Notebook that is not owned by the user identified by
   *        the passed authenticationToken.</li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Notebook.guid" - not found, by GUID</li>
   *   <li> "Workspace.guid" - if a non empty workspaceGuid set and no Workspace exists
   *        for the GUID
   *   </li>
   * </ul>
   */
  i32 updateNotebook(1: string authenticationToken,
                     2: Types.Notebook notebook)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Permanently removes the notebook from the user's account.
   * After this action, the notebook is no longer available for undeletion, etc.
   * If the notebook contains any Notes, they will be moved to the current
   * default notebook and moved into the trash (i.e. Note.active=false).
   * <p/>
   * NOTE: This function is generally not available to third party applications.
   * Calls will result in an EDAMUserException with the error code
   * PERMISSION_DENIED.
   *
   * @param guid
   *   The GUID of the notebook to delete.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Notebook.guid" - if the parameter is missing
   *   </li>
   *   <li> LIMIT_REACHED "Notebook" - trying to expunge the last Notebook
   *   </li>
   *   <li> PERMISSION_DENIED "Notebook" - private notebook, user doesn't own
   *   </li>
   * </ul>
   */
  i32 expungeNotebook(1: string authenticationToken,
                      2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns a list of the tags in the account.  Evernote does not support
   * the undeletion of tags, so this will only include active tags.
   */
  list<Types.Tag> listTags(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Returns a list of the tags that are applied to at least one note within
   * the provided notebook.  If the notebook is public, the authenticationToken
   * may be ignored.
   *
   * @param notebookGuid
   *    the GUID of the notebook to use to find tags
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Notebook.guid" - notebook not found by GUID
   *   </li>
   * </ul>
   */
  list<Types.Tag> listTagsByNotebook(1: string authenticationToken,
                                     2: Types.Guid notebookGuid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns the current state of the Tag with the provided GUID.
   *
   * @param guid
   *   The GUID of the tag to be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Tag.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Tag" - private Tag, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Tag.guid" - tag not found, by GUID
   *   </li>
   * </ul>
   */
  Types.Tag getTag(1: string authenticationToken,
                   2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Asks the service to make a tag with a set of information.
   *
   * @param tag
   *   The desired list of fields for the tag are specified in this
   *   object.  The caller must specify the tag name, and may provide
   *   the parentGUID.
   *
   * @return
   *   The newly created Tag.  The server-side GUID will be
   *   saved in this object.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Tag.name" - invalid length or pattern
   *   </li>
   *   <li> BAD_DATA_FORMAT "Tag.parentGuid" - malformed GUID
   *   </li>
   *   <li> DATA_CONFLICT "Tag.name" - name already in use
   *   </li>
   *   <li> LIMIT_REACHED "Tag" - at max number of tags
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Tag.parentGuid" - not found, by GUID
   *   </li>
   * </ul>
   */
  Types.Tag createTag(1: string authenticationToken,
                      2: Types.Tag tag)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Submits tag changes to the service.  The provided data must include
   * the tag's guid field for identification.  The service will apply
   * updates to the following tag fields:  name, parentGuid
   *
   * @param tag
   *   The tag object containing the requested changes.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Tag.name" - invalid length or pattern
   *   </li>
   *   <li> BAD_DATA_FORMAT "Tag.parentGuid" - malformed GUID
   *   </li>
   *   <li> DATA_CONFLICT "Tag.name" - name already in use
   *   </li>
   *   <li> DATA_CONFLICT "Tag.parentGuid" - can't set parent: circular
   *   </li>
   *   <li> PERMISSION_DENIED "Tag" - user doesn't own tag
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Tag.guid" - tag not found, by GUID
   *   </li>
   *   <li> "Tag.parentGuid" - parent not found, by GUID
   *   </li>
   * </ul>
   */
  i32 updateTag(1: string authenticationToken,
                2: Types.Tag tag)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Removes the provided tag from every note that is currently tagged with
   * this tag.  If this operation is successful, the tag will still be in
   * the account, but it will not be tagged on any notes.
   *
   * This function is not indended for use by full synchronizing clients, since
   * it does not provide enough result information to the client to reconcile
   * the local state without performing a follow-up sync from the service.  This
   * is intended for "thin clients" that need to efficiently support this as
   * a UI operation.
   *
   * @param guid
   *   The GUID of the tag to remove from all notes.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Tag.guid" - if the guid parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Tag" - user doesn't own tag
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Tag.guid" - tag not found, by GUID
   *   </li>
   * </ul>
   */
  void untagAll(1: string authenticationToken,
                2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Permanently deletes the tag with the provided GUID, if present.
   * <p/>
   * NOTE: This function is not generally available to third party applications.
   * Calls will result in an EDAMUserException with the error code
   * PERMISSION_DENIED.
   *
   * @param guid
   *   The GUID of the tag to delete.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Tag.guid" - if the guid parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Tag" - user doesn't own tag
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Tag.guid" - tag not found, by GUID
   *   </li>
   * </ul>
   */
  i32 expungeTag(1: string authenticationToken,
                 2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),


  /**
   * Returns a list of the searches in the account.  Evernote does not support
   * the undeletion of searches, so this will only include active searches.
   */
  list<Types.SavedSearch> listSearches(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Returns the current state of the search with the provided GUID.
   *
   * @param guid
   *   The GUID of the search to be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "SavedSearch.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "SavedSearch" - private Tag, user doesn't own
   *   </li>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "SavedSearch.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  Types.SavedSearch getSearch(1: string authenticationToken,
                              2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Asks the service to make a saved search with a set of information.
   *
   * @param search
   *   The desired list of fields for the search are specified in this
   *   object. The caller must specify the name and query for the
   *   search, and may optionally specify a search scope.
   *   The SavedSearch.format field is ignored by the service.
   *
   * @return
   *   The newly created SavedSearch.  The server-side GUID will be
   *   saved in this object.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "SavedSearch.name" - invalid length or pattern
   *   </li>
   *   <li> BAD_DATA_FORMAT "SavedSearch.query" - invalid length
   *   </li>
   *   <li> DATA_CONFLICT "SavedSearch.name" - name already in use
   *   </li>
   *   <li> LIMIT_REACHED "SavedSearch" - at max number of searches
   *   </li>
   * </ul>
   */
  Types.SavedSearch createSearch(1: string authenticationToken,
                                 2: Types.SavedSearch search)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException),

  /**
   * Submits search changes to the service. The provided data must include
   * the search's guid field for identification. The service will apply
   * updates to the following search fields: name, query, and scope.
   *
   * @param search
   *   The search object containing the requested changes.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "SavedSearch.name" - invalid length or pattern
   *   </li>
   *   <li> BAD_DATA_FORMAT "SavedSearch.query" - invalid length
   *   </li>
   *   <li> DATA_CONFLICT "SavedSearch.name" - name already in use
   *   </li>
   *   <li> PERMISSION_DENIED "SavedSearch" - user doesn't own tag
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "SavedSearch.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  i32 updateSearch(1: string authenticationToken,
                   2: Types.SavedSearch search)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Permanently deletes the saved search with the provided GUID, if present.
   * <p/>
   * NOTE: This function is generally not available to third party applications.
   * Calls will result in an EDAMUserException with the error code
   * PERMISSION_DENIED.
   *
   * @param guid
   *   The GUID of the search to delete.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "SavedSearch.guid" - if the guid parameter is empty
   *   </li>
   *   <li> PERMISSION_DENIED "SavedSearch" - user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "SavedSearch.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  i32 expungeSearch(1: string authenticationToken,
                    2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Finds the position of a note within a sorted subset of all of the user's
   * notes. This may be useful for thin clients that are displaying a paginated
   * listing of a large account, which need to know where a particular note
   * sits in the list without retrieving all notes first.
   *
   * @param authenticationToken
   *   Must be a valid token for the user's account unless the NoteFilter
   *   'notebookGuid' is the GUID of a public notebook.
   *
   * @param filter
   *   The list of criteria that will constrain the notes to be returned.
   *
   * @param guid
   *   The GUID of the note to be retrieved.
   *
   * @return
   *   If the note with the provided GUID is found within the matching note
   *   list, this will return the offset of that note within that list (where
   *   the first offset is 0).  If the note is not found within the set of
   *   notes, this will return -1.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "offset" - not between 0 and EDAM_USER_NOTES_MAX
   *   </li>
   *   <li> BAD_DATA_FORMAT "maxNotes" - not between 0 and EDAM_USER_NOTES_MAX
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.notebookGuid" - if malformed
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.tagGuids" - if any are malformed
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.words" - if search string too long
   *   </li>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Notebook.guid" - not found, by GUID
   *   </li>
   *   <li> "Note.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  i32 findNoteOffset(1: string authenticationToken,
                     2: NoteFilter filter,
                     3: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Used to find the high-level information about a set of the notes from a
   * user's account based on various criteria specified via a NoteFilter object.
   * <p/>
   * Web applications that wish to periodically check for new content in a user's
   * Evernote account should consider using webhooks instead of polling this API.
   * See http://dev.evernote.com/documentation/cloud/chapters/polling_notification.php
   * for more information.
   *
   * @param authenticationToken
   *   Must be a valid token for the user's account unless the NoteFilter
   *   'notebookGuid' is the GUID of a public notebook.
   *
   * @param filter
   *   The list of criteria that will constrain the notes to be returned.
   *
   * @param offset
   *   The numeric index of the first note to show within the sorted
   *   results.  The numbering scheme starts with "0".  This can be used for
   *   pagination.
   *
   * @param maxNotes
   *   The maximum notes to return in this query.  The service will return a set
   *   of notes that is no larger than this number, but may return fewer notes
   *   if needed.  The NoteList.totalNotes field in the return value will
   *   indicate whether there are more values available after the returned set.
   *   Currently, the service will not return more than 250 notes in a single request,
   *   but this number may change in the future.
   *
   * @param resultSpec
   *   This specifies which information should be returned for each matching
   *   Note. The fields on this structure can be used to eliminate data that
   *   the client doesn't need, which will reduce the time and bandwidth
   *   to receive and process the reply.
   *
   * @return
   *   The list of notes that match the criteria.
   *   The Notes.sharedNotes field will not be set.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "offset" - not between 0 and EDAM_USER_NOTES_MAX
   *   </li>
   *   <li> BAD_DATA_FORMAT "maxNotes" - not between 0 and EDAM_USER_NOTES_MAX
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.notebookGuid" - if malformed
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.tagGuids" - if any are malformed
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.words" - if search string too long
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Notebook.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  NotesMetadataList findNotesMetadata(1: string authenticationToken,
                                      2: NoteFilter filter,
                                      3: i32 offset,
                                      4: i32 maxNotes,
                                      5: NotesMetadataResultSpec resultSpec)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),


  /**
   * This function is used to determine how many notes are found for each
   * notebook and tag in the user's account, given a current set of filter
   * parameters that determine the current selection.  This function will
   * return a structure that gives the note count for each notebook and tag
   * that has at least one note under the requested filter.  Any notebook or
   * tag that has zero notes in the filtered set will not be listed in the
   * reply to this function (so they can be assumed to be 0).
   *
   * @param authenticationToken
   *   Must be a valid token for the user's account unless the NoteFilter
   *   'notebookGuid' is the GUID of a public notebook.
   *
   * @param filter
   *   The note selection filter that is currently being applied.  The note
   *   counts are to be calculated with this filter applied to the total set
   *   of notes in the user's account.
   *
   * @param withTrash
   *   If true, then the NoteCollectionCounts.trashCount will be calculated
   *   and supplied in the reply. Otherwise, the trash value will be omitted.
   *
   * @throws EDAMUserException <ul>
   *   <li>BAD_DATA_FORMAT "NoteFilter.notebookGuid" - if malformed</li>
   *   <li>BAD_DATA_FORMAT "NoteFilter.notebookGuids" - if any are malformed</li>
   *   <li>BAD_DATA_FORMAT "NoteFilter.words" - if search string too long</li>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Notebook.guid" - not found, by GUID</li>
   * </ul>
   */
  NoteCollectionCounts findNoteCounts(1: string authenticationToken,
                                      2: NoteFilter filter,
                                      3: bool withTrash)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns the current state of the note in the service with the provided
   * GUID.  The ENML contents of the note will only be provided if the
   * 'withContent' parameter is true.  The service will include the meta-data
   * for each resource in the note, but the binary content depends
   * on whether it is explicitly requested in resultSpec parameter.
   * If the Note is found in a public notebook, the authenticationToken
   * will be ignored (so it could be an empty string).  The applicationData
   * fields are returned as keysOnly.
   *
   * @param authenticationToken
   *   An authentication token that grants the caller access to the requested note.
   *
   * @param guid
   *   The GUID of the note to be retrieved.
   *
   * @param resultSpec
   *   A structure specifying the fields of the note that the caller would like to get.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Note.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Note" - private note, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  Types.Note getNoteWithResultSpec(1: string authenticationToken,
                                   2: Types.Guid guid,
                                   3: NoteResultSpec resultSpec)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * DEPRECATED. See getNoteWithResultSpec.
   *
   * This function is equivalent to getNoteWithResultSpec, with each of the boolean parameters
   * mapping to the equivalent field of a NoteResultSpec. The Note.sharedNotes field is never
   * populated on the returned note. To get a note with its shares, use getNoteWithResultSpec.
   */
  Types.Note getNote(1: string authenticationToken,
                     2: Types.Guid guid,
                     3: bool withContent,
                     4: bool withResourcesData,
                     5: bool withResourcesRecognition,
                     6: bool withResourcesAlternateData)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Get all of the application data for the note identified by GUID,
   * with values returned within the LazyMap fullMap field.
   * If there are no applicationData entries, then a LazyMap
   * with an empty fullMap will be returned. If your application
   * only needs to fetch its own applicationData entry, use
   * getNoteApplicationDataEntry instead.
   */
  Types.LazyMap getNoteApplicationData(1: string authenticationToken,
                                       2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Get the value of a single entry in the applicationData map
   * for the note identified by GUID.
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - note not found, by GUID</li>
   *   <li> "NoteAttributes.applicationData.key" - note not found, by key</li>
   * </ul>
   */
  string getNoteApplicationDataEntry(1: string authenticationToken,
                                     2: Types.Guid guid,
                                     3: string key)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Update, or create, an entry in the applicationData map for
   * the note identified by guid.
   */
  i32 setNoteApplicationDataEntry(1: string authenticationToken,
                                  2: Types.Guid guid,
                                  3: string key,
                                  4: string value)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Remove an entry identified by 'key' from the applicationData map for
   * the note identified by 'guid'. Silently ignores an unset of a
   * non-existing key.
   */
  i32 unsetNoteApplicationDataEntry(1: string authenticationToken,
                                    2: Types.Guid guid,
                                    3: string key)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns XHTML contents of the note with the provided GUID.
   * If the Note is found in a public notebook, the authenticationToken
   * will be ignored (so it could be an empty string).
   *
   * @param guid
   *   The GUID of the note to be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Note.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Note" - private note, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  string getNoteContent(1: string authenticationToken,
                        2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns a block of the extracted plain text contents of the note with the
   * provided GUID.  This text can be indexed for search purposes by a light
   * client that doesn't have capabilities to extract all of the searchable
   * text content from the note and its resources.
   *
   * If the Note is found in a public notebook, the authenticationToken
   * will be ignored (so it could be an empty string).
   *
   * @param guid
   *   The GUID of the note to be retrieved.
   *
   * @param noteOnly
   *   If true, this will only return the text extracted from the ENML contents
   *   of the note itself.  If false, this will also include the extracted text
   *   from any text-bearing resources (PDF, recognized images)
   *
   * @param tokenizeForIndexing
   *   If true, this will break the text into cleanly separated and sanitized
   *   tokens.  If false, this will return the more raw text extraction, with
   *   its original punctuation, capitalization, spacing, etc.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Note.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Note" - private note, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  string getNoteSearchText(1: string authenticationToken,
                           2: Types.Guid guid,
                           3: bool noteOnly,
                           4: bool tokenizeForIndexing)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),


  /**
   * Returns a block of the extracted plain text contents of the resource with
   * the provided GUID.  This text can be indexed for search purposes by a light
   * client that doesn't have capability to extract all of the searchable
   * text content from a resource.
   *
   * If the Resource is found in a public notebook, the authenticationToken
   * will be ignored (so it could be an empty string).
   *
   * @param guid
   *   The GUID of the resource to be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Resource.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Resource" - private resource, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Resource.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  string getResourceSearchText(1: string authenticationToken,
                               2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns a list of the names of the tags for the note with the provided
   * guid.  This can be used with authentication to get the tags for a
   * user's own note, or can be used without valid authentication to retrieve
   * the names of the tags for a note in a public notebook.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Note.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Note" - private note, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  list<string> getNoteTagNames(1: string authenticationToken,
                               2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Asks the service to make a note with the provided set of information.
   *
   * @param note
   *   A Note object containing the desired fields to be populated on
   *   the service.
   *
   * @return
   *   The newly created Note from the service.  The server-side
   *   GUIDs for the Note and any Resources will be saved in this object.
   *   The service will include the meta-data
   *   for each resource in the note, but the binary contents of the resources
   *   and their recognition data will be omitted (except Recognition Resource body,
   *   for which the behavior is unspecified).
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Note.title" - invalid length or pattern
   *   </li>
   *   <li> BAD_DATA_FORMAT "Note.content" - invalid length for ENML content
   *   </li>
   *   <li> BAD_DATA_FORMAT "Resource.mime" - invalid resource MIME type
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteAttributes.*" - bad resource string
   *   </li>
   *   <li> BAD_DATA_FORMAT "ResourceAttributes.*" - bad resource string
   *   </li>
   *   <li> DATA_CONFLICT "Note.deleted" - deleted time set on active note
   *   </li>
   *   <li> DATA_REQUIRED "Resource.data" - resource data body missing
   *   </li>
   *   <li> ENML_VALIDATION "*" - note content doesn't validate against DTD
   *   </li>
   *   <li> LIMIT_REACHED "Note" - at max number per account
   *   </li>
   *   <li> LIMIT_REACHED "Note.size" - total note size too large
   *   </li>
   *   <li> LIMIT_REACHED "Note.resources" - too many resources on Note
   *   </li>
   *   <li> LIMIT_REACHED "Note.tagGuids" - too many Tags on Note
   *   </li>
   *   <li> LIMIT_REACHED "Resource.data.size" - resource too large
   *   </li>
   *   <li> LIMIT_REACHED "NoteAttribute.*" - attribute string too long
   *   </li>
   *   <li> LIMIT_REACHED "ResourceAttribute.*" - attribute string too long
   *   </li>
   *   <li> PERMISSION_DENIED "Note.notebookGuid" - NB not owned by user
   *   </li>
   *   <li> QUOTA_REACHED "Accounting.uploadLimit" - note exceeds upload quota
   *   </li>
   *   <li> BAD_DATA_FORMAT "Tag.name" - Note.tagNames was provided, and one
   *     of the specified tags had an invalid length or pattern
   *   </li>
   *   <li> LIMIT_REACHED "Tag" - Note.tagNames was provided, and the required
   *     new tags would exceed the maximum number per account
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.notebookGuid" - not found, by GUID
   *   </li>
   * </ul>
   */
  Types.Note createNote(1: string authenticationToken,
                        2: Types.Note note)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Submit a set of changes to a note to the service.  The provided data
   * must include the note's guid field for identification. The note's
   * title must also be set.
   *
   * @param note
   *   A Note object containing the desired fields to be populated on
   *   the service. With the exception of the note's title and guid, fields
   *   that are not being changed do not need to be set. If the content is not
   *   being modified, note.content should be left unset. If the list of
   *   resources is not being modified, note.resources should be left unset.
   *
   * @return
   *   The Note.sharedNotes field will not be set.
   *   The service will include the meta-data
   *   for each resource in the note, but the binary contents of the resources
   *   and their recognition data will be omitted.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Note.title" - invalid length or pattern
   *   </li>
   *   <li> BAD_DATA_FORMAT "Note.content" - invalid length for ENML body
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteAttributes.*" - bad resource string
   *   </li>
   *   <li> BAD_DATA_FORMAT "ResourceAttributes.*" - bad resource string
   *   </li>
   *   <li> BAD_DATA_FORMAT "Resource.mime" - invalid resource MIME type
   *   </li>
   *   <li> DATA_CONFLICT "Note.deleted" - deleted time set on active note
   *   </li>
   *   <li> DATA_REQUIRED "Resource.data" - resource data body missing
   *   </li>
   *   <li> ENML_VALIDATION "*" - note content doesn't validate against DTD
   *   </li>
   *   <li> LIMIT_REACHED "Note.tagGuids" - too many Tags on Note
   *   </li>
   *   <li> LIMIT_REACHED "Note.resources" - too many resources on Note
   *   </li>
   *   <li> LIMIT_REACHED "Note.size" - total note size too large
   *   </li>
   *   <li> LIMIT_REACHED "Resource.data.size" - resource too large
   *   </li>
   *   <li> LIMIT_REACHED "NoteAttribute.*" - attribute string too long
   *   </li>
   *   <li> LIMIT_REACHED "ResourceAttribute.*" - attribute string too long
   *   </li>
   *   <li> PERMISSION_DENIED "Note.notebookGuid" - user doesn't own destination
   *   <li> PERMISSION_DENIED "Note.tags" - user doesn't have permission to
   *     modify the note's tags. note.tags must be unset.
   *   </li>
   *   <li> PERMISSION_DENIED "Note.attributes" - user doesn't have permission
   *     to modify the note's attributes. note.attributes must be unset.
   *   </li>
   *   <li> QUOTA_REACHED "Accounting.uploadLimit" - note exceeds upload quota
   *   </li>
   *   <li> BAD_DATA_FORMAT "Tag.name" - Note.tagNames was provided, and one
   *     of the specified tags had an invalid length or pattern
   *   </li>
   *   <li> LIMIT_REACHED "Tag" - Note.tagNames was provided, and the required
   *     new tags would exceed the maximum number per account
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - note not found, by GUID
   *   </li>
   *   <li> "Note.notebookGuid" - if notebookGuid provided, but not found
   *   </li>
   * </ul>
   */
  Types.Note updateNote(1: string authenticationToken,
                        2: Types.Note note)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Moves the note into the trash. The note may still be undeleted, unless it
   * is expunged.  This is equivalent to calling updateNote() after setting
   * Note.active = false
   *
   * @param guid
   *   The GUID of the note to delete.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> PERMISSION_DENIED "Note" - user doesn't have permission to
   *          update the note.
   *   </li>
   * </ul>
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_CONFLICT "Note.guid" - the note is already deleted
   *   </li>
   * </ul>
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  i32 deleteNote(1: string authenticationToken,
                  2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Permanently removes a Note, and all of its Resources,
   * from the service.
   * <p/>
   * NOTE: This function is not available to third party applications.
   * Calls will result in an EDAMUserException with the error code
   * PERMISSION_DENIED.
   *
   * @param guid
   *   The GUID of the note to delete.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> PERMISSION_DENIED "Note" - user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  i32 expungeNote(1: string authenticationToken,
                  2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Performs a deep copy of the Note with the provided GUID 'noteGuid' into
   * the Notebook with the provided GUID 'toNotebookGuid'.
   * The caller must be the owner of both the Note and the Notebook.
   * This creates a new Note in the destination Notebook with new content and
   * Resources that match all of the content and Resources from the original
   * Note, but with new GUID identifiers.
   * The original Note is not modified by this operation.
   * The copied note is considered as an "upload" for the purpose of upload
   * transfer limit calculation, so its size is added to the upload count for
   * the owner.
   *
   * If the original note has been shared and has SharedNote records, the shares
   * are NOT copied.
   *
   * @param noteGuid
   *   The GUID of the Note to copy.
   *
   * @param toNotebookGuid
   *   The GUID of the Notebook that should receive the new Note.
   *
   * @return
   *   The metadata for the new Note that was created.  This will include the
   *   new GUID for this Note (and any copied Resources), but will not include
   *   the content body or the binary bodies of any Resources.
   *
   * @throws EDAMUserException <ul>
   *   <li> LIMIT_REACHED "Note" - at max number per account
   *   </li>
   *   <li> PERMISSION_DENIED "Notebook.guid" - destination not owned by user
   *   </li>
   *   <li> PERMISSION_DENIED "Note" - user doesn't own
   *   </li>
   *   <li> QUOTA_REACHED "Accounting.uploadLimit" - note exceeds upload quota
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Notebook.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  Types.Note copyNote(1: string authenticationToken,
                      2: Types.Guid noteGuid,
                      3: Types.Guid toNotebookGuid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns a list of the prior versions of a particular note that are
   * saved within the service.  These prior versions are stored to provide a
   * recovery from unintentional removal of content from a note. The identifiers
   * that are returned by this call can be used with getNoteVersion to retrieve
   * the previous note.
   * The identifiers will be listed from the most recent versions to the oldest.
   * This call is only available for notes in Premium accounts. (I.e. access
   * to past versions of Notes is a Premium-only feature.)
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "Note.guid" - if GUID is null or empty string.
   *   </li>
   *   <li> BAD_DATA_FORMAT "Note.guid" - if GUID is not of correct length.
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID.
   *   </li>
   * </ul>
   */
  list<NoteVersionId> listNoteVersions(1: string authenticationToken,
                                       2: Types.Guid noteGuid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * This can be used to retrieve a previous version of a Note after it has been
   * updated within the service.  The caller must identify the note (via its
   * guid) and the version (via the updateSequenceNumber of that version).
   * to find a listing of the stored version USNs for a note, call
   * listNoteVersions.
   * This call is only available for notes in Premium accounts. (I.e. access
   * to past versions of Notes is a Premium-only feature.)
   *
   * @param noteGuid
   *   The GUID of the note to be retrieved.
   *
   * @param updateSequenceNum
   *   The USN of the version of the note that is being retrieved
   *
   * @param withResourcesData
   *   If true, any Resource elements in this Note will include the binary
   *   contents of their 'data' field's body.
   *
   * @param withResourcesRecognition
   *   If true, any Resource elements will include the binary contents of the
   *   'recognition' field's body if recognition data is present.
   *
   * @param withResourcesAlternateData
   *   If true, any Resource elements in this Note will include the binary
   *   contents of their 'alternateData' fields' body, if an alternate form
   *   is present.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "Note.guid" - if GUID is null or empty string.
   *   </li>
   *   <li> BAD_DATA_FORMAT "Note.guid" - if GUID is not of correct length.
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID.
   *   </li>
   *   <li> "Note.updateSequenceNumber" - the Note doesn't have a version with
   *      the corresponding USN.
   *   </li>
   * </ul>
   */
  Types.Note getNoteVersion(1: string authenticationToken,
                            2: Types.Guid noteGuid,
                            3: i32 updateSequenceNum,
                            4: bool withResourcesData,
                            5: bool withResourcesRecognition,
                            6: bool withResourcesAlternateData)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns the current state of the resource in the service with the
   * provided GUID.
   * If the Resource is found in a public notebook, the authenticationToken
   * will be ignored (so it could be an empty string).  Only the
   * keys for the applicationData will be returned.
   *
   * @param guid
   *   The GUID of the resource to be retrieved.
   *
   * @param withData
   *   If true, the Resource will include the binary contents of the
   *   'data' field's body.
   *
   * @param withRecognition
   *   If true, the Resource will include the binary contents of the
   *   'recognition' field's body if recognition data is present.
   *
   * @param withAttributes
   *   If true, the Resource will include the attributes
   *
   * @param withAlternateData
   *   If true, the Resource will include the binary contents of the
   *   'alternateData' field's body, if an alternate form is present.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Resource.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Resource" - private resource, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Resource.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  Types.Resource getResource(1: string authenticationToken,
                             2: Types.Guid guid,
                             3: bool withData,
                             4: bool withRecognition,
                             5: bool withAttributes,
                             6: bool withAlternateData)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Get all of the application data for the Resource identified by GUID,
   * with values returned within the LazyMap fullMap field.
   * If there are no applicationData entries, then a LazyMap
   * with an empty fullMap will be returned. If your application
   * only needs to fetch its own applicationData entry, use
   * getResourceApplicationDataEntry instead.
   */
  Types.LazyMap getResourceApplicationData(1: string authenticationToken,
                                           2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Get the value of a single entry in the applicationData map
   * for the Resource identified by GUID.
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Resource.guid" - Resource not found, by GUID</li>
   *   <li> "ResourceAttributes.applicationData.key" - Resource not found, by key</li>
   * </ul>
   */
  string getResourceApplicationDataEntry(1: string authenticationToken,
                                         2: Types.Guid guid,
                                         3: string key)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Update, or create, an entry in the applicationData map for
   * the Resource identified by guid.
   */
  i32 setResourceApplicationDataEntry(1: string authenticationToken,
                                      2: Types.Guid guid,
                                      3: string key,
                                      4: string value)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Remove an entry identified by 'key' from the applicationData map for
   * the Resource identified by 'guid'.
   */
  i32 unsetResourceApplicationDataEntry(1: string authenticationToken,
                                        2: Types.Guid guid,
                                        3: string key)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Submit a set of changes to a resource to the service.  This can be used
   * to update the meta-data about the resource, but cannot be used to change
   * the binary contents of the resource (including the length and hash).  These
   * cannot be changed directly without creating a new resource and removing the
   * old one via updateNote.
   *
   * @param resource
   *   A Resource object containing the desired fields to be populated on
   *   the service.  The service will attempt to update the resource with the
   *   following fields from the client:
   *   <ul>
   *      <li>guid:  must be provided to identify the resource
   *      </li>
   *      <li>mime
   *      </li>
   *      <li>width
   *      </li>
   *      <li>height
   *      </li>
   *      <li>duration
   *      </li>
   *      <li>attributes:  optional.  if present, the set of attributes will
   *           be replaced.
   *      </li>
   *   </ul>
   *
   * @return
   *   The Update Sequence Number of the resource after the changes have been
   *   applied.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Resource.guid" - if the parameter is missing
   *   </li>
   *   <li> BAD_DATA_FORMAT "Resource.mime" - invalid resource MIME type
   *   </li>
   *   <li> BAD_DATA_FORMAT "ResourceAttributes.*" - bad resource string
   *   </li>
   *   <li> LIMIT_REACHED "ResourceAttribute.*" - attribute string too long
   *   </li>
   *   <li> PERMISSION_DENIED "Resource" - private resource, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Resource.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  i32 updateResource(1: string authenticationToken,
                     2: Types.Resource resource)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns binary data of the resource with the provided GUID.  For
   * example, if this were an image resource, this would contain the
   * raw bits of the image.
   * If the Resource is found in a public notebook, the authenticationToken
   * will be ignored (so it could be an empty string).
   *
   * @param guid
   *   The GUID of the resource to be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Resource.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Resource" - private resource, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Resource.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  binary getResourceData(1: string authenticationToken,
                         2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns the current state of a resource, referenced by containing
   * note GUID and resource content hash.
   *
   * @param noteGuid
   *   The GUID of the note that holds the resource to be retrieved.
   *
   * @param contentHash
   *   The MD5 checksum of the resource within that note. Note that
   *   this is the binary checksum, for example from Resource.data.bodyHash,
   *   and not the hex-encoded checksum that is used within an en-media
   *   tag in a note body.
   *
   * @param withData
   *   If true, the Resource will include the binary contents of the
   *   'data' field's body.
   *
   * @param withRecognition
   *   If true, the Resource will include the binary contents of the
   *   'recognition' field's body.
   *
   * @param withAlternateData
   *   If true, the Resource will include the binary contents of the
   *   'alternateData' field's body, if an alternate form is present.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "Note.guid" - noteGuid param missing
   *   </li>
   *   <li> DATA_REQUIRED "Note.contentHash" - contentHash param missing
   *   </li>
   *   <li> PERMISSION_DENIED "Resource" - private resource, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note" - not found, by guid
   *   </li>
   *   <li> "Resource" - not found, by hash
   *   </li>
   * </ul>
   */
  Types.Resource getResourceByHash(1: string authenticationToken,
                                   2: Types.Guid noteGuid,
                                   3: binary contentHash,
                                   4: bool withData,
                                   5: bool withRecognition,
                                   6: bool withAlternateData)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns the binary contents of the recognition index for the resource
   * with the provided GUID.  If the caller asks about a resource that has
   * no recognition data, this will throw EDAMNotFoundException.
   * If the Resource is found in a public notebook, the authenticationToken
   * will be ignored (so it could be an empty string).
   *
   * @param guid
   *   The GUID of the resource whose recognition data should be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Resource.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Resource" - private resource, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Resource.guid" - not found, by GUID
   *   </li>
   *   <li> "Resource.recognition" - resource has no recognition
   *   </li>
   * </ul>
   */
  binary getResourceRecognition(1: string authenticationToken,
                                2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * If the Resource with the provided GUID has an alternate data representation
   * (indicated via the Resource.alternateData field), then this request can
   * be used to retrieve the binary contents of that alternate data file.
   * If the caller asks about a resource that has no alternate data form, this
   * will throw EDAMNotFoundException.
   *
   * @param guid
   *    The GUID of the resource whose recognition data should be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Resource.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Resource" - private resource, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Resource.guid" - not found, by GUID
   *   </li>
   *   <li> "Resource.alternateData" - resource has no recognition
   *   </li>
   * </ul>
   */
  binary getResourceAlternateData(1: string authenticationToken,
                                  2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Returns the set of attributes for the Resource with the provided GUID.
   * If the Resource is found in a public notebook, the authenticationToken
   * will be ignored (so it could be an empty string).
   *
   * @param guid
   *   The GUID of the resource whose attributes should be retrieved.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Resource.guid" - if the parameter is missing
   *   </li>
   *   <li> PERMISSION_DENIED "Resource" - private resource, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Resource.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  Types.ResourceAttributes getResourceAttributes(1: string authenticationToken,
                                                 2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),


  /**
   * <p>
   * Looks for a user account with the provided userId on this NoteStore
   * shard and determines whether that account contains a public notebook
   * with the given URI.  If the account is not found, or no public notebook
   * exists with this URI, this will throw an EDAMNotFoundException,
   * otherwise this will return the information for that Notebook.
   * </p>
   * <p>
   * If a notebook is visible on the web with a full URL like
   * http://www.evernote.com/pub/sethdemo/api
   * Then 'sethdemo' is the username that can be used to look up the userId,
   * and 'api' is the publicUri.
   * </p>
   *
   * @param userId
   *    The numeric identifier for the user who owns the public notebook.
   *    To find this value based on a username string, you can invoke
   *    UserStore.getPublicUserInfo
   *
   * @param publicUri
   *    The uri string for the public notebook, from Notebook.publishing.uri.
   *
   * @throws EDAMNotFoundException <ul>
   *   <li>"Publishing.uri" - not found, by URI</li>
   * </ul>
   *
   * @throws EDAMSystemException <ul>
   *   <li> TAKEN_DOWN "PublicNotebook" - The specified public notebook is
   *     taken down (for all requesters).</li>
   *   <li> TAKEN_DOWN "Country" - The specified public notebook is taken
   *     down for the requester because of an IP-based country lookup.</li>
   * </ul>
   */
  Types.Notebook getPublicNotebook(1: Types.UserID userId,
                                   2: string publicUri)
    throws (1: Errors.EDAMSystemException systemException,
            2: Errors.EDAMNotFoundException notFoundException),


  /**
   * @Deprecated for first-party clients. See createOrUpdateNotebookShares.
   *
   * Share a notebook with an email address, and optionally to a specific
   * recipient. If an existing SharedNotebook associated with
   * sharedNotebook.notebookGuid is found by recipientUsername or email, then
   * the values of sharedNotebook will be used to update the existing record,
   * else a new record will be created.
   *
   * If recipientUsername is set and there is already a SharedNotebook
   * for that Notebook with that recipientUsername and the privileges on the
   * existing notebook are lower, than on this one, this will update the
   * privileges and sharerUserId. If there isn't an existing SharedNotebook for
   * recipientUsername, this will create and return a shared notebook for that
   * email and recipientUsername. If recipientUsername is not set and there
   * already is a SharedNotebook for a Notebook for that email address and the
   * privileges on the existing SharedNotebook are lower than on this one, this
   * will update the privileges and sharerUserId, and return the updated
   * SharedNotebook. Otherwise, this will create and return a SharedNotebook for
   * the email address.
   *
   * If the authenticationToken is a Business auth token, recipientUsername is
   * set and the recipient is in the same business as the business auth token,
   * this method will also auto-join the business user to the SharedNotebook -
   * that is it will set serviceJoined on the SharedNotebook and create a
   * LinkedNotebook on the recipient's account pointing to the SharedNotebook.
   * The LinkedNotebook creation happens out-of-band, so there will be a delay
   * on the order of half a minute between the SharedNotebook and LinkedNotebook
   * creation.
   *
   * Also handles sending an email to the email addresses: if a SharedNotebook
   * is being created, this will send the shared notebook invite email, and
   * if a SharedNotebook already exists, it will send the shared notebook
   * reminder email. Both these emails contain a link to join the notebook.
   * If the notebook is being auto-joined, it sends an email with that
   * information to the recipient.
   *
   * @param authenticationToken
   *   Must be an authentication token from the owner or a shared notebook
   *   authentication token or business authentication token with sufficient
   *   permissions to change invitations for a notebook.
   *
   * @param sharedNotebook
   *   A shared notebook object populated with the email address of the share
   *   recipient, the notebook guid and the access permissions. All other
   *   attributes of the shared object are ignored. The SharedNotebook.allowPreview
   *   field must be explicitly set with either a true or false value.
   *
   * @param message
   *   The sharer-defined message to put in the email sent out.
   *
   * @return
   *   The fully populated SharedNotebook object including the server assigned
   *   globalId which can both be used to uniquely identify the SharedNotebook.
   *
   * @throws EDAMUserException <ul>
   *   <li>BAD_DATA_FORMAT "SharedNotebook.email" - if the email was not valid</li>
   *   <li>DATA_REQUIRED "SharedNotebook.privilege" - if the
   *       SharedNotebook.privilegeLevel was not set.</li>
   *   <li>BAD_DATA_FORMAT "SharedNotebook.requireLogin" - if requireLogin was
   *       set. requireLogin is deprecated.</li>
   *   <li>BAD_DATA_FORMAT "SharedNotebook.privilegeLevel" - if the
   *       SharedNotebook.privilegeLevel field was unset or set to GROUP.</li>
   *   <li>PERMISSION_DENIED "user" - if the email address on the authenticationToken's
           owner's account is not confirmed.</li>
   *   <li>PERMISSION_DENIED "SharedNotebook.recipientSettings" - if
   *       recipientSettings is set in the sharedNotebook.  Only the recipient
   *       can set these values via the setSharedNotebookRecipientSettings
   *       method.</li>
   *   <li>EDAMErrorCode.LIMIT_REACHED "SharedNotebook" - The notebook already has
   *       EDAM_NOTEBOOK_SHARED_NOTEBOOK_MAX shares.</li>
   *   </ul>
   * @throws EDAMNotFoundException <ul>
   *   <li>Notebook.guid - if the notebookGuid is not a valid GUID for the user.
   *   </li>
   *   </ul>
   */
  Types.SharedNotebook shareNotebook(1: string authenticationToken,
                                     2: Types.SharedNotebook sharedNotebook,
                                     3: string message)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Share a notebook by a messaging thread ID or a list of contacts. This function is
   * intended to be used in conjunction with Evernote messaging, and as such does not
   * notify the recipient that a notebook has been shared with them.
   *
   * Sharing with a subset of participants on a thread is accomplished by specifying both
   * a thread ID and a list of contacts. This ensures that even if those contacts are
   * on the thread under a deactivated identity, the correct user (the one who has the
   * given contact on the thread) receives the share.
   *
   * @param authenticationToken
   *   An authentication token that grants the caller permission to share the notebook.
   *   This should be an owner token if the notebook is owned by the caller.
   *   If the notebook is a business notebook to which the caller has full access,
   *   this should be their business authentication token. If the notebook is a shared
   *   (non-business) notebook to which the caller has full access, this should be the
   *   shared notebook authentication token returned by NoteStore.authenticateToNotebook.
   *
   * @param shareTemplate
   *   Specifies the GUID of the notebook to be shared, the privilege at which the notebook
   *   should be shared, and the recipient information.
   *
   * @return
   *   A structure containing the USN of the Notebook after the change and a list of created
   *   or updated SharedNotebooks.
   *
   * @throws EDAMUserException <ul>
   *   <li>DATA_REQUIRED "Notebook.guid" - if no notebook GUID was specified</li>
   *   <li>BAD_DATA_FORMAT "Notebook.guid" - if shareTemplate.notebookGuid is not a
   *     valid GUID</li>
   *   <li>DATA_REQUIRED "shareTemplate" - if the shareTemplate parameter was missing</li>
   *   <li>DATA_REQUIRED "NotebookShareTemplate.privilege" - if no privilege was
   *     specified</li>
   *   <li>DATA_CONFLICT "NotebookShareTemplate.privilege" - if the specified privilege
   *     is not allowed.</li>
   *   <li>DATA_REQUIRED "NotebookShareTemplate.recipients" - if no recipients were
   *     specified, either by thread ID or as a list of contacts</li>
   *   <li>LIMIT_REACHED "SharedNotebook" - if the notebook has reached its maximum
   *     number of shares</li>
   * </ul>
   *
   * @throws EDAMInvalidContactsException <ul>
   *   <li>"NotebookShareTemplate.recipients" - if one or more of the recipients specified
   *     in shareTemplate.recipients was not syntactically valid, or if attempting to
   *     share a notebook with an Evernote identity that the sharer does not have a
   *     connection to. The exception will specify which recipients were invalid.</li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li>"Notebook.guid" - if no notebook with the specified GUID was found</li>
   *   <li>"NotebookShareTemplate.recipientThreadId" - if the recipient thread ID was
   *     specified, but no thread with that ID exists</li>
   * </ul>
   */
   CreateOrUpdateNotebookSharesResult
     createOrUpdateNotebookShares(1: string authenticationToken,
                                  2: NotebookShareTemplate shareTemplate)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException
            4: Errors.EDAMInvalidContactsException invalidContactsException),

  /**
   * @Deprecated See createOrUpdateNotebookShares and manageNotebookShares.
   */
  i32  updateSharedNotebook(1: string authenticationToken,
                            2: Types.SharedNotebook sharedNotebook)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Set values for the recipient settings associated with a notebook share. Only the
   * recipient of the share can update their recipient settings.
   *
   * If you do <i>not</i> wish to, or cannot, change one of the recipient settings fields,
   * you must leave that field unset in recipientSettings.
   * This method will skip that field for updates and attempt to leave the existing value as
   * it is.
   *
   * If recipientSettings.inMyList is false, both reminderNotifyInApp and reminderNotifyEmail
   * will be either left as null or converted to false (if currently true).
   *
   * To unset a notebook's stack, pass in the empty string for the stack field.
   *
   * @param authenticationToken The owner authentication token for the recipient of the share.
   *
   * @return The updated Notebook with the new recipient settings. Note that some of the
   * recipient settings may differ from what was requested. Clients should update their state
   * based on this return value.
   *
   * @throws EDAMNotFoundException <ul>
   *   <li>Notebook.guid - Thrown if the service does not have a notebook record with the
   *       notebookGuid on the given shard.</li>
   *   <li>Publishing.publishState - Thrown if the business notebook is not shared with the
   *       user and is also not published to their business.</li>
   * </ul>
   *
   * @throws EDAMUserException <ul>
   *   <li>PEMISSION_DENIED "authenticationToken" - If the owner of the given token is not
   *       allowed to set recipient settings on the specified notebook.</li>
   *   <li>DATA_CONFLICT "recipientSettings.reminderNotifyEmail" - Setting reminderNotifyEmail
   *       is allowed only for notebooks which belong to the same business as the user.</li>
   *   <li>DATA_CONFLICT "recipientSettings.inMyList" - If the request is setting inMyList
   *       to false and any of reminder* settings to true.</li>
   * </ul>
   */
  Types.Notebook setNotebookRecipientSettings(
      1: string authenticationToken,
      2: string notebookGuid,
      3: Types.NotebookRecipientSettings recipientSettings)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Lists the collection of shared notebooks for all notebooks in the
   * users account.
   *
   * @return
   *  The list of all SharedNotebooks for the user
   */
  list<Types.SharedNotebook> listSharedNotebooks(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Asks the service to make a linked notebook with the provided name, username
   * of the owner and identifiers provided. A linked notebook can be either a
   * link to a public notebook or to a private shared notebook.
   *
   * @param linkedNotebook
   *   The desired fields for the linked notebook must be provided on this
   *   object.  The name of the linked notebook must be set. Either a username
   *   uri or a shard id and share key must be provided otherwise a
   *   EDAMUserException is thrown.
   *
   * @return
   *   The newly created LinkedNotebook.  The server-side id will be
   *   saved in this object's 'id' field.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "LinkedNotebook.shareName" - missing shareName
   *   <li> BAD_DATA_FORMAT "LinkedNotebook.name" - invalid shareName length or pattern
   *   </li>
   *   <li> BAD_DATA_FORMAT "LinkedNotebook.username" - bad username format
   *   </li>
   *   <li> BAD_DATA_FORMAT "LinkedNotebook.uri" -
   *     if public notebook set but bad uri
   *   </li>
   *   <li> DATA_REQUIRED "LinkedNotebook.shardId" -
   *     if private notebook but shard id not provided
   *   </li>
   *   <li> BAD_DATA_FORMAT "LinkedNotebook.stack" - invalid stack name length or pattern
   *   </li>
   * </ul>
   *
   * @throws EDAMSystemException <ul>
   *   <li> BAD_DATA_FORMAT "LinkedNotebook.sharedNotebookGlobalId" -
   *     if a bad global identifer was set on a private notebook
   *   </li>
   * </ul>
   */
  Types.LinkedNotebook createLinkedNotebook(1: string authenticationToken,
                                            2: Types.LinkedNotebook linkedNotebook)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * @param linkedNotebook
   *   Updates the name of a linked notebook.
   *
   * @return
   *   The Update Sequence Number for this change within the account.
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "LinkedNotebook.shareName" - missing shareName
   *   </li>
   *   <li> BAD_DATA_FORMAT "LinkedNotebook.shareName" - invalid shareName length or pattern
   *   </li>
   *   <li> BAD_DATA_FORMAT "LinkedNotebook.stack" - invalid stack name length or pattern
   *   </li>
   * </ul>
   */
  i32 updateLinkedNotebook(1: string authenticationToken,
                           2: Types.LinkedNotebook linkedNotebook)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Returns a list of linked notebooks
   */
  list<Types.LinkedNotebook> listLinkedNotebooks(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Permanently expunges the linked notebook from the account.
   * <p/>
   * NOTE: This function is generally not available to third party applications.
   * Calls will result in an EDAMUserException with the error code
   * PERMISSION_DENIED.
   *
   * @param guid
   *   The LinkedNotebook.guid field of the LinkedNotebook to permanently remove
   *   from the account.
   */
  i32 expungeLinkedNotebook(1: string authenticationToken,
                            2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Asks the service to produce an authentication token that can be used to
   * access the contents of a shared notebook from someone else's account.
   * This authenticationToken can be used with the various other NoteStore
   * calls to find and retrieve notes, and if the permissions in the shared
   * notebook are sufficient, to make changes to the contents of the notebook.
   *
   * @param shareKeyOrGlobalId
   *   May be one of the following:
   *   <ul>
   *     <li>A share key for a shared notebook that was granted to some recipient
   *         Must be used if you are joining a notebook unless it was shared via
   *         createOrUpdateNotebookShares. Share keys are delivered out-of-band
   *         and are generally not available to clients. For security reasons,
   *         share keys may be invalidated at the discretion of the service.
   *     </li>
   *     <li>The shared notebook global identifier. May be used to access a
   *         notebook that is already joined.
   *     </li>
   *     <li>The Notebook GUID. May be used to access a notebook that was already
   *         joined, or to access a notebook that was shared with the recipient
   *         via createOrUpdateNotebookShares.
   *     </li>
   *   </ul>
   *
   * @param authenticationToken
   *   If a non-empty string is provided, this is the full user-based
   *   authentication token that identifies the user who is currently logged in
   *   and trying to access the shared notebook.
   *   If this string is empty, the service will attempt to authenticate to the
   *   shared notebook without any logged in user.
   *
   * @throws EDAMSystemException <ul>
   *   <li> BAD_DATA_FORMAT "shareKey" - invalid shareKey string</li>
   *   <li> INVALID_AUTH "shareKey" - bad signature on shareKey string</li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "SharedNotebook.id" - the shared notebook no longer exists</li>
   * </ul>
   *
   * @throws EDAMUserException <ul>
   *   <li> DATA_REQUIRED "authenticationToken" - the share requires login, and
   *          no valid authentication token was provided.
   *   </li>
   *   <li> PERMISSION_DENIED "SharedNotebook.username" - share requires login,
   *          and another username has already been bound to this notebook.
   *   </li>
   * </ul>
   */
  UserStore.AuthenticationResult
    authenticateToSharedNotebook(1: string shareKeyOrGlobalId,
                                 2: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * This function is used to retrieve extended information about a shared
   * notebook by a guest who has already authenticated to access that notebook.
   * This requires an 'authenticationToken' parameter which should be the
   * resut of a call to authenticateToSharedNotebook(...).
   * I.e. this is the token that gives access to the particular shared notebook
   * in someone else's account -- it's not the authenticationToken for the
   * owner of the notebook itself.
   *
   * @param authenticationToken
   *   Should be the authentication token retrieved from the reply of
   *   authenticateToSharedNotebook(), proving access to a particular shared
   *   notebook.
   *
   * @throws EDAMUserException <ul>
   *   <li> PERMISSION_DENIED "authenticationToken" -
   *          authentication token doesn't correspond to a valid shared notebook
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "SharedNotebook.id" - the shared notebook no longer exists
   *   </li>
   * </ul>
   */
  Types.SharedNotebook getSharedNotebookByAuth(1: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Attempts to send a single note to one or more email recipients.
   * <p/>
   * NOTE: This function is generally not available to third party applications.
   * Calls will result in an EDAMUserException with the error code
   * PERMISSION_DENIED.
   *
   * @param authenticationToken
   *    The note will be sent as the user logged in via this token, using that
   *    user's registered email address.  If the authenticated user doesn't
   *    have permission to read that note, the emailing will fail.
   *
   * @param parameters
   *    The note must be specified either by GUID (in which case it will be
   *    sent using the existing data in the service), or else the full Note
   *    must be passed to this call.  This also specifies the additional
   *    email fields that will be used in the email.
   *
   * @throws EDAMUserException <ul>
   *   <li> LIMIT_REACHED "NoteEmailParameters.toAddresses" -
   *     The email can't be sent because this would exceed the user's daily
   *     email limit.
   *   </li>
   *   <li> BAD_DATA_FORMAT "(email address)" -
   *     email address malformed
   *   </li>
   *   <li> DATA_REQUIRED "NoteEmailParameters.toAddresses" -
   *     if there are no To: or Cc: addresses provided.
   *   </li>
   *   <li> DATA_REQUIRED "Note.title" -
   *     if the caller provides a Note parameter with no title
   *   </li>
   *   <li> DATA_REQUIRED "Note.content" -
   *     if the caller provides a Note parameter with no content
   *   </li>
   *   <li> ENML_VALIDATION "*" - note content doesn't validate against DTD
   *   </li>
   *   <li> DATA_REQUIRED "NoteEmailParameters.note" -
   *     if no guid or note provided
   *   </li>
   *   <li> PERMISSION_DENIED "Note" - private note, user doesn't own
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID
   *   </li>
   * </ul>
   */
  void emailNote(1: string authenticationToken,
                 2: NoteEmailParameters parameters)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * If this note is not already shared publicly (via its own direct URL), then this
   * will start sharing that note.
   * This will return the secret "Note Key" for this note that
   * can currently be used in conjunction with the Note's GUID to gain direct
   * read-only access to the Note.
   * If the note is already shared, then this won't make any changes to the
   * note, and the existing "Note Key" will be returned.  The only way to change
   * the Note Key for an existing note is to stopSharingNote first, and then
   * call this function.
   *
   * @param guid
   *   The GUID of the note to be shared.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Note.guid" - if the parameter is missing</li>
   *   <li> PERMISSION_DENIED "Note" - private note, user doesn't own</li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "Note.guid" - not found, by GUID</li>
   * </ul>
   */
  string shareNote(1: string authenticationToken,
                   2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * If this note is shared publicly then this will stop sharing that note
   * and invalidate its "Note Key", so any existing URLs to access that Note
   * will stop working.
   *
   * If the Note is not shared, then this function will do nothing.
   *
   * This function does not remove invididual shares for the note. To remove
   * individual shares, see stopSharingNoteWithRecipients.
   *
   * @param guid
   *   The GUID of the note to be un-shared.
   *
   * @throws EDAMUserException <ul>
   *   <li> BAD_DATA_FORMAT "Note.guid" - if the parameter is missing</li>
   *   <li> PERMISSION_DENIED "Note" - private note, user doesn't own</li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li>"Note.guid" - not found, by GUID</li>
   * </ul>
   */
  void stopSharingNote(1: string authenticationToken,
                       2: Types.Guid guid)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Asks the service to produce an authentication token that can be used to
   * access the contents of a single Note which was individually shared
   * from someone's account.
   * This authenticationToken can be used with the various other NoteStore
   * calls to find and retrieve the Note and its directly-referenced children.
   *
   * @param guid
   *   The GUID identifying this Note on this shard.
   *
   * @param noteKey
   *   The 'noteKey' identifier from the Note that was originally created via
   *   a call to shareNote() and then given to a recipient to access.
   *
   * @param authenticationToken
   *   An optional authenticationToken that identifies the user accessing the
   *   shared note. This parameter may be required to access some shared notes.
   *
   * @throws EDAMUserException <ul>
   *   <li> PERMISSION_DENIED "Note" - the Note with that GUID is either not
   *     shared, or the noteKey doesn't match the current key for this note
   *   </li>
   *   <li> PERMISSION_DENIED "authenticationToken" - an authentication token is
   *     required to access this Note, but either no authentication token or a
   *     "non-owner" authentication token was provided.
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li> "guid" - the note with that GUID is not found
   *   </li>
   * </ul>
   *
   * @throws EDAMSystemException <ul>
   *   <li> TAKEN_DOWN "Note" - The specified shared note is taken down (for
   *     all requesters).
   *   </li>
   *   <li> TAKEN_DOWN "Country" - The specified shared note is taken down
   *     for the requester because of an IP-based country lookup.
   *   </ul>
   * </ul>
   */
  UserStore.AuthenticationResult
    authenticateToSharedNote(1: string guid,
                             2: string noteKey,
                             3: string authenticationToken)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMNotFoundException notFoundException,
            3: Errors.EDAMSystemException systemException),

  /**
   * Identify related entities on the service, such as notes,
   * notebooks, tags and users in a business related to notes or content.
   *
   * @param query
   *   The information about which we are finding related entities.
   *
   * @param resultSpec
   *   Allows the client to indicate the type and quantity of
   *   information to be returned, allowing a saving of time and
   *   bandwidth.
   *
   * @return
   *   The result of the query, with information considered
   *   to likely be relevantly related to the information
   *   described by the query.
   *
   * @throws EDAMUserException <ul>
   *   <li>BAD_DATA_FORMAT "RelatedQuery.plainText" - If you provided a
   *     a zero-length plain text value.
   *   </li>
   *   <li>BAD_DATA_FORMAT "RelatedQuery.noteGuid" - If you provided an
   *     invalid Note GUID, that is, one that does not match the constraints
   *     defined by EDAM_GUID_LEN_MIN, EDAM_GUID_LEN_MAX, EDAM_GUID_REGEX.
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.notebookGuid" - if malformed
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.tagGuids" - if any are malformed
   *   </li>
   *   <li> BAD_DATA_FORMAT "NoteFilter.words" - if search string too long
   *   </li>
   *   <li>PERMISSION_DENIED "Note" - If the caller does not have access to
   *     the note identified by RelatedQuery.noteGuid.
   *   </li>
   *   <li>PERMISSION_DENIED "authenticationToken" - If the caller has requested to
   *     findExperts in the context of a non business user (i.e. The authenticationToken
   *     is not a business auth token).
   *   </li>
   *   <li>DATA_REQUIRED "RelatedResultSpec" - If you did not not set any values
   *     in the result spec.
   *   </li>
   * </ul>
   *
   * @throws EDAMNotFoundException <ul>
   *   <li>"RelatedQuery.noteGuid" - the note with that GUID is not
   *     found, if that field has been set in the query.
   *   </li>
   * </ul>
   */
  RelatedResult findRelated(1: string authenticationToken,
                            2: RelatedQuery query,
                            3: RelatedResultSpec resultSpec)
    throws (1: Errors.EDAMUserException userException,
            2: Errors.EDAMSystemException systemException,
            3: Errors.EDAMNotFoundException notFoundException),

  /**
   * Perform the same operation as updateNote() would provided that the update
   * sequence number on the parameter Note object matches the current update sequence
   * number that the service has for the note.  If they do <i>not</i> match, then
   * <i>no</i> update is performed and the return value will have the current server
   * state in the note field and updated will be false.  If the update sequence
   * numbers between the client and server do match, then the note will be updated
   * and the note field of the return value will be returned as it would be for the
   * updateNote method.  This method allows you to check for an update to the note
   * on the service, by another client instance, from when you obtained the
   * note state as a baseline for your edits and the time when you wish to save your
   * edits.  If your client can merge the conflict, you can avoid overwriting changes
   * that were saved to the service by the other client.
   *
   * See the updateNote method for information on the exceptions and parameters for
   * this method.  The only difference is that you must have an update sequence number
   * defined on the note parameter (equal to the USN of the note as synched to the
   * client), and the following additional exceptions might be thrown.
   *
   * @throws EDAMUserException <ul>
   *   <li>DATA_REQUIRED "Note.updateSequenceNum" - If the update sequence number was
   *       not provided.  This includes a value that is set as 0.</li>
   *   <li>BAD_DATA_FORMAT "Note.updateSequenceNum" - If the note has an update
   *       sequence number that is larger than the current server value, which should
   *       not happen if your client is working correctly.</li>
   * </ul>
   */
  UpdateNoteIfUsnMatchesResult updateNoteIfUsnMatches(1: string authenticationToken,
                                                      2: Types.Note note)
     throws (1: Errors.EDAMUserException userException,
             2: Errors.EDAMNotFoundException notFoundException,
             3: Errors.EDAMSystemException systemException),

  /**
   * Manage invitations and memberships associated with a given notebook.
   *
   * <i>Note:</i> Beta method! This method is currently intended for
   * limited use by Evernote clients that have discussed using this
   * routine with the platform team.
   *
   * @param parameters A structure containing all parameters for the updates.
   *    See the structure documentation for details.
   *
   * @throws EDAMUserException <ul>
   *   <li>EDAMErrorCode.LIMIT_REACHED "SharedNotebook" - Trying to share a
   *    notebook while the notebook already has EDAM_NOTEBOOK_SHARED_NOTEBOOK_MAX
   *    shares.</li>
   * </ul>
   */
  ManageNotebookSharesResult manageNotebookShares(1: string authenticationToken,
                                                  2: ManageNotebookSharesParameters parameters)
     throws (1: Errors.EDAMUserException userException,
             2: Errors.EDAMNotFoundException notFoundException,
             3: Errors.EDAMSystemException systemException),

  /**
   * Return the share relationships for the given notebook, including
   * both the invitations and the memberships.
   *
   * <i>Note:</i> Beta method! This method is currently intended for
   * limited use by Evernote clients that have discussed using this
   * routine with the platform team.
   */
  ShareRelationships getNotebookShares(1: string authenticationToken,
                                       2: string notebookGuid)
     throws (1: Errors.EDAMUserException userException,
             2: Errors.EDAMNotFoundException notFoundException,
             3: Errors.EDAMSystemException systemException)
}
