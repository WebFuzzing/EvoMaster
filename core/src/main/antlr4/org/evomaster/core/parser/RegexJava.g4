/*

    WARNING: This is a copy&paste of RegexEcma262

    Regex in Java and JavaScript are very similar, but not exactly the same.
    For example, in Java we can have quotes inside \Q and \E, whereas it is not
    possible in JavaScript.

    Improvements/changes in one .g4 file will likely need to be done as well on the
    other grammar.

    TODO: is there any easy way in Antlr to share rules between 2 grammars?

    Note: the extra rules for Java were written directly, as did not find any existing .g4 grammar
    to use as a reference.
    Their implementation could hence be wrong/inefficient
*/

grammar RegexJava;



//------ PARSER ------------------------------
// Parser rules have first letter in lower-case

pattern : disjunction EOF;


disjunction
 : alternative
 | alternative OR disjunction
 ;


alternative
 : term*
 ;

term
 : assertion
 | FLAG_SCOPE_OPEN
 | atom
 | atom quantifier
 ;

assertion
 : CARET
 | DOLLAR
 //TODO
//// | '\\' 'b'
//// | '\\' 'B'
//// | '(' '?' '=' disjunction ')'
//// | '(' '?' '!' disjunction ')'
 ;

quantifier
 : quantifierPrefix
 | quantifierPrefix QUESTION
 ;


quantifierPrefix
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
 : quote
 | patternCharacter+
 | DOT
 | atomEscape
 | characterClass
 | FLAG_GROUP_OPEN disjunction PAREN_close
 // capturing and non capturing groups
 | PAREN_open disjunction PAREN_close // capturing
 | PAREN_open QUESTION COLON disjunction PAREN_close // non capturing
 | NAMED_CAPTURE_GROUP_OPEN disjunction PAREN_close // named capturing
 ;

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

// Special for Java
quote
 : SLASH Q quoteBlock SLASH E
;

quoteBlock
  : quoteChar*
;

quoteChar
 : classAtom
 | SLASH
 | BRACKET_close
 | Q
 | E
;

//TODO
CharacterEscape
 : SLASH ControlEscape
 | SLASH 'c' ControlLetter
 | SLASH HexEscapeSequence
 | SLASH UnicodeEscapeSequence
 | SLASH OctalEscapeSequence
 | SLASH ('p' | 'P') BRACE_open PCharacterClassEscapeLabel BRACE_close // this is only implemented in Java at the moment
                                                        // as on JS this is allowed only while certain flags are enabled

 //| IdentityEscape
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
//fragment IdentityEscape ::
//SourceCharacter but not IdentifierPart
//<ZWJ>
//<ZWNJ>

//TODO
//DecimalEscape
// //[lookahead ∉ DecimalDigit]
// : DecimalIntegerLiteral
// ;

patternCharacter
 // SourceCharacter but not one of ^ $ \ . * + ? ( ) [ ] { } |
 //: ~[^$\\.*+?()[\]{}|]
 : BaseChar
 | COMMA
 | MINUS
 | DecimalDigit
 | E | Q
 // These are also allowed as literals when no matching pair exists
 | BRACE_close
 | BRACKET_close
 | COLON
 | INTERSECTION
 ;


INTERSECTION : '&&' ;

characterClass
    : BRACKET_open CARET classContents BRACKET_close
    | BRACKET_open classContents BRACKET_close
    ;

classContents
    : classUnion (INTERSECTION classUnion)*
    ;

classUnion
    : characterClass+                          // one or more nested classes = UNION
    | classRanges                           // bare ranges
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
 //SourceCharacter but not one of \ or ] or -
 //TODO
 //: ~[-\]\\]
 : classEscape
 | BaseChar
 | DecimalDigit
 | COMMA | CARET | DOLLAR | DOT | STAR | PLUS | QUESTION
 | PAREN_open | PAREN_close | BRACKET_open | BRACE_open | BRACE_close | OR | E | Q
 | COLON
 // should be interpreted literally:
 // As they are lexer tokens, these character sequences are captured as such. In particular these require some extra
 // steps to interpret them correctly given the context.
 // [(?iu)] -> FLAG_SCOPE_OPEN, each letter of the token should be interpreted literally.
 | FLAG_SCOPE_OPEN | FLAG_GROUP_OPEN
 | NAMED_CAPTURE_GROUP_OPEN
 ;

decimalDigits
 : DecimalDigit+
 ;

classEscape
 : atomEscape
// | SLASH 'b'
 ;

atomEscape
 : CharacterClassEscape
 | CharacterEscape
 | SyntaxEscapes
 | BackReference
 | NamedBackReference
 ;

//------ LEXER ------------------------------
// Lexer rules have first letter in upper-case

DecimalDigit
 : [0-9]
 ;

CharacterClassEscape
 //one of d D s S w W v V h H
  // v, V, h and H are java8 exclusive, they represent vertical spaces and horizaontal spaces respectively
  // see https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html for more information
  : SLASH [dDsSwWvVhH]
 ;


SyntaxEscapes
 : SLASH [^$\\.*+?()[\]{}|/\-,:<>=!]
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






