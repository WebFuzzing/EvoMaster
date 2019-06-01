/*
    DISCLAIMER:
    Parts of this file is based on https://github.com/antlr/grammars-v4/blob/master/ecmascript/ECMAScript.g4
    MIT License (MIT), Copyright (c) 2014 by Bart Kiers

    However, we only need to handle regex, not the full JS.

    Useful links
    Literal: https://www.ecma-international.org/ecma-262/5.1/#sec-7.8.5
    Pattern: https://www.ecma-international.org/ecma-262/5.1/#sec-15.10
    Tutorial: http://meri-stuff.blogspot.com/2011/09/antlr-tutorial-expression-language.html
  */

grammar RegexEcma262;

//@parser::members {
//}
//
//@lexer::members {
//}


pattern : disjunction;


disjunction
 : alternative
 | alternative '|' disjunction
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
 : '^'
 | '$'
//FIXME: this gives issues... commented out for the moment
// | '\\' 'b'
// | '\\' 'B'
// | '(' '?' '=' disjunction ')'
// | '(' '?' '!' disjunction ')'
 ;

quantifier
 : quantifierPrefix
 | quantifierPrefix '?'
 ;


quantifierPrefix
 : '*'
 | '+'
 | '?'
 | bracketQuantifier
// | '{' DecimalDigits '}'
// | '{' DecimalDigits ',' '}'
// | '{' DecimalDigits ',' DecimalDigits '}'
 ;

bracketQuantifier
 : bracketQuantifierSingle
 | bracketQuantifierOnlyMin
 | bracketQuantifierRange
 ;

bracketQuantifierSingle
 : '{' DecimalDigits '}'
 ;


bracketQuantifierOnlyMin
 : '{' DecimalDigits ',' '}'
 ;

bracketQuantifierRange
 : '{' DecimalDigits ',' DecimalDigits '}'
 ;

atom
 : PatternCharacter+
// | '.'
 | AtomEscape
 | DecimalDigits // FIXME check this one
// | CharacterClass
// | '(' disjunction ')'
// | '(' '?' ':' disjunction ')'
 ;



//CharacterEscape
// : ControlEscape
// | 'c' ControlLetter
// | HexEscapeSequence
// | UnicodeEscapeSequence
 //| IdentityEscape
// ;

//ControlEscape
// //one of f n r t v
// : [fnrtv]
// ;

//ControlLetter
// : [a-zA-Z]
// ;


//TODO
//fragment IdentityEscape ::
//SourceCharacter but not IdentifierPart
//<ZWJ>
//<ZWNJ>


//DecimalEscape
// //[lookahead ∉ DecimalDigit]
// : DecimalIntegerLiteral
// ;




//CharacterClass
// //TODO really unsure how to handle this one
// //[ [lookahead ∉ {^}] ClassRanges ]
// //[ ^ ClassRanges ]
// : ClassRanges
// ;

//ClassRanges
// :  //[empty]
// | NonemptyClassRanges
// ;
//
//
//NonemptyClassRanges
// : ClassAtom
// | ClassAtom NonemptyClassRangesNoDash
// | ClassAtom '-' ClassAtom ClassRanges
// ;
//
//NonemptyClassRangesNoDash
// : ClassAtom
// | ClassAtomNoDash NonemptyClassRangesNoDash
// | ClassAtomNoDash '-' ClassAtom ClassRanges
// ;
//
//ClassAtom:
// | '-'
// | ClassAtomNoDash
// ;
//
//
//ClassAtomNoDash
// //SourceCharacter but not one of \ or ] or -
// : ~[-\]\\]
// | '\\' ClassEscape
// ;

//ClassEscape
// : CharacterClassEscape
//// | DecimalEscape
//// | 'b'
// //| CharacterEscape
// ;


fragment DecimalDigit
 : [0-9]
 ;

DecimalDigits
 : DecimalDigit+
 ;

AtomEscape
 : '\\' CharacterClassEscape
// | '\\' DecimalEscape
// | '\\' CharacterEscape
 ;

fragment CharacterClassEscape
 //one of d D s S w W
 : [dDsSwW]
 ;


PatternCharacter
 // SourceCharacter but not one of ^ $ \ . * + ? ( ) [ ] { } |
 : ~[^$\\.*+?()[\]{}|]
 ;




//HexEscapeSequence
// : 'x' HexDigit HexDigit
// ;
//
//DecimalIntegerLiteral
// : '0'
// | [1-9] DecimalDigit*
// ;








