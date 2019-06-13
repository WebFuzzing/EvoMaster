/*
  Grammar for SIMILAR TO constraints in Postgres.
  Info based on:
  https://www.postgresql.org/docs/9.5/functions-matching.html

  "SQL regular expressions are a curious cross between LIKE notation and common
   regular expression notation.
   Like LIKE, the SIMILAR TO operator succeeds only if its pattern matches the entire string;
   this is unlike common regular expression behavior where the pattern can match any part of
   the string. Also like LIKE, SIMILAR TO uses _ and % as wildcard characters denoting any
   single character and any string, respectively (these are comparable to . and .* in
   POSIX regular expressions)."

   Grammar is inspired from the ECMA 262 one.
*/

grammar PostgresSimilarTo;



//------ PARSER ------------------------------
// Parser rules have first letter in lower-case

pattern : disjunction;


disjunction
 : alternative
 | alternative OR disjunction
 ;


alternative
 : term*
 ;

term
 : atom
 | atom quantifier
 ;


quantifier
 : STAR
 | PLUS
 | QUESTION
 | bracketQuantifier
 ;

bracketQuantifier
 : bracketQuantifierSingle
 | bracketQuantifierOnlyMin
 | bracketQuantifierRange
 ;

bracketQuantifierSingle
 : BRACE_open decimalDigits BRACE_close
 ;


bracketQuantifierOnlyMin
 : BRACE_open decimalDigits COMMA BRACE_close
 ;

bracketQuantifierRange
 : BRACE_open decimalDigits COMMA decimalDigits BRACE_close
 ;

atom
 : patternCharacter+
 | UNDERSCORE
 | PERCENT
 | characterClass
 | PAREN_open disjunction PAREN_close
;



patternCharacter
 : BaseChar
 | MINUS
 | DecimalDigit
 ;


characterClass
 : BRACKET_open CARET classRanges BRACKET_close
 | BRACKET_open classRanges BRACKET_close
 ;

classRanges
 :
 | nonemptyClassRanges
 ;


nonemptyClassRanges
 : classAtom
 | classAtom nonemptyClassRangesNoDash
 | classAtom MINUS classAtom classRanges
 ;

nonemptyClassRangesNoDash
 : classAtom
 | classAtomNoDash nonemptyClassRangesNoDash
 | classAtomNoDash MINUS classAtom classRanges
 ;

classAtom
 : MINUS
 | classAtomNoDash
 ;


classAtomNoDash
 : BaseChar
 | DecimalDigit
 | COMMA | CARET  | SLASH  | STAR | PLUS | QUESTION
 | PAREN_open | PAREN_close | BRACKET_open | BRACE_open | BRACE_close | OR;


decimalDigits
 : DecimalDigit+
 ;



//------ LEXER ------------------------------
// Lexer rules have first letter in upper-case

DecimalDigit
 : [0-9]
 ;



CARET                      : '^';
SLASH                      : '\\';
STAR                       : '*';
PLUS                       : '+';
QUESTION                   : '?';
PAREN_open                 : '(';
PAREN_close                : ')';
BRACKET_open               : '[';
BRACKET_close              : ']';
BRACE_open                 : '{';
BRACE_close                : '}';
OR                         : '|';
MINUS                      : '-';
COMMA                      : ',';
UNDERSCORE                 : '_';
PERCENT                    : '%';

BaseChar: ~[%_0-9,^*+?()[\]{}|-] ;

