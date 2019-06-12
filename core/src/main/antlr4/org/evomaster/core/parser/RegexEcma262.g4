/*
    DISCLAIMER:
    Parts of this file is based on https://github.com/antlr/grammars-v4/blob/master/ecmascript/ECMAScript.g4
    MIT License (MIT), Copyright (c) 2014 by Bart Kiers

    However, we only need to handle regex, not the full JS.

    Useful links
    Literal: https://www.ecma-international.org/ecma-262/5.1/#sec-7.8.5
    Pattern: https://www.ecma-international.org/ecma-262/5.1/#sec-15.10
    Tutorial: http://meri-stuff.blogspot.com/2011/09/antlr-tutorial-expression-language.html


    TODO: following grammar covers most of the regex syntax, but not all

  */

grammar RegexEcma262;



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
 : patternCharacter+
 | DOT
 | AtomEscape
 | characterClass
 | PAREN_open disjunction PAREN_close
 //TODO
// | '(' '?' ':' disjunction ')'
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
 | PAREN_open | PAREN_close | BRACKET_open | BRACE_open | BRACE_close | OR;


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








