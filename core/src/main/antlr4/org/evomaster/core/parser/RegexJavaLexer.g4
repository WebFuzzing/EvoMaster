/*
    This is the lexer grammar for the RegexJava grammar, see RegexJavaParser.g4 (parser grammar) for more information.
*/

lexer grammar RegexJavaLexer;


//------ LEXER ------------------------------
// Lexer rules have first letter in upper-case

NAMED_CAPTURE_GROUP_OPEN
 : '(?<' [a-zA-Z] [a-zA-Z0-9]* '>'
 ;

FLAG_GROUP_OPEN
 : PAREN_open QUESTION [idmsuxU]+ (MINUS [idmsuxU]+)? COLON
 | PAREN_open QUESTION MINUS [idmsuxU]+ COLON
 ;

FLAG_SCOPE_OPEN
 : PAREN_open QUESTION [idmsuxU]+ (MINUS [iumdsx]+)? PAREN_close
 | PAREN_open QUESTION MINUS [idmsuxU]+ PAREN_close
 ;

CharacterEscape
 : SLASH ControlEscape
 | SLASH 'c' ControlLetter
 | SLASH HexEscapeSequence
 | SLASH UnicodeEscapeSequence
 | SLASH OctalEscapeSequence
 | SLASH ('p' | 'P') BRACE_open PCharacterClassEscapeLabel BRACE_close // this is only implemented in Java at the moment
                                                        // as on JS this is allowed only while certain flags are enabled
 | SLASH ~[a-zA-Z0-9] // identity escape
 ;

// Instead of listing all unicode scripts, blocks, etc. the parser allows anything
// then we filter by checking if the label is valid when it is used.
fragment PCharacterClassEscapeLabel
 : [0-9a-zA-Z_=]+
// posix character classes, java.lang.Character methods and Unicode scripts, blocks, categories and binary properties.
// https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#:~:text=character%3A%20%5B%5E%5Cw%5D-,POSIX,-character%20classes%20(US
// https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#jcc
// https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#usc
// https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#ubc
// https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#ucc
// https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#ubpc
 ;

fragment ControlEscape
 //one of f n r t v
 : [aefnrt]
 ;

fragment ControlLetter
 : [?-_a-z]
 ;

//TODO
//DecimalEscape
// //[lookahead ∉ DecimalDigit]
// : DecimalIntegerLiteral
// ;

DOUBLE_AMPERSAND
 : '&&'
 ;

DecimalDigit
 : [0-9]
 ;

CharacterClassEscape
 //one of d D s S w W v V h H
  // v, V, h and H are java8 exclusive, they represent vertical spaces and horizaontal spaces respectively
  // see https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html for more information
  : SLASH [dDsSwWvVhH]
 ;

CARET                      : '^';
DOLLAR                     : '$';
SLASH                      : '\\';
DOT                        : '.';
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
COLON                      : ':';

Q: 'Q';
E: 'E';

BaseChar
 // practically all chars but the ones used for control and digits
 : ~[0-9:,^$\\.*+?()[\]{}|-]
 ;

fragment OctalEscapeSequence
 : '0' OctalDigit
 | '0' OctalDigit OctalDigit
 | '0' [0-3] OctalDigit OctalDigit
;

fragment UnicodeEscapeSequence:
 'u' HexDigit HexDigit HexDigit HexDigit
;

fragment HexEscapeSequence
 : 'x' HexDigit HexDigit
 | 'x' BRACE_open HexDigit+ BRACE_close
 ;

fragment HexDigit:
 [a-fA-F0-9]
 ;

fragment OctalDigit:
 [0-7]
 ;

 // \1, \2, ... \99 etc, distinguished from \0XX octal which starts with 0
 BackReference
  : SLASH [1-9] DecimalDigit*
  ;

// \k<name>, first character must be letter, following characters may be letters or digits
NamedBackReference
 : SLASH 'k<' [a-zA-Z] [a-zA-Z0-9]* '>'
 ;
