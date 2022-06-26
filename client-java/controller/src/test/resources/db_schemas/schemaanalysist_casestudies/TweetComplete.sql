-- Remember that both of these commands require the use of sqlite3

-- Drop the Tweets table if it does not already exist
DROP TABLE IF EXISTS Tweets;
 
-- Drop the Expanded URLs table if it does not exist 
DROP TABLE IF EXISTS Expanded_URLS;

-- The names of the variables that are inside of the Tweets Table

    -- // "tweet_id",
    -- // 	"in_reply_to_status_id",
    -- // 	"in_reply_to_user_id",
    -- // 	"retweeted_status_id",
    -- // 	"retweeted_status_user_id",
    -- // 	"timestamp",
    -- // 	"source",
    -- // 	"text",
    -- // 	"expanded_urls"

-- Create the Tweets table
create table Tweets(tweet_id integer primary key, 
					in_reply_to_status_id integer,
					in_reply_to_user_id integer,
					retweeted_status_id integer,
					retweeted_status_user_id integer,
					timestamp datetime,
					source text,
					text varchar(140));
 
-- Create the Expanded_URLS table
create table Expanded_URLS(tweet_id integer,
							expanded_url text,
							primary key(tweet_id, expanded_url),
							foreign key(tweet_id) references Tweets(tweet_id));

