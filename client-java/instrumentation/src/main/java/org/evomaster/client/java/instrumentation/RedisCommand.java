package org.evomaster.client.java.instrumentation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Info related to Redis commands execution.
 */
public class RedisCommand implements Serializable {
    /**
     * Redis commands we'd like to capture. Extendable to any other command in Redis that may be of interest.
     */
    public enum RedisCommandType {
        /**
         * Container command for Access Control List management (CAT, DELUSER, GETUSER, LIST, LOAD, SAVE, SETUSER, WHOAMI subcommands).
         * <a href="https://redis.io/docs/latest/commands/acl/">ACL Documentation</a>
         */
        ACL("acl", "server", false),
        /**
         * If key already exists and is a string, this command appends the value at the end of the string.
         * <a href="https://redis.io/docs/latest/commands/append/">APPEND Documentation</a>
         */
        APPEND("append", "string", false),
        /**
         * Used in Redis Cluster to signal that a client is willing to get served by a slot that is in migrating state.
         * <a href="https://redis.io/docs/latest/commands/asking/">ASKING Documentation</a>
         */
        ASKING("asking", "cluster", false),
        /**
         * Authenticates the current connection against the server's requirepass, or against a specific user's password with ACL.
         * <a href="https://redis.io/docs/latest/commands/auth/">AUTH Documentation</a>
         */
        AUTH("auth", "connection", false),
        /**
         * Instructs Redis to start an Append Only File rewrite process in the background.
         * <a href="https://redis.io/docs/latest/commands/bgrewriteaof/">BGREWRITEAOF Documentation</a>
         */
        BGREWRITEAOF("bgrewriteaof", "server", false),
        /**
         * Saves the dataset to disk in the background, forking a child process that persists the data and then exits.
         * <a href="https://redis.io/docs/latest/commands/bgsave/">BGSAVE Documentation</a>
         */
        BGSAVE("bgsave", "server", false),
        /**
         * Count the number of set bits (population counting) in a string.
         * <a href="https://redis.io/docs/latest/commands/bitcount/">BITCOUNT Documentation</a>
         */
        BITCOUNT("bitcount", "string", false),
        /**
         * Treats a Redis string as an array of bits, and is capable of addressing specific integer fields of varying bit widths.
         * <a href="https://redis.io/docs/latest/commands/bitfield/">BITFIELD Documentation</a>
         */
        BITFIELD("bitfield", "string", false),
        /**
         * Read-only variant of BITFIELD, guaranteed to never perform writes even sub-commands seemingly not intended to.
         * <a href="https://redis.io/docs/latest/commands/bitfield_ro/">BITFIELD_RO Documentation</a>
         */
        BITFIELD_RO("bitfield_ro", "string", false),
        /**
         * Perform a bitwise operation between multiple keys and store the result in the destination key.
         * <a href="https://redis.io/docs/latest/commands/bitop/">BITOP Documentation</a>
         */
        BITOP("bitop", "string", false),
        /**
         * Returns the position of the first bit set to 1 or 0 in a string.
         * <a href="https://redis.io/docs/latest/commands/bitpos/">BITPOS Documentation</a>
         */
        BITPOS("bitpos", "string", false),
        /**
         * Blocking variant of LMOVE, blocks the connection when there are no elements to pop from the source list.
         * <a href="https://redis.io/docs/latest/commands/blmove/">BLMOVE Documentation</a>
         */
        BLMOVE("blmove", "list", false),
        /**
         * Blocking variant of LMPOP, blocks the connection when there are no elements to pop from any of the given lists.
         * <a href="https://redis.io/docs/latest/commands/blmpop/">BLMPOP Documentation</a>
         */
        BLMPOP("blmpop", "list", false),
        /**
         * Blocking variant of LPOP, blocks the connection when there are no elements to pop from any of the given lists.
         * <a href="https://redis.io/docs/latest/commands/blpop/">BLPOP Documentation</a>
         */
        BLPOP("blpop", "list", false),
        /**
         * Blocking variant of RPOP, blocks the connection when there are no elements to pop from any of the given lists.
         * <a href="https://redis.io/docs/latest/commands/brpop/">BRPOP Documentation</a>
         */
        BRPOP("brpop", "list", false),
        /**
         * Blocking variant of RPOPLPUSH, blocks the connection when there are no elements to pop from source.
         * <a href="https://redis.io/docs/latest/commands/brpoplpush/">BRPOPLPUSH Documentation</a>
         */
        BRPOPLPUSH("brpoplpush", "list", false),
        /**
         * Blocking variant of ZMPOP, blocks the connection when there are no members to pop from any of the given sorted sets.
         * <a href="https://redis.io/docs/latest/commands/bzmpop/">BZMPOP Documentation</a>
         */
        BZMPOP("bzmpop", "zset", false),
        /**
         * Blocking variant of ZPOPMAX, blocks the connection when there are no members to pop from any of the given sorted sets.
         * <a href="https://redis.io/docs/latest/commands/bzpopmax/">BZPOPMAX Documentation</a>
         */
        BZPOPMAX("bzpopmax", "zset", false),
        /**
         * Blocking variant of ZPOPMIN, blocks the connection when there are no members to pop from any of the given sorted sets.
         * <a href="https://redis.io/docs/latest/commands/bzpopmin/">BZPOPMIN Documentation</a>
         */
        BZPOPMIN("bzpopmin", "zset", false),
        /**
         * Container command for client connection introspection and control (LIST, KILL, SETNAME, GETNAME, PAUSE, NO-EVICT, etc.).
         * <a href="https://redis.io/docs/latest/commands/client/">CLIENT Documentation</a>
         */
        CLIENT("client", "server", false),
        /**
         * Container command for cluster management and introspection (INFO, NODES, SLOTS, SHARDS, MYID subcommands).
         * <a href="https://redis.io/docs/latest/commands/cluster/">CLUSTER Documentation</a>
         */
        CLUSTER("cluster", "cluster", false),
        /**
         * Returns information about the commands supported by the Redis server (COUNT, DOCS, INFO, LIST, GETKEYS subcommands).
         * <a href="https://redis.io/docs/latest/commands/command/">COMMAND Documentation</a>
         */
        COMMAND("command", "server", false),
        /**
         * Container command for reading and altering server configuration parameters at runtime (GET, SET, REWRITE, RESETSTAT subcommands).
         * <a href="https://redis.io/docs/latest/commands/config/">CONFIG Documentation</a>
         */
        CONFIG("config", "server", false),
        /**
         * Copies the value stored at the source key to the destination key.
         * <a href="https://redis.io/docs/latest/commands/copy/">COPY Documentation</a>
         */
        COPY("copy", "key", false),
        /**
         * Return the number of keys in the currently selected database.
         * <a href="https://redis.io/docs/latest/commands/dbsize/">DBSIZE Documentation</a>
         */
        DBSIZE("dbsize", "key", false),
        /**
         * Decrements the number stored at key by one.
         * <a href="https://redis.io/docs/latest/commands/decr/">DECR Documentation</a>
         */
        DECR("decr", "string", false),
        /**
         * Decrements the number stored at key by decrement.
         * <a href="https://redis.io/docs/latest/commands/decrby/">DECRBY Documentation</a>
         */
        DECRBY("decrby", "string", false),
        /**
         * Removes the specified keys. A key is ignored if it does not exist.
         * Integer reply: the number of keys that were removed.
         * <a href="https://redis.io/docs/latest/commands/del/">DEL Documentation</a>
         */
        DEL("del", "mixed", false),
        /**
         * Flushes all previously queued commands in a transaction and restores the connection state to normal.
         * <a href="https://redis.io/docs/latest/commands/discard/">DISCARD Documentation</a>
         */
        DISCARD("discard", "transaction", false),
        /**
         * Serialize the value stored at key in a Redis-specific format and return it to the user.
         * <a href="https://redis.io/docs/latest/commands/dump/">DUMP Documentation</a>
         */
        DUMP("dump", "key", false),
        /**
         * Returns the given string message.
         * <a href="https://redis.io/docs/latest/commands/echo/">ECHO Documentation</a>
         */
        ECHO("echo", "connection", false),
        /**
         * Invoke the execution of a server-side Lua script.
         * The return value depends on the script that was executed.
         * <a href="https://redis.io/docs/latest/commands/eval/">EVAL Documentation</a>
         */
        EVAL("eval", "script", false),
        /**
         * Evaluate a script from the server's cache by its SHA1 digest.
         * The return value depends on the script that was executed.
         * <a href="https://redis.io/docs/latest/commands/evalsha/">EVALSHA Documentation</a>
         */
        EVALSHA("evalsha", "script", false),
        /**
         * Read-only variant of EVALSHA, guaranteed to never perform writes.
         * <a href="https://redis.io/docs/latest/commands/evalsha_ro/">EVALSHA_RO Documentation</a>
         */
        EVALSHA_RO("evalsha_ro", "script", false),
        /**
         * Read-only variant of EVAL, guaranteed to never perform writes.
         * <a href="https://redis.io/docs/latest/commands/eval_ro/">EVAL_RO Documentation</a>
         */
        EVAL_RO("eval_ro", "script", false),
        /**
         * Executes all previously queued commands in a transaction and restores the connection state to normal.
         * <a href="https://redis.io/docs/latest/commands/exec/">EXEC Documentation</a>
         */
        EXEC("exec", "transaction", false),
        /**
         * Returns if key exists.
         * Integer reply: the number of keys that exist from those specified as arguments.
         * <a href="https://redis.io/docs/latest/commands/exists/">EXISTS Documentation</a>
         */
        EXISTS("exists", "mixed", true),
        /**
         * Set a timeout on key, after which the key will automatically be deleted.
         * <a href="https://redis.io/docs/latest/commands/expire/">EXPIRE Documentation</a>
         */
        EXPIRE("expire", "key", false),
        /**
         * Has the same effect and semantics as EXPIRE, but instead of specifying the number of seconds, it takes an absolute Unix timestamp.
         * <a href="https://redis.io/docs/latest/commands/expireat/">EXPIREAT Documentation</a>
         */
        EXPIREAT("expireat", "key", false),
        /**
         * Returns the absolute Unix timestamp at which the given key will expire.
         * <a href="https://redis.io/docs/latest/commands/expiretime/">EXPIRETIME Documentation</a>
         */
        EXPIRETIME("expiretime", "key", false),
        /**
         * Manages a failover to a replica for high availability administrative purposes.
         * <a href="https://redis.io/docs/latest/commands/failover/">FAILOVER Documentation</a>
         */
        FAILOVER("failover", "server", false),
        /**
         * Invokes a function previously loaded via FUNCTION LOAD.
         * <a href="https://redis.io/docs/latest/commands/fcall/">FCALL Documentation</a>
         */
        FCALL("fcall", "script", false),
        /**
         * Read-only variant of FCALL, guaranteed to never perform writes.
         * <a href="https://redis.io/docs/latest/commands/fcall_ro/">FCALL_RO Documentation</a>
         */
        FCALL_RO("fcall_ro", "script", false),
        /**
         * Delete all the keys of all the existing databases, not just the currently selected one.
         * <a href="https://redis.io/docs/latest/commands/flushall/">FLUSHALL Documentation</a>
         */
        FLUSHALL("flushall", "key", false),
        /**
         * Delete all the keys of the currently selected DB.
         * <a href="https://redis.io/docs/latest/commands/flushdb/">FLUSHDB Documentation</a>
         */
        FLUSHDB("flushdb", "key", false),
        /**
         * Runs a search query on an index and performs aggregate transformations on the results.
         * <a href="https://redis.io/docs/latest/commands/ft.aggregate/">FT.AGGREGATE Documentation</a>
         */
        FT_AGGREGATE("ft.aggregate", "search", false),
        /**
         * Adds an alias to an index.
         * <a href="https://redis.io/docs/latest/commands/ft.aliasadd/">FT.ALIASADD Documentation</a>
         */
        FT_ALIASADD("ft.aliasadd", "search", false),
        /**
         * Removes an alias from an index.
         * <a href="https://redis.io/docs/latest/commands/ft.aliasdel/">FT.ALIASDEL Documentation</a>
         */
        FT_ALIASDEL("ft.aliasdel", "search", false),
        /**
         * Adds an alias to an index, removing the alias from any other index it was previously associated with.
         * <a href="https://redis.io/docs/latest/commands/ft.aliasupdate/">FT.ALIASUPDATE Documentation</a>
         */
        FT_ALIASUPDATE("ft.aliasupdate", "search", false),
        /**
         * Adds a new attribute to an existing index.
         * <a href="https://redis.io/docs/latest/commands/ft.alter/">FT.ALTER Documentation</a>
         */
        FT_ALTER("ft.alter", "search", false),
        /**
         * Container command for reading and setting RediSearch runtime configuration options (GET, SET, HELP subcommands).
         * <a href="https://redis.io/docs/latest/commands/ft.config/">FT.CONFIG Documentation</a>
         */
        FT_CONFIG("ft.config", "search", false),
        /**
         * Creates an index with the given specification.
         * <a href="https://redis.io/docs/latest/commands/ft.create/">FT.CREATE Documentation</a>
         */
        FT_CREATE("ft.create", "search", false),
        /**
         * Container command for managing cursors created by FT.AGGREGATE (READ, DEL subcommands).
         * <a href="https://redis.io/docs/latest/commands/ft.cursor/">FT.CURSOR Documentation</a>
         */
        FT_CURSOR("ft.cursor", "search", false),
        /**
         * Adds terms to a dictionary.
         * <a href="https://redis.io/docs/latest/commands/ft.dictadd/">FT.DICTADD Documentation</a>
         */
        FT_DICTADD("ft.dictadd", "search", false),
        /**
         * Deletes terms from a dictionary.
         * <a href="https://redis.io/docs/latest/commands/ft.dictdel/">FT.DICTDEL Documentation</a>
         */
        FT_DICTDEL("ft.dictdel", "search", false),
        /**
         * Dumps all terms in the given dictionary.
         * <a href="https://redis.io/docs/latest/commands/ft.dictdump/">FT.DICTDUMP Documentation</a>
         */
        FT_DICTDUMP("ft.dictdump", "search", false),
        /**
         * Deletes an index, optionally deleting the documents associated with it.
         * <a href="https://redis.io/docs/latest/commands/ft.dropindex/">FT.DROPINDEX Documentation</a>
         */
        FT_DROPINDEX("ft.dropindex", "search", false),
        /**
         * Returns the execution plan for a complex query.
         * <a href="https://redis.io/docs/latest/commands/ft.explain/">FT.EXPLAIN Documentation</a>
         */
        FT_EXPLAIN("ft.explain", "search", false),
        /**
         * Returns the execution plan for a complex query, formatted for the CLI.
         * <a href="https://redis.io/docs/latest/commands/ft.explaincli/">FT.EXPLAINCLI Documentation</a>
         */
        FT_EXPLAINCLI("ft.explaincli", "search", false),
        /**
         * Returns information and statistics about an index.
         * <a href="https://redis.io/docs/latest/commands/ft.info/">FT.INFO Documentation</a>
         */
        FT_INFO("ft.info", "search", false),
        /**
         * Runs a search or aggregate query and returns a profile of how the query was processed.
         * <a href="https://redis.io/docs/latest/commands/ft.profile/">FT.PROFILE Documentation</a>
         */
        FT_PROFILE("ft.profile", "search", false),
        /**
         * Searches the index with a textual query, returning either documents or just ids.
         * <a href="https://redis.io/docs/latest/commands/ft.search/">FT.SEARCH Documentation</a>
         */
        FT_SEARCH("ft.search", "search", false),
        /**
         * Performs spelling correction on a query, returning suggestions for misspelled terms.
         * <a href="https://redis.io/docs/latest/commands/ft.spellcheck/">FT.SPELLCHECK Documentation</a>
         */
        FT_SPELLCHECK("ft.spellcheck", "search", false),
        /**
         * Adds a suggestion string to an auto-complete suggestion dictionary.
         * <a href="https://redis.io/docs/latest/commands/ft.sugadd/">FT.SUGADD Documentation</a>
         */
        FT_SUGADD("ft.sugadd", "search", false),
        /**
         * Deletes a string from a suggestion index.
         * <a href="https://redis.io/docs/latest/commands/ft.sugdel/">FT.SUGDEL Documentation</a>
         */
        FT_SUGDEL("ft.sugdel", "search", false),
        /**
         * Gets completion suggestions for a prefix from an auto-complete suggestion dictionary.
         * <a href="https://redis.io/docs/latest/commands/ft.sugget/">FT.SUGGET Documentation</a>
         */
        FT_SUGGET("ft.sugget", "search", false),
        /**
         * Gets the size of an auto-complete suggestion dictionary.
         * <a href="https://redis.io/docs/latest/commands/ft.suglen/">FT.SUGLEN Documentation</a>
         */
        FT_SUGLEN("ft.suglen", "search", false),
        /**
         * Dumps the contents of a synonym group.
         * <a href="https://redis.io/docs/latest/commands/ft.syndump/">FT.SYNDUMP Documentation</a>
         */
        FT_SYNDUMP("ft.syndump", "search", false),
        /**
         * Updates a synonym group with additional terms.
         * <a href="https://redis.io/docs/latest/commands/ft.synupdate/">FT.SYNUPDATE Documentation</a>
         */
        FT_SYNUPDATE("ft.synupdate", "search", false),
        /**
         * Returns the distinct values indexed in a Tag field.
         * <a href="https://redis.io/docs/latest/commands/ft.tagvals/">FT.TAGVALS Documentation</a>
         */
        FT_TAGVALS("ft.tagvals", "search", false),
        /**
         * Returns a list of all existing indexes.
         * <a href="https://redis.io/docs/latest/commands/ft._list/">FT._LIST Documentation</a>
         */
        FT__LIST("ft._list", "search", false),
        /**
         * Container command for managing Redis functions, libraries of scripts stored server-side (LOAD, DELETE, LIST, DUMP subcommands).
         * <a href="https://redis.io/docs/latest/commands/function/">FUNCTION Documentation</a>
         */
        FUNCTION("function", "script", false),
        /**
         * Adds the specified geospatial items (longitude, latitude, name) to the specified key.
         * <a href="https://redis.io/docs/latest/commands/geoadd/">GEOADD Documentation</a>
         */
        GEOADD("geoadd", "geo", false),
        /**
         * Returns the distance between two members in the geospatial index represented by the sorted set.
         * <a href="https://redis.io/docs/latest/commands/geodist/">GEODIST Documentation</a>
         */
        GEODIST("geodist", "geo", false),
        /**
         * Returns valid Geohash strings representing the position of one or more elements in a geospatial data structure.
         * <a href="https://redis.io/docs/latest/commands/geohash/">GEOHASH Documentation</a>
         */
        GEOHASH("geohash", "geo", false),
        /**
         * Returns the positions (longitude, latitude) of all the specified members in the geospatial index.
         * <a href="https://redis.io/docs/latest/commands/geopos/">GEOPOS Documentation</a>
         */
        GEOPOS("geopos", "geo", false),
        /**
         * Returns the members of a geospatial index that are within the given distance from the given coordinates.
         * <a href="https://redis.io/docs/latest/commands/georadius/">GEORADIUS Documentation</a>
         */
        GEORADIUS("georadius", "geo", false),
        /**
         * Returns the members of a geospatial index within a given distance from a member already stored in the index.
         * <a href="https://redis.io/docs/latest/commands/georadiusbymember/">GEORADIUSBYMEMBER Documentation</a>
         */
        GEORADIUSBYMEMBER("georadiusbymember", "geo", false),
        /**
         * Read-only variant of GEORADIUSBYMEMBER, refuses the STORE and STOREDIST options.
         * <a href="https://redis.io/docs/latest/commands/georadiusbymember_ro/">GEORADIUSBYMEMBER_RO Documentation</a>
         */
        GEORADIUSBYMEMBER_RO("georadiusbymember_ro", "geo", false),
        /**
         * Read-only variant of GEORADIUS, refuses the STORE and STOREDIST options.
         * <a href="https://redis.io/docs/latest/commands/georadius_ro/">GEORADIUS_RO Documentation</a>
         */
        GEORADIUS_RO("georadius_ro", "geo", false),
        /**
         * Searches for members within a geospatial index in a given area, by radius or bounding box.
         * <a href="https://redis.io/docs/latest/commands/geosearch/">GEOSEARCH Documentation</a>
         */
        GEOSEARCH("geosearch", "geo", false),
        /**
         * Similar to GEOSEARCH, but stores the result in the destination key.
         * <a href="https://redis.io/docs/latest/commands/geosearchstore/">GEOSEARCHSTORE Documentation</a>
         */
        GEOSEARCHSTORE("geosearchstore", "geo", false),
        /**
         * Get the value of key.
         * <a href="https://redis.io/docs/latest/commands/get/">GET Documentation</a>
         */
        GET("get", "string", true),
        /**
         * Returns the bit value at offset in the string value stored at key.
         * <a href="https://redis.io/docs/latest/commands/getbit/">GETBIT Documentation</a>
         */
        GETBIT("getbit", "string", false),
        /**
         * Gets the value of key and deletes the key. This command is similar to GET, except for the fact that it also deletes the key on success.
         * <a href="https://redis.io/docs/latest/commands/getdel/">GETDEL Documentation</a>
         */
        GETDEL("getdel", "key", false),
        /**
         * Gets the value of key and optionally sets its expiration, similarly to SET with the EX/PX/EXAT/PXAT/PERSIST options.
         * <a href="https://redis.io/docs/latest/commands/getex/">GETEX Documentation</a>
         */
        GETEX("getex", "key", false),
        /**
         * Returns the substring of the string value stored at key, determined by the offsets start and end.
         * <a href="https://redis.io/docs/latest/commands/getrange/">GETRANGE Documentation</a>
         */
        GETRANGE("getrange", "string", false),
        /**
         * Atomically sets key to value and returns the old value stored at key.
         * <a href="https://redis.io/docs/latest/commands/getset/">GETSET Documentation</a>
         */
        GETSET("getset", "string", false),
        /**
         * Removes the specified fields from the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hdel/">HDEL Documentation</a>
         */
        HDEL("hdel", "hash", false),
        /**
         * Switches the connection's protocol version and returns information about the server and connection.
         * <a href="https://redis.io/docs/latest/commands/hello/">HELLO Documentation</a>
         */
        HELLO("hello", "connection", false),
        /**
         * Returns if field is an existing field in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hexists/">HEXISTS Documentation</a>
         */
        HEXISTS("hexists", "hash", false),
        /**
         * Returns the value associated with field in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hget/">HGET Documentation</a>
         */
        HGET("hget", "hash", true),
        /**
         * Returns all fields and values of the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hgetall/">HGETALL Documentation</a>
         */
        HGETALL("hgetall", "hash", true),
        /**
         * Increments the number stored at field in the hash stored at key by increment.
         * <a href="https://redis.io/docs/latest/commands/hincrby/">HINCRBY Documentation</a>
         */
        HINCRBY("hincrby", "hash", false),
        /**
         * Increment the specified field of a hash stored at key, and representing a floating point number, by the specified increment.
         * <a href="https://redis.io/docs/latest/commands/hincrbyfloat/">HINCRBYFLOAT Documentation</a>
         */
        HINCRBYFLOAT("hincrbyfloat", "hash", false),
        /**
         * Returns all field names in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hkeys/">HKEYS Documentation</a>
         */
        HKEYS("hkeys", "hash", false),
        /**
         * Returns the number of fields contained in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hlen/">HLEN Documentation</a>
         */
        HLEN("hlen", "hash", false),
        /**
         * Returns the values associated with the specified fields in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hmget/">HMGET Documentation</a>
         */
        HMGET("hmget", "hash", false),
        /**
         * Deprecated alias for HSET, sets the specified fields to their respective values in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hmset/">HMSET Documentation</a>
         */
        HMSET("hmset", "hash", false),
        /**
         * Returns one or more random fields from the hash value stored at key.
         * <a href="https://redis.io/docs/latest/commands/hrandfield/">HRANDFIELD Documentation</a>
         */
        HRANDFIELD("hrandfield", "hash", false),
        /**
         * Iterates fields of a hash and their associated values, cursor-based, without blocking the server.
         * <a href="https://redis.io/docs/latest/commands/hscan/">HSCAN Documentation</a>
         */
        HSCAN("hscan", "hash", false),
        /**
         * Sets the specified fields to their respective values in the hash stored at key.
         * This command overwrites the values of specified fields that exist in the hash.
         * If key doesn't exist, a new key holding a hash is created.
         * <a href="https://redis.io/docs/latest/commands/hset/">HSET Documentation</a>
         */
        HSET("hset", "hash", false),
        /**
         * Sets field in the hash stored at key to value, only if field does not yet exist.
         * <a href="https://redis.io/docs/latest/commands/hsetnx/">HSETNX Documentation</a>
         */
        HSETNX("hsetnx", "hash", false),
        /**
         * Returns the string length of the value associated with field in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hstrlen/">HSTRLEN Documentation</a>
         */
        HSTRLEN("hstrlen", "hash", false),
        /**
         * Returns all values in the hash stored at key.
         * <a href="https://redis.io/docs/latest/commands/hvals/">HVALS Documentation</a>
         */
        HVALS("hvals", "hash", false),
        /**
         * Increments the number stored at key by one.
         * If the key does not exist, it is set to 0 before performing the operation.
         * An error is returned if the key contains a value of the wrong type
         * or contains a string that can not be represented as integer.
         * This operation is limited to 64-bit signed integers.
         * <a href="https://redis.io/docs/latest/commands/incr/">INCR Documentation</a>
         */
        INCR("incr", "string", false),
        /**
         * Increments the number stored at key by increment.
         * <a href="https://redis.io/docs/latest/commands/incrby/">INCRBY Documentation</a>
         */
        INCRBY("incrby", "string", false),
        /**
         * Increment the string representing a floating point number stored at key by the specified increment.
         * <a href="https://redis.io/docs/latest/commands/incrbyfloat/">INCRBYFLOAT Documentation</a>
         */
        INCRBYFLOAT("incrbyfloat", "string", false),
        /**
         * Returns information and statistics about the server in a format that is simple to parse by computers and easy to read by humans.
         * <a href="https://redis.io/docs/latest/commands/info/">INFO Documentation</a>
         */
        INFO("info", "server", false),
        /**
         * Append one or more json values into the array at path after the last element in it.
         * <a href="https://redis.io/docs/latest/commands/json.arrappend/">JSON.ARRAPPEND Documentation</a>
         */
        JSON_ARRAPPEND("json.arrappend", "json", false),
        /**
         * Returns the index of the first occurrence of a JSON scalar value in the array at path.
         * <a href="https://redis.io/docs/latest/commands/json.arrindex/">JSON.ARRINDEX Documentation</a>
         */
        JSON_ARRINDEX("json.arrindex", "json", true),
        /**
         * Inserts the JSON scalar(s) value at the specified index in the array at path.
         * <a href="https://redis.io/docs/latest/commands/json.arrinsert/">JSON.ARRINSERT Documentation</a>
         */
        JSON_ARRINSERT("json.arrinsert", "json", false),
        /**
         * Returns the length of the array at path.
         * <a href="https://redis.io/docs/latest/commands/json.arrlen/">JSON.ARRLEN Documentation</a>
         */
        JSON_ARRLEN("json.arrlen", "json", true),
        /**
         * Removes and returns the element at the specified index in the array at path.
         * <a href="https://redis.io/docs/latest/commands/json.arrpop/">JSON.ARRPOP Documentation</a>
         */
        JSON_ARRPOP("json.arrpop", "json", false),
        /**
         * Trims the array at path to contain only the specified inclusive range of indices from start to stop.
         * <a href="https://redis.io/docs/latest/commands/json.arrtrim/">JSON.ARRTRIM Documentation</a>
         */
        JSON_ARRTRIM("json.arrtrim", "json", false),
        /**
         * Clears all values from an array or an object and sets numeric values to 0.
         * <a href="https://redis.io/docs/latest/commands/json.clear/">JSON.CLEAR Documentation</a>
         */
        JSON_CLEAR("json.clear", "json", false),
        /**
         * Debugging container command.
         * <a href="https://redis.io/docs/latest/commands/json.debug/">JSON.DEBUG Documentation</a>
         */
        JSON_DEBUG("json.debug", "json", false),
        /**
         * Deletes a value.
         * <a href="https://redis.io/docs/latest/commands/json.del/">JSON.DEL Documentation</a>
         */
        JSON_DEL("json.del", "json", false),
        /**
         * Deletes a value.
         * <a href="https://redis.io/docs/latest/commands/json.forget/">JSON.FORGET Documentation</a>
         */
        JSON_FORGET("json.forget", "json", false),
        /**
         * Gets the value at one or more paths in JSON serialized form.
         * <a href="https://redis.io/docs/latest/commands/json.get/">JSON.GET Documentation</a>
         */
        JSON_GET("json.get", "json", true),
        /**
         * Merges a given JSON value into matching paths. Consequently, JSON values at matching paths
         * are updated, deleted, or expanded with new children.
         * <a href="https://redis.io/docs/latest/commands/json.merge/">JSON.MERGE Documentation</a>
         */
        JSON_MERGE("json.merge", "json", false),
        /**
         * Returns the values at a path from one or more keys.
         * <a href="https://redis.io/docs/latest/commands/json.mget/">JSON.MGET Documentation</a>
         */
        JSON_MGET("json.mget", "json", true),
        /**
         * Sets or updates the JSON value of one or more keys.
         * <a href="https://redis.io/docs/latest/commands/json.mset/">JSON.MSET Documentation</a>
         */
        JSON_MSET("json.mset", "json", false),
        /**
         * Increments the numeric value at path by a value.
         * <a href="https://redis.io/docs/latest/commands/json.numincrby/">JSON.NUMINCRBY Documentation</a>
         */
        JSON_NUMINCRBY("json.numincrby", "json", false),
        /**
         * Multiplies the numeric value at path by a value.
         * <a href="https://redis.io/docs/latest/commands/json.nummultby/">JSON.NUMMULTBY Documentation</a>
         */
        JSON_NUMMULTBY("json.nummultby", "json", false),
        /**
         * Returns the key names of JSON objects at the paths matching a given path expression.
         * <a href="https://redis.io/docs/latest/commands/json.objkeys/">JSON.OBJKEYS Documentation</a>
         */
        JSON_OBJKEYS("json.objkeys", "json", true),
        /**
         * Returns the number of keys in JSON objects at the paths matching a given path expression.
         * <a href="https://redis.io/docs/latest/commands/json.objlen/">JSON.OBJLEN Documentation</a>
         */
        JSON_OBJLEN("json.objlen", "json", true),
        /**
         * Returns the JSON value at path in Redis Serialization Protocol (RESP).
         * <a href="https://redis.io/docs/latest/commands/json.resp/">JSON.RESP Documentation</a>
         */
        JSON_RESP("json.resp", "json", true),
        /**
         * Sets or updates the JSON value at a path.
         * <a href="https://redis.io/docs/latest/commands/json.set/">JSON.SET Documentation</a>
         */
        JSON_SET("json.set", "json", false),
        /**
         * Appends a string to JSON strings at the paths matching a given path expression.
         * <a href="https://redis.io/docs/latest/commands/json.strappend/">JSON.STRAPPEND Documentation</a>
         */
        JSON_STRAPPEND("json.strappend", "json", false),
        /**
         * Returns the length of JSON strings at the paths matching a given path expression.
         * <a href="https://redis.io/docs/latest/commands/json.strlen/">JSON.STRLEN Documentation</a>
         */
        JSON_STRLEN("json.strlen", "json", true),
        /**
         * Toggles a boolean value.
         * <a href="https://redis.io/docs/latest/commands/json.toggle/">JSON.TOGGLE Documentation</a>
         */
        JSON_TOGGLE("json.toggle", "json", false),
        /**
         * Returns the type of the JSON value at path.
         * <a href="https://redis.io/docs/latest/commands/json.type/">JSON.TYPE Documentation</a>
         */
        JSON_TYPE("json.type", "json", true),
        /**
         * Returns all keys matching pattern.
         * <a href="https://redis.io/docs/latest/commands/keys/">KEYS Documentation</a>
         */
        KEYS("keys", "none", true),
        /**
         * Returns the Unix timestamp of the last successful save to disk.
         * <a href="https://redis.io/docs/latest/commands/lastsave/">LASTSAVE Documentation</a>
         */
        LASTSAVE("lastsave", "server", false),
        /**
         * Container command for latency monitoring (LATEST, HISTORY, RESET, GRAPH, DOCTOR subcommands).
         * <a href="https://redis.io/docs/latest/commands/latency/">LATENCY Documentation</a>
         */
        LATENCY("latency", "server", false),
        /**
         * Implements the longest common subsequence algorithm between the values stored at two keys.
         * <a href="https://redis.io/docs/latest/commands/lcs/">LCS Documentation</a>
         */
        LCS("lcs", "string", false),
        /**
         * Returns the element at index in the list stored at key.
         * <a href="https://redis.io/docs/latest/commands/lindex/">LINDEX Documentation</a>
         */
        LINDEX("lindex", "list", false),
        /**
         * Inserts element in the list stored at key either before or after the reference value pivot.
         * <a href="https://redis.io/docs/latest/commands/linsert/">LINSERT Documentation</a>
         */
        LINSERT("linsert", "list", false),
        /**
         * Returns the length of the list stored at key.
         * <a href="https://redis.io/docs/latest/commands/llen/">LLEN Documentation</a>
         */
        LLEN("llen", "list", false),
        /**
         * Atomically returns and removes the first or last element of the list stored at source, and pushes it to the first or last position of the list stored at destination.
         * <a href="https://redis.io/docs/latest/commands/lmove/">LMOVE Documentation</a>
         */
        LMOVE("lmove", "list", false),
        /**
         * Pops one or more elements from the first non-empty list key from the list of provided key names.
         * <a href="https://redis.io/docs/latest/commands/lmpop/">LMPOP Documentation</a>
         */
        LMPOP("lmpop", "list", false),
        /**
         * Displays a piece of generative computer art together with the Redis version.
         * <a href="https://redis.io/docs/latest/commands/lolwut/">LOLWUT Documentation</a>
         */
        LOLWUT("lolwut", "server", false),
        /**
         * Removes and returns the first elements of the list stored at key.
         * <a href="https://redis.io/docs/latest/commands/lpop/">LPOP Documentation</a>
         */
        LPOP("lpop", "list", false),
        /**
         * Returns the index of matching elements inside a Redis list.
         * <a href="https://redis.io/docs/latest/commands/lpos/">LPOS Documentation</a>
         */
        LPOS("lpos", "list", false),
        /**
         * Insert all the specified values at the head of the list stored at key.
         * <a href="https://redis.io/docs/latest/commands/lpush/">LPUSH Documentation</a>
         */
        LPUSH("lpush", "list", false),
        /**
         * Inserts specified values at the head of the list stored at key, only if key already exists and holds a list.
         * <a href="https://redis.io/docs/latest/commands/lpushx/">LPUSHX Documentation</a>
         */
        LPUSHX("lpushx", "list", false),
        /**
         * Returns the specified elements of the list stored at key, using zero-based start and stop indexes.
         * <a href="https://redis.io/docs/latest/commands/lrange/">LRANGE Documentation</a>
         */
        LRANGE("lrange", "list", false),
        /**
         * Removes the first count occurrences of elements equal to element from the list stored at key.
         * <a href="https://redis.io/docs/latest/commands/lrem/">LREM Documentation</a>
         */
        LREM("lrem", "list", false),
        /**
         * Sets the list element at index to value.
         * <a href="https://redis.io/docs/latest/commands/lset/">LSET Documentation</a>
         */
        LSET("lset", "list", false),
        /**
         * Trim an existing list so that it will contain only the specified range of elements.
         * <a href="https://redis.io/docs/latest/commands/ltrim/">LTRIM Documentation</a>
         */
        LTRIM("ltrim", "list", false),
        /**
         * Container command for memory introspection (DOCTOR, STATS, USAGE, MALLOC-STATS, PURGE subcommands).
         * <a href="https://redis.io/docs/latest/commands/memory/">MEMORY Documentation</a>
         */
        MEMORY("memory", "server", false),
        /**
         * Returns the values of all specified keys, for every key that does not hold a string value a nil value is returned.
         * <a href="https://redis.io/docs/latest/commands/mget/">MGET Documentation</a>
         */
        MGET("mget", "string", false),
        /**
         * Atomically transfer a key from a source Redis instance to a destination Redis instance.
         * <a href="https://redis.io/docs/latest/commands/migrate/">MIGRATE Documentation</a>
         */
        MIGRATE("migrate", "key", false),
        /**
         * Container command for module management (LIST, LOAD, UNLOAD subcommands).
         * <a href="https://redis.io/docs/latest/commands/module/">MODULE Documentation</a>
         */
        MODULE("module", "server", false),
        /**
         * Streams back every command processed by the Redis server, useful for debugging.
         * <a href="https://redis.io/docs/latest/commands/monitor/">MONITOR Documentation</a>
         */
        MONITOR("monitor", "server", false),
        /**
         * Moves key from the currently selected database to the specified destination database.
         * <a href="https://redis.io/docs/latest/commands/move/">MOVE Documentation</a>
         */
        MOVE("move", "key", false),
        /**
         * Sets the given keys to their respective values, atomically.
         * <a href="https://redis.io/docs/latest/commands/mset/">MSET Documentation</a>
         */
        MSET("mset", "string", false),
        /**
         * Sets the given keys to their respective values, only if none of the keys exist.
         * <a href="https://redis.io/docs/latest/commands/msetnx/">MSETNX Documentation</a>
         */
        MSETNX("msetnx", "string", false),
        /**
         * Marks the start of a transaction block. Subsequent commands will be queued for atomic execution using EXEC.
         * <a href="https://redis.io/docs/latest/commands/multi/">MULTI Documentation</a>
         */
        MULTI("multi", "transaction", false),
        /**
         * Container command for introspecting the internal representation of Redis objects (ENCODING, FREQ, IDLETIME, REFCOUNT subcommands).
         * <a href="https://redis.io/docs/latest/commands/object/">OBJECT Documentation</a>
         */
        OBJECT("object", "key", false),
        /**
         * Remove the existing timeout on key, turning the key from volatile to persistent.
         * <a href="https://redis.io/docs/latest/commands/persist/">PERSIST Documentation</a>
         */
        PERSIST("persist", "key", false),
        /**
         * This command works exactly like EXPIRE but the time to live of the key is specified in milliseconds.
         * <a href="https://redis.io/docs/latest/commands/pexpire/">PEXPIRE Documentation</a>
         */
        PEXPIRE("pexpire", "key", false),
        /**
         * Has the same effect and semantics as EXPIREAT, but the Unix time at which the key will expire is specified in milliseconds.
         * <a href="https://redis.io/docs/latest/commands/pexpireat/">PEXPIREAT Documentation</a>
         */
        PEXPIREAT("pexpireat", "key", false),
        /**
         * Returns the absolute Unix timestamp in milliseconds at which the given key will expire.
         * <a href="https://redis.io/docs/latest/commands/pexpiretime/">PEXPIRETIME Documentation</a>
         */
        PEXPIRETIME("pexpiretime", "key", false),
        /**
         * Adds all the elements to the HyperLogLog data structure stored at the variable name specified as the key.
         * <a href="https://redis.io/docs/latest/commands/pfadd/">PFADD Documentation</a>
         */
        PFADD("pfadd", "hyperloglog", false),
        /**
         * Returns the approximated cardinality of the set observed by the HyperLogLog at key.
         * <a href="https://redis.io/docs/latest/commands/pfcount/">PFCOUNT Documentation</a>
         */
        PFCOUNT("pfcount", "hyperloglog", false),
        /**
         * Merges multiple HyperLogLog values into a unique value that approximates the cardinality of the union of the observed sets.
         * <a href="https://redis.io/docs/latest/commands/pfmerge/">PFMERGE Documentation</a>
         */
        PFMERGE("pfmerge", "hyperloglog", false),
        /**
         * Returns PONG if no argument is provided, otherwise returns a copy of the argument as a bulk string.
         * <a href="https://redis.io/docs/latest/commands/ping/">PING Documentation</a>
         */
        PING("ping", "connection", false),
        /**
         * Works exactly like SETEX with the sole difference that the expire time is specified in milliseconds instead of seconds.
         * <a href="https://redis.io/docs/latest/commands/psetex/">PSETEX Documentation</a>
         */
        PSETEX("psetex", "string", false),
        /**
         * Subscribes the client to the given patterns.
         * <a href="https://redis.io/docs/latest/commands/psubscribe/">PSUBSCRIBE Documentation</a>
         */
        PSUBSCRIBE("psubscribe", "pubsub", false),
        /**
         * Like TTL, but returns the remaining time to live of a key in milliseconds.
         * <a href="https://redis.io/docs/latest/commands/pttl/">PTTL Documentation</a>
         */
        PTTL("pttl", "key", false),
        /**
         * Posts a message to the given channel.
         * Integer reply: the number of clients that the message was sent to.
         * <a href="https://redis.io/docs/latest/commands/publish/">PUBLISH Documentation</a>
         */
        PUBLISH("publish", "pubsub", false),
        /**
         * Container command for introspecting the Pub/Sub subsystem state (CHANNELS, NUMSUB, NUMPAT subcommands).
         * <a href="https://redis.io/docs/latest/commands/pubsub/">PUBSUB Documentation</a>
         */
        PUBSUB("pubsub", "pubsub", false),
        /**
         * Unsubscribes the client from the given patterns, or from all of them if none is given.
         * <a href="https://redis.io/docs/latest/commands/punsubscribe/">PUNSUBSCRIBE Documentation</a>
         */
        PUNSUBSCRIBE("punsubscribe", "pubsub", false),
        /**
         * Return a random key from the currently selected database.
         * <a href="https://redis.io/docs/latest/commands/randomkey/">RANDOMKEY Documentation</a>
         */
        RANDOMKEY("randomkey", "key", false),
        /**
         * Enables read queries for a connection to a Redis Cluster replica node.
         * <a href="https://redis.io/docs/latest/commands/readonly/">READONLY Documentation</a>
         */
        READONLY("readonly", "cluster", false),
        /**
         * Disables read queries for a connection to a Redis Cluster replica node, reverting READONLY.
         * <a href="https://redis.io/docs/latest/commands/readwrite/">READWRITE Documentation</a>
         */
        READWRITE("readwrite", "cluster", false),
        /**
         * Renames key to newkey. It returns an error when key does not exist.
         * <a href="https://redis.io/docs/latest/commands/rename/">RENAME Documentation</a>
         */
        RENAME("rename", "key", false),
        /**
         * Renames key to newkey if newkey does not yet exist.
         * <a href="https://redis.io/docs/latest/commands/renamenx/">RENAMENX Documentation</a>
         */
        RENAMENX("renamenx", "key", false),
        /**
         * Configures the current instance as a replica of a master instance, or promotes an instance back to being a master.
         * <a href="https://redis.io/docs/latest/commands/replicaof/">REPLICAOF Documentation</a>
         */
        REPLICAOF("replicaof", "cluster", false),
        /**
         * Performs a full reset of the connection's server-side context, discarding MULTI/WATCH state and subscriptions.
         * <a href="https://redis.io/docs/latest/commands/reset/">RESET Documentation</a>
         */
        RESET("reset", "connection", false),
        /**
         * Create a key associated with a value that is obtained by deserializing the provided serialized value, obtained via DUMP.
         * <a href="https://redis.io/docs/latest/commands/restore/">RESTORE Documentation</a>
         */
        RESTORE("restore", "key", false),
        /**
         * Returns the role of the instance in the context of replication, along with additional replication information.
         * <a href="https://redis.io/docs/latest/commands/role/">ROLE Documentation</a>
         */
        ROLE("role", "server", false),
        /**
         * Removes and returns the last elements of the list stored at key.
         * <a href="https://redis.io/docs/latest/commands/rpop/">RPOP Documentation</a>
         */
        RPOP("rpop", "list", false),
        /**
         * Atomically returns and removes the last element of the list stored at source, and pushes it to the front of the list stored at destination.
         * <a href="https://redis.io/docs/latest/commands/rpoplpush/">RPOPLPUSH Documentation</a>
         */
        RPOPLPUSH("rpoplpush", "list", false),
        /**
         * Insert all the specified values at the tail of the list stored at key.
         * <a href="https://redis.io/docs/latest/commands/rpush/">RPUSH Documentation</a>
         */
        RPUSH("rpush", "list", false),
        /**
         * Inserts specified values at the tail of the list stored at key, only if key already exists and holds a list.
         * <a href="https://redis.io/docs/latest/commands/rpushx/">RPUSHX Documentation</a>
         */
        RPUSHX("rpushx", "list", false),
        /**
         * Add the specified members to the set stored at key.
         * Specified members that are already a member of this set are ignored.
         * If key does not exist, a new set is created before adding the specified members.
         * An error is returned when the value stored at key is not a set.
         * <a href="https://redis.io/docs/latest/commands/sadd/">SADD Documentation</a>
         */
        SADD("sadd", "set", false),
        /**
         * Performs a synchronous save of the dataset producing a point in time snapshot of all the data inside the Redis instance.
         * <a href="https://redis.io/docs/latest/commands/save/">SAVE Documentation</a>
         */
        SAVE("save", "server", false),
        /**
         * Iterates the set of keys in the currently selected database, cursor-based, without blocking the server.
         * <a href="https://redis.io/docs/latest/commands/scan/">SCAN Documentation</a>
         */
        SCAN("scan", "key", false),
        /**
         * Returns the set cardinality (number of elements) of the set stored at key.
         * <a href="https://redis.io/docs/latest/commands/scard/">SCARD Documentation</a>
         */
        SCARD("scard", "set", false),
        /**
         * Container command for script management (LOAD, EXISTS, FLUSH, KILL subcommands).
         * <a href="https://redis.io/docs/latest/commands/script/">SCRIPT Documentation</a>
         */
        SCRIPT("script", "script", false),
        /**
         * Returns the members of the set resulting from the difference between the first set and all the successive sets.
         * <a href="https://redis.io/docs/latest/commands/sdiff/">SDIFF Documentation</a>
         */
        SDIFF("sdiff", "set", false),
        /**
         * Stores the members of the set resulting from the difference between the first set and all the successive sets in destination.
         * <a href="https://redis.io/docs/latest/commands/sdiffstore/">SDIFFSTORE Documentation</a>
         */
        SDIFFSTORE("sdiffstore", "set", false),
        /**
         * Select the Redis logical database having the specified zero-based numeric index.
         * New connections always use the database 0.
         * <a href="https://redis.io/docs/latest/commands/select/">SELECT Documentation</a>
         */
        SELECT("select", "none", false),
        /**
         * Container command for Redis Sentinel administration.
         * <a href="https://redis.io/docs/latest/commands/sentinel/">SENTINEL Documentation</a>
         */
        SENTINEL("sentinel", "cluster", false),
        /**
         * Set key to hold the string value. If key already holds a value, it is overwritten, regardless of its type.
         * Any previous time to live associated with the key is discarded on successful SET operation.
         * <a href="https://redis.io/docs/latest/commands/set/">SET Documentation</a>
         */
        SET("set", "string", false),
        /**
         * Sets or clears the bit at offset in the string value stored at key.
         * <a href="https://redis.io/docs/latest/commands/setbit/">SETBIT Documentation</a>
         */
        SETBIT("setbit", "string", false),
        /**
         * Set key to hold the string value and set key to timeout after a given number of seconds.
         * <a href="https://redis.io/docs/latest/commands/setex/">SETEX Documentation</a>
         */
        SETEX("setex", "string", false),
        /**
         * Set key to hold string value if key does not exist.
         * <a href="https://redis.io/docs/latest/commands/setnx/">SETNX Documentation</a>
         */
        SETNX("setnx", "string", false),
        /**
         * Overwrites part of the string stored at key, starting at the specified offset.
         * <a href="https://redis.io/docs/latest/commands/setrange/">SETRANGE Documentation</a>
         */
        SETRANGE("setrange", "string", false),
        /**
         * Synchronously saves the dataset to disk (if configured) and then shuts down the server.
         * <a href="https://redis.io/docs/latest/commands/shutdown/">SHUTDOWN Documentation</a>
         */
        SHUTDOWN("shutdown", "server", false),
        /**
         * Returns the members of the set resulting from the intersection of all the given sets.
         * <a href="https://redis.io/docs/latest/commands/sinter/">SINTER Documentation</a>
         */
        SINTER("sinter", "set", true),
        /**
         * Returns the cardinality of the set which would result from the intersection of all the given sets, without actually computing it.
         * <a href="https://redis.io/docs/latest/commands/sintercard/">SINTERCARD Documentation</a>
         */
        SINTERCARD("sintercard", "set", false),
        /**
         * Stores the members of the set resulting from the intersection of all the given sets in destination.
         * <a href="https://redis.io/docs/latest/commands/sinterstore/">SINTERSTORE Documentation</a>
         */
        SINTERSTORE("sinterstore", "set", false),
        /**
         * Returns if member is a member of the set stored at key.
         * <a href="https://redis.io/docs/latest/commands/sismember/">SISMEMBER Documentation</a>
         */
        SISMEMBER("sismember", "set", false),
        /**
         * Deprecated alias for REPLICAOF, configures the current instance as a replica of a master instance, or promotes it to master.
         * <a href="https://redis.io/docs/latest/commands/slaveof/">SLAVEOF Documentation</a>
         */
        SLAVEOF("slaveof", "cluster", false),
        /**
         * Container command for reading and resetting the Redis slow queries log (GET, LEN, RESET subcommands).
         * <a href="https://redis.io/docs/latest/commands/slowlog/">SLOWLOG Documentation</a>
         */
        SLOWLOG("slowlog", "server", false),
        /**
         * Returns all the members of the set value stored at key.
         * This has the same effect as running SINTER with one argument key.
         * <a href="https://redis.io/docs/latest/commands/smembers/">SMEMBERS Documentation</a>
         */
        SMEMBERS("smembers", "set", true),
        /**
         * Returns whether each member is a member of the set stored at key.
         * <a href="https://redis.io/docs/latest/commands/smismember/">SMISMEMBER Documentation</a>
         */
        SMISMEMBER("smismember", "set", false),
        /**
         * Moves member from the set at source to the set at destination.
         * <a href="https://redis.io/docs/latest/commands/smove/">SMOVE Documentation</a>
         */
        SMOVE("smove", "set", false),
        /**
         * Returns or stores the elements contained in the list, set or sorted set at key, sorted or filtered as requested.
         * <a href="https://redis.io/docs/latest/commands/sort/">SORT Documentation</a>
         */
        SORT("sort", "key", false),
        /**
         * Read-only variant of the SORT command. Refuses the STORE option and can safely be used in read-only replicas.
         * <a href="https://redis.io/docs/latest/commands/sort_ro/">SORT_RO Documentation</a>
         */
        SORT_RO("sort_ro", "key", false),
        /**
         * Removes and returns one or more random members from the set value store at key.
         * Nil reply: if the key does not exist.
         * Bulk string reply: when called without the count argument, the removed member.
         * Array reply: when called with the count argument, a list of the removed members.
         * <a href="https://redis.io/docs/latest/commands/spop/">SPOP Documentation</a>
         */
        SPOP("spop", "set", false),
        /**
         * Posts a message to the given shard channel.
         * <a href="https://redis.io/docs/latest/commands/spublish/">SPUBLISH Documentation</a>
         */
        SPUBLISH("spublish", "pubsub", false),
        /**
         * Returns one or more random members from the set value stored at key, without removing them.
         * <a href="https://redis.io/docs/latest/commands/srandmember/">SRANDMEMBER Documentation</a>
         */
        SRANDMEMBER("srandmember", "set", false),
        /**
         * Remove the specified members from the set stored at key.
         * Specified members that are not a member of this set are ignored.
         * If key does not exist, it is treated as an empty set and this command returns 0.
         * An error is returned when the value stored at key is not a set.
         * <a href="https://redis.io/docs/latest/commands/srem/">SREM Documentation</a>
         */
        SREM("srem", "set", false),
        /**
         * Iterates elements of a set, cursor-based, without blocking the server.
         * <a href="https://redis.io/docs/latest/commands/sscan/">SSCAN Documentation</a>
         */
        SSCAN("sscan", "set", false),
        /**
         * Subscribes the client to the specified shard channels.
         * <a href="https://redis.io/docs/latest/commands/ssubscribe/">SSUBSCRIBE Documentation</a>
         */
        SSUBSCRIBE("ssubscribe", "pubsub", false),
        /**
         * Returns the length of the string value stored at key.
         * <a href="https://redis.io/docs/latest/commands/strlen/">STRLEN Documentation</a>
         */
        STRLEN("strlen", "string", false),
        /**
         * Subscribes the client to the specified channels.
         * When successful, this command doesn't return anything.
         * Instead, for each channel, one message with the first element being the string subscribe is pushed
         * as a confirmation that the command succeeded.
         * <a href="https://redis.io/docs/latest/commands/subscribe/">SUBSCRIBE Documentation</a>
         */
        SUBSCRIBE("subscribe", "pubsub", false),
        /**
         * Deprecated alias for GETRANGE, returns the substring of the string value stored at key.
         * <a href="https://redis.io/docs/latest/commands/substr/">SUBSTR Documentation</a>
         */
        SUBSTR("substr", "string", false),
        /**
         * Returns the members of the set resulting from the union of all the given sets.
         * <a href="https://redis.io/docs/latest/commands/sunion/">SUNION Documentation</a>
         */
        SUNION("sunion", "set", false),
        /**
         * Stores the members of the set resulting from the union of all the given sets in destination.
         * <a href="https://redis.io/docs/latest/commands/sunionstore/">SUNIONSTORE Documentation</a>
         */
        SUNIONSTORE("sunionstore", "set", false),
        /**
         * Unsubscribes the client from the given shard channels, or from all of them if none is given.
         * <a href="https://redis.io/docs/latest/commands/sunsubscribe/">SUNSUBSCRIBE Documentation</a>
         */
        SUNSUBSCRIBE("sunsubscribe", "pubsub", false),
        /**
         * Swaps two Redis databases, so that immediately all the clients connected to a given database will see the data of the other database.
         * <a href="https://redis.io/docs/latest/commands/swapdb/">SWAPDB Documentation</a>
         */
        SWAPDB("swapdb", "key", false),
        /**
         * Returns the current server time as a two items lists: a Unix timestamp and the amount of microseconds already elapsed in the current second.
         * <a href="https://redis.io/docs/latest/commands/time/">TIME Documentation</a>
         */
        TIME("time", "server", false),
        /**
         * Alters the last access time of the specified keys, returning the number of existing keys specified.
         * <a href="https://redis.io/docs/latest/commands/touch/">TOUCH Documentation</a>
         */
        TOUCH("touch", "key", false),
        /**
         * Returns the remaining time to live of a key that has a timeout, in seconds.
         * <a href="https://redis.io/docs/latest/commands/ttl/">TTL Documentation</a>
         */
        TTL("ttl", "key", false),
        /**
         * Returns the string representation of the type of the value stored at key.
         * <a href="https://redis.io/docs/latest/commands/type/">TYPE Documentation</a>
         */
        TYPE("type", "key", false),
        /**
         * Removes the specified keys, like DEL, but performs the memory reclamation in a different thread, non-blocking.
         * <a href="https://redis.io/docs/latest/commands/unlink/">UNLINK Documentation</a>
         */
        UNLINK("unlink", "key", false),
        /**
         * Unsubscribes the client from the given channels, or from all of them if none is given.
         * When successful, this command doesn't return anything.
         * Instead, for each channel, one message with the first element being the string unsubscribe is pushed
         * as a confirmation that the command succeeded.
         * <a href="https://redis.io/docs/latest/commands/unsubscribe/">UNSUBSCRIBE Documentation</a>
         */
        UNSUBSCRIBE("unsubscribe", "pubsub", false),
        /**
         * Flushes all the previously watched keys for a transaction.
         * <a href="https://redis.io/docs/latest/commands/unwatch/">UNWATCH Documentation</a>
         */
        UNWATCH("unwatch", "transaction", false),
        /**
         * Blocks the current client until all the previous write commands are successfully transferred and acknowledged by at least the specified number of replicas.
         * <a href="https://redis.io/docs/latest/commands/wait/">WAIT Documentation</a>
         */
        WAIT("wait", "server", false),
        /**
         * Blocks the current client until all previous write commands are successfully written to the append-only file of the local and the specified number of replicas.
         * <a href="https://redis.io/docs/latest/commands/waitaof/">WAITAOF Documentation</a>
         */
        WAITAOF("waitaof", "server", false),
        /**
         * Marks the given keys to be watched for conditional execution of a transaction.
         * <a href="https://redis.io/docs/latest/commands/watch/">WATCH Documentation</a>
         */
        WATCH("watch", "transaction", false),
        /**
         * Removes one or multiple messages from the pending entries list of a stream consumer group.
         * <a href="https://redis.io/docs/latest/commands/xack/">XACK Documentation</a>
         */
        XACK("xack", "stream", false),
        /**
         * Appends the specified stream entry to the stream at the specified key.
         * <a href="https://redis.io/docs/latest/commands/xadd/">XADD Documentation</a>
         */
        XADD("xadd", "stream", false),
        /**
         * Transfers ownership of pending stream entries that match the criteria to the specified consumer.
         * <a href="https://redis.io/docs/latest/commands/xautoclaim/">XAUTOCLAIM Documentation</a>
         */
        XAUTOCLAIM("xautoclaim", "stream", false),
        /**
         * Changes the ownership of a pending message to a different consumer, without acknowledging it.
         * <a href="https://redis.io/docs/latest/commands/xclaim/">XCLAIM Documentation</a>
         */
        XCLAIM("xclaim", "stream", false),
        /**
         * Removes the specified entries from a stream, and returns the number of entries deleted.
         * <a href="https://redis.io/docs/latest/commands/xdel/">XDEL Documentation</a>
         */
        XDEL("xdel", "stream", false),
        /**
         * Container command for consumer group management (CREATE, SETID, DESTROY, CREATECONSUMER, DELCONSUMER subcommands).
         * <a href="https://redis.io/docs/latest/commands/xgroup/">XGROUP Documentation</a>
         */
        XGROUP("xgroup", "stream", false),
        /**
         * Container command for stream introspection (STREAM, GROUPS, CONSUMERS subcommands).
         * <a href="https://redis.io/docs/latest/commands/xinfo/">XINFO Documentation</a>
         */
        XINFO("xinfo", "stream", false),
        /**
         * Returns the number of entries inside a stream.
         * <a href="https://redis.io/docs/latest/commands/xlen/">XLEN Documentation</a>
         */
        XLEN("xlen", "stream", false),
        /**
         * Fetches information about pending messages of a given consumer group.
         * <a href="https://redis.io/docs/latest/commands/xpending/">XPENDING Documentation</a>
         */
        XPENDING("xpending", "stream", false),
        /**
         * Returns the stream entries matching a given range of IDs.
         * <a href="https://redis.io/docs/latest/commands/xrange/">XRANGE Documentation</a>
         */
        XRANGE("xrange", "stream", false),
        /**
         * Reads data from one or multiple streams, only returning entries with an ID greater than the last received ID.
         * <a href="https://redis.io/docs/latest/commands/xread/">XREAD Documentation</a>
         */
        XREAD("xread", "stream", false),
        /**
         * Reads messages from a stream via a consumer group, similarly to XREAD.
         * <a href="https://redis.io/docs/latest/commands/xreadgroup/">XREADGROUP Documentation</a>
         */
        XREADGROUP("xreadgroup", "stream", false),
        /**
         * Like XRANGE, but returns entries in reverse order, and takes the range in reverse order.
         * <a href="https://redis.io/docs/latest/commands/xrevrange/">XREVRANGE Documentation</a>
         */
        XREVRANGE("xrevrange", "stream", false),
        /**
         * Trims the stream by evicting older entries if needed.
         * <a href="https://redis.io/docs/latest/commands/xtrim/">XTRIM Documentation</a>
         */
        XTRIM("xtrim", "stream", false),
        /**
         * Adds all the specified members with the specified scores to the sorted set stored at key.
         * <a href="https://redis.io/docs/latest/commands/zadd/">ZADD Documentation</a>
         */
        ZADD("zadd", "zset", false),
        /**
         * Returns the sorted set cardinality (number of elements) of the sorted set stored at key.
         * <a href="https://redis.io/docs/latest/commands/zcard/">ZCARD Documentation</a>
         */
        ZCARD("zcard", "zset", false),
        /**
         * Returns the number of elements in the sorted set at key with a score between min and max.
         * <a href="https://redis.io/docs/latest/commands/zcount/">ZCOUNT Documentation</a>
         */
        ZCOUNT("zcount", "zset", false),
        /**
         * Computes the difference between the first and all successive sorted sets and returns the result.
         * <a href="https://redis.io/docs/latest/commands/zdiff/">ZDIFF Documentation</a>
         */
        ZDIFF("zdiff", "zset", false),
        /**
         * Computes the difference between the first and all successive sorted sets and stores the result in destination.
         * <a href="https://redis.io/docs/latest/commands/zdiffstore/">ZDIFFSTORE Documentation</a>
         */
        ZDIFFSTORE("zdiffstore", "zset", false),
        /**
         * Increments the score of member in the sorted set stored at key by increment.
         * <a href="https://redis.io/docs/latest/commands/zincrby/">ZINCRBY Documentation</a>
         */
        ZINCRBY("zincrby", "zset", false),
        /**
         * Computes the intersection of the given sorted sets and returns the result.
         * <a href="https://redis.io/docs/latest/commands/zinter/">ZINTER Documentation</a>
         */
        ZINTER("zinter", "zset", false),
        /**
         * Returns the cardinality of the intersection of the given sorted sets, without actually computing it.
         * <a href="https://redis.io/docs/latest/commands/zintercard/">ZINTERCARD Documentation</a>
         */
        ZINTERCARD("zintercard", "zset", false),
        /**
         * Computes the intersection of the given sorted sets and stores the result in destination.
         * <a href="https://redis.io/docs/latest/commands/zinterstore/">ZINTERSTORE Documentation</a>
         */
        ZINTERSTORE("zinterstore", "zset", false),
        /**
         * Returns the number of elements in the sorted set at key with a value between min and max, when all elements have the same score.
         * <a href="https://redis.io/docs/latest/commands/zlexcount/">ZLEXCOUNT Documentation</a>
         */
        ZLEXCOUNT("zlexcount", "zset", false),
        /**
         * Pops one or more elements, with the highest or lowest scores, from the first non-empty sorted set from the list of provided key names.
         * <a href="https://redis.io/docs/latest/commands/zmpop/">ZMPOP Documentation</a>
         */
        ZMPOP("zmpop", "zset", false),
        /**
         * Returns the scores associated with the specified members in the sorted set stored at key.
         * <a href="https://redis.io/docs/latest/commands/zmscore/">ZMSCORE Documentation</a>
         */
        ZMSCORE("zmscore", "zset", false),
        /**
         * Removes and returns up to count members with the highest scores in the sorted set stored at key.
         * <a href="https://redis.io/docs/latest/commands/zpopmax/">ZPOPMAX Documentation</a>
         */
        ZPOPMAX("zpopmax", "zset", false),
        /**
         * Removes and returns up to count members with the lowest scores in the sorted set stored at key.
         * <a href="https://redis.io/docs/latest/commands/zpopmin/">ZPOPMIN Documentation</a>
         */
        ZPOPMIN("zpopmin", "zset", false),
        /**
         * Returns one or more random members from the sorted set value stored at key.
         * <a href="https://redis.io/docs/latest/commands/zrandmember/">ZRANDMEMBER Documentation</a>
         */
        ZRANDMEMBER("zrandmember", "zset", false),
        /**
         * Returns the specified range of elements in the sorted set stored at key.
         * <a href="https://redis.io/docs/latest/commands/zrange/">ZRANGE Documentation</a>
         */
        ZRANGE("zrange", "zset", false),
        /**
         * Returns all the elements in the sorted set at key with a value between min and max, when all elements have the same score.
         * <a href="https://redis.io/docs/latest/commands/zrangebylex/">ZRANGEBYLEX Documentation</a>
         */
        ZRANGEBYLEX("zrangebylex", "zset", false),
        /**
         * Returns all the elements in the sorted set at key with a score between min and max.
         * <a href="https://redis.io/docs/latest/commands/zrangebyscore/">ZRANGEBYSCORE Documentation</a>
         */
        ZRANGEBYSCORE("zrangebyscore", "zset", false),
        /**
         * Stores a range of members from the sorted set at source into a new sorted set at destination.
         * <a href="https://redis.io/docs/latest/commands/zrangestore/">ZRANGESTORE Documentation</a>
         */
        ZRANGESTORE("zrangestore", "zset", false),
        /**
         * Returns the rank of member in the sorted set stored at key, with the scores ordered from low to high.
         * <a href="https://redis.io/docs/latest/commands/zrank/">ZRANK Documentation</a>
         */
        ZRANK("zrank", "zset", false),
        /**
         * Removes the specified members from the sorted set stored at key.
         * <a href="https://redis.io/docs/latest/commands/zrem/">ZREM Documentation</a>
         */
        ZREM("zrem", "zset", false),
        /**
         * Removes all elements in the sorted set between the lexicographical range specified by min and max.
         * <a href="https://redis.io/docs/latest/commands/zremrangebylex/">ZREMRANGEBYLEX Documentation</a>
         */
        ZREMRANGEBYLEX("zremrangebylex", "zset", false),
        /**
         * Removes all elements in the sorted set stored at key with rank between start and stop.
         * <a href="https://redis.io/docs/latest/commands/zremrangebyrank/">ZREMRANGEBYRANK Documentation</a>
         */
        ZREMRANGEBYRANK("zremrangebyrank", "zset", false),
        /**
         * Removes all elements in the sorted set stored at key with a score between min and max.
         * <a href="https://redis.io/docs/latest/commands/zremrangebyscore/">ZREMRANGEBYSCORE Documentation</a>
         */
        ZREMRANGEBYSCORE("zremrangebyscore", "zset", false),
        /**
         * Returns the specified range of elements in the sorted set stored at key, ordered from the highest to the lowest score.
         * <a href="https://redis.io/docs/latest/commands/zrevrange/">ZREVRANGE Documentation</a>
         */
        ZREVRANGE("zrevrange", "zset", false),
        /**
         * Returns all the elements in the sorted set at key with a value between max and min, when all elements have the same score.
         * <a href="https://redis.io/docs/latest/commands/zrevrangebylex/">ZREVRANGEBYLEX Documentation</a>
         */
        ZREVRANGEBYLEX("zrevrangebylex", "zset", false),
        /**
         * Returns all the elements in the sorted set at key with a score between max and min, ordered from high to low.
         * <a href="https://redis.io/docs/latest/commands/zrevrangebyscore/">ZREVRANGEBYSCORE Documentation</a>
         */
        ZREVRANGEBYSCORE("zrevrangebyscore", "zset", false),
        /**
         * Returns the rank of member in the sorted set stored at key, with the scores ordered from high to low.
         * <a href="https://redis.io/docs/latest/commands/zrevrank/">ZREVRANK Documentation</a>
         */
        ZREVRANK("zrevrank", "zset", false),
        /**
         * Iterates elements of a sorted set and their scores, cursor-based, without blocking the server.
         * <a href="https://redis.io/docs/latest/commands/zscan/">ZSCAN Documentation</a>
         */
        ZSCAN("zscan", "zset", false),
        /**
         * Returns the score of member in the sorted set stored at key.
         * <a href="https://redis.io/docs/latest/commands/zscore/">ZSCORE Documentation</a>
         */
        ZSCORE("zscore", "zset", false),
        /**
         * Computes the union of the given sorted sets and returns the result.
         * <a href="https://redis.io/docs/latest/commands/zunion/">ZUNION Documentation</a>
         */
        ZUNION("zunion", "zset", false),
        /**
         * Computes the union of the given sorted sets and stores the result in destination.
         * <a href="https://redis.io/docs/latest/commands/zunionstore/">ZUNIONSTORE Documentation</a>
         */
        ZUNIONSTORE("zunionstore", "zset", false),

        /**
         * Default unregistered command value.
         */
        OTHER("other", "none", false);

        private final String label;
        private final String dataType;
        private final boolean calculateHeuristic;

        RedisCommandType(String label, String dataType, boolean shouldCalculateHeuristic) {
            this.label = label;
            this.dataType = dataType;
            this.calculateHeuristic = shouldCalculateHeuristic;
        }

        public String getLabel() {
            return label;
        }

        public String getDataType() {
            return dataType;
        }

        public boolean shouldCalculateHeuristic() {
            return calculateHeuristic;
        }
    }

    /**
     * Enumerated type of command.
     */
    private final RedisCommandType type;

    /**
     * Already-parsed argument values, in the order the command received them.
     */
    private final String[] args;

    /**
     * If the operation was successfully executed.
     */
    private final boolean successfullyExecuted;

    /**
     * Elapsed execution time.
     */
    private final long executionTime;

    public RedisCommand(RedisCommandType type,
                        String[] args,
                        boolean successfullyExecuted,
                        long executionTime) {
        this.type = type;
        this.args = (args != null) ? Arrays.copyOf(args, args.length) : new String[0];
        this.successfullyExecuted = successfullyExecuted;
        this.executionTime = executionTime;
    }

    public RedisCommandType getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }

    public List<String> extractArgs(){
        return Arrays.asList(args);
    }

    public boolean getSuccessfullyExecuted() {
        return successfullyExecuted;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public String toString(){
        return this.getType().getLabel() + " " + String.join(" ", this.getArgs());
    }
}
