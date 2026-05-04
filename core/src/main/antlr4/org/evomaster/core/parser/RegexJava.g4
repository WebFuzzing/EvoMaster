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
 | PAREN_open disjunction PAREN_close

 //TODO
// | '(' '?' ':' disjunction ')'
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

//TODO backreferences
// In java/js regex, you can form capture groups which capture parts of the input and then use backreferences to
// match the same thing again, for example "(a|b)\1" only matches "aa" and "bb", backreferences are numbers escaped
// which reference the capture groups by order of appearance. There are also named capture groups which work similarly.
// Currently in both Java/JS the capture groups are just regular parenthesis and do not save the matched result yet.

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
 ;


characterClass
 //TODO check if lookahead needed, or implicit in rule order resoution
 //[ [lookahead ∉ {^}] ClassRanges ]
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
 //SourceCharacter but not one of \ or ] or -
 //TODO
 //: ~[-\]\\]
 : classEscape
 | BaseChar
 | DecimalDigit
 | COMMA | CARET | DOLLAR | DOT | STAR | PLUS | QUESTION
 | PAREN_open | PAREN_close | BRACKET_open | BRACE_open | BRACE_close | OR | E | Q
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
// TODO
// | '\\' DecimalEscape
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

Q: 'Q';
E: 'E';


BaseChar
 // practically all chars but the ones used for control and digits
 : ~[0-9,^$\\.*+?()[\]{}|-]
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

//TODO
//DecimalIntegerLiteral
// : '0'
// | [1-9] DecimalDigit*
// ;








