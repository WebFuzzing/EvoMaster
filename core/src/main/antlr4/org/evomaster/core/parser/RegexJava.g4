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

pattern : disjunction;


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
 | AtomEscape
 | characterClass
 | PAREN_open disjunction PAREN_close
 // These two rules are added to handle the . and + symbols in emails
 // A more general solution is needed for escaped control symbols in Java
 // regular expressions
 | ESCAPED_DOT
 | ESCAPED_PLUS

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
//CharacterEscape
// : ControlEscape
// | 'c' ControlLetter
// | HexEscapeSequence
// | UnicodeEscapeSequence
 //| IdentityEscape
// ;

//TODO
//ControlEscape
// //one of f n r t v
// : [fnrtv]
// ;

//TODO
//ControlLetter
// : [a-zA-Z]
// ;


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
 | MINUS
 | DecimalDigit
 | E | Q
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
// | '\\' ClassEscape
 : BaseChar
 | DecimalDigit
 | COMMA | CARET | DOLLAR | SLASH | DOT | STAR | PLUS | QUESTION
 | PAREN_open | PAREN_close | BRACKET_open | BRACE_open | BRACE_close | OR | E | Q
 | ESCAPED_DOT | ESCAPED_PLUS;


//TODO
//ClassEscape
// : CharacterClassEscape
//// | DecimalEscape
//// | 'b'
// //| CharacterEscape
// ;

decimalDigits
 : DecimalDigit+
 ;



//------ LEXER ------------------------------
// Lexer rules have first letter in upper-case

DecimalDigit
 : [0-9]
 ;


AtomEscape
 : '\\' CharacterClassEscape
 //TODO
// | '\\' DecimalEscape
// | '\\' CharacterEscape
 ;

fragment CharacterClassEscape
 //one of d D s S w W
 : [dDsSwW]
 ;


ESCAPED_PLUS               : '\\+'; // Recognize \+
ESCAPED_DOT                : '\\.'; // Recognize \-
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

//TODO
//HexEscapeSequence
// : 'x' HexDigit HexDigit
// ;
//

//TODO
//DecimalIntegerLiteral
// : '0'
// | [1-9] DecimalDigit*
// ;








