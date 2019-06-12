/*
  Grammar for LIKE constraints in Postgres.
  Info based on:
  https://www.postgresql.org/docs/9.5/functions-matching.html

   "If pattern does not contain percent signs or underscores,
   then the pattern only represents the string itself; in that
   case LIKE acts like the equals operator. An underscore (_)
   in pattern stands for (matches) any single character; a percent
   sign (%) matches any sequence of zero or more characters."
*/


grammar PostgresLike;

//------ PARSER ------------------------------

pattern: term*;

term
  : specialSymbol
  | baseSymbol+
  ;

specialSymbol
  : PERCENT
  | UNDERSCORE
  ;

baseSymbol
  : ESCAPED_UNDERSCORE
  | ESCAPED_PERCENT
  | CHAR
  ;

//------ LEXER ------------------------------

ESCAPED_PERCENT: '\\' PERCENT;
ESCAPED_UNDERSCORE: '\\' UNDERSCORE;

PERCENT: '%';
UNDERSCORE: '_';

CHAR: . ;
