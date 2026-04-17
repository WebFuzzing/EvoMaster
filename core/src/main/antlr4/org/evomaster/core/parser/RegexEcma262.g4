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
 : patternCharacter+
 | DOT
 | atomEscape
 | characterClass
 | PAREN_open disjunction PAREN_close
 //TODO
// | '(' '?' ':' disjunction ')'
 ;


//TODO
CharacterEscape
 : SLASH ControlEscape
 | SLASH HexEscapeSequence
 | SLASH UnicodeEscapeSequence
 | SLASH OctalEscapeSequence // legacy octal escapes are deprecated, but this also works for null escape (\u0000)
 | SLASH IdentityEscape
 ;

//TODO backreferences
// In java/js regex, you can form capture groups which capture parts of the input and then use backreferences to
// match the same thing again, for example "(a|b)\1" only matches "aa" and "bb", backreferences are numbers escaped
// which reference the capture groups by order of appearance. There are also named capture groups which work similarly.
// Currently in both Java/JS the capture groups are just regular parenthesis and do not save the matched result yet.

ControlLetterExtendedEscape
 // This handles both control letter escapes (\ca, \cZ, etc.) and literal interpretations of \c.
 // As in JS: "\c" + [^a-zA-Z]? is taken literally as "\c" + [^a-zA-Z]? outside charclasses
 // while "\c" + [^a-zA-Z0-9_]? is taken literally as "\c" + [^a-zA-Z0-9_]? within charclasses.
 // Therefore, as all characters following "\c" (or none) are permitted we accept "\c" + .? here
 // and handle each case in visitor.
 : SLASH 'c' .?   // matches \c, \c<anything>
 ;

fragment ControlEscape
 //one of f n r t v
 : [fnrtv]
 ;

fragment IdentityEscape
 // In JS escape sequences that are not one of the above (excluding backreferences) become identity escapes:
 // they represent the character that follows the backslash. (e.g.: "\a" becomes "a")
 // see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Regular_expressions/Character_escape#:~:text=identity%20escapes
 : ~[dDsSwWfnrtvxuc0-9bB]
 ;

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
 : classEscape
 | BaseChar
 | DecimalDigit
 | COMMA | CARET | DOLLAR | DOT | STAR | PLUS | QUESTION
 | PAREN_open | PAREN_close | BRACKET_open | BRACE_open | BRACE_close | OR;


classEscape
 : controlLetterExtendedEscape // this needs to be first so that we can accept things like \c and \c0 within charclasses
 | atomEscape
 | LowerCaseBEscape
 ;

decimalDigits
 : DecimalDigit+
 ;

controlLetterExtendedEscape
 // we need this as a parser rule because differentiating between being inside a charclass or outside is important
 // as behavior changes in each case
 : ControlLetterExtendedEscape
 ;

//------ LEXER ------------------------------
// Lexer rules have first letter in upper-case

DecimalDigit
 : [0-9]
 ;


atomEscape
 : CharacterClassEscape
 //TODO
// | '\\' DecimalEscape
 | CharacterEscape
 | controlLetterExtendedEscape
 ;

CharacterClassEscape
 //one of d D s S w W
 : SLASH [dDsSwW]
 ;

LowerCaseBEscape
 // In JS, within charclass this is interpreted as backspace, outside it is a word boundary assert
 : SLASH 'b'
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

fragment OctalEscapeSequence
 : OctalDigit
 | OctalDigit OctalDigit
 | [0-3] OctalDigit OctalDigit
;

fragment UnicodeEscapeSequence
 : 'u' HexDigit HexDigit HexDigit HexDigit
 ;

fragment HexEscapeSequence
 : 'x' HexDigit HexDigit
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








