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

parser grammar RegexJavaParser;

options { tokenVocab = RegexJavaLexer; }

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
 | DOUBLE_AMPERSAND // char class intersection not supported by default in JS, only supported if "v" flag is turned on.
 ;


characterClass
    : BRACKET_open CARET classContents BRACKET_close
    | BRACKET_open classContents BRACKET_close
    ;

classContents
    : classUnion (DOUBLE_AMPERSAND classUnion)*
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
 | BackReference
 | NamedBackReference
 ;
