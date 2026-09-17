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

// NOTE: if IntelliJ shows errors in this file its because it does not have a recognizer for the lexer, to solve this:
// Right click the lexer file and press "Generate ANTLR Recognizer", after a few seconds it should stop showing errors.

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
 | PAREN_open QUESTION EQUAL disjunction PAREN_close                  // lookahead (?=...)
 | PAREN_open QUESTION LESS_THAN EQUAL disjunction PAREN_close        // lookbehind (?<=...)
//// | '(' '?' '!' disjunction ')'
 | StartOfInputAssertion // \A
 | EndOfInputAssertion // \z
 | EndOfInputOrFinalLineTerminatorAssertion // \Z
 | WordBoundaryAssertion // \b
 | NonWordBoundaryAssertion // \B
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
 : QUOTE_OPEN QUOTE_CONTENT? QUOTE_CLOSE? // both the content and closing the quote is optional in java regex.
;

patternCharacter
 // SourceCharacter but not one of ^ $ \ . * + ? ( ) [ ] { } |
 //: ~[^$\\.*+?()[\]{}|]
 : BaseChar
 | COMMA
 | MINUS
 | DecimalDigit
 // These are also allowed as literals when no matching pair exists
 | BRACE_close
 | BRACKET_close
 | COLON | EQUAL | LESS_THAN
 ;


characterClass
    : BRACKET_open CARET classContents BRACKET_close
    | BRACKET_open classContents BRACKET_close
    ;

classContents
    : classRanges (CC_DOUBLE_AMPERSAND classRanges)*
    ;

classRanges
 :
 | nonemptyClassRanges
 | characterClass classRanges
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
 | characterClass classRanges
 ;

classAtom
 : MINUS
 | classAtomNoDash
 ;


classAtomNoDash
 //SourceCharacter but not one of \ or ] or - or [
 //: ~[-\]\\]
 : classEscape
 | CARET
 | BaseChar // this is the CHAR_CLASS_MODE token (CC_BaseChar), so it includes all chars but \ or ] or - or [ or ^
 ;

decimalDigits
 : DecimalDigit+
 ;

classEscape
 : CharacterClassEscape   // char class
 | CharacterEscape        // single char
// | SLASH 'b'
 ;

atomEscape
 : CharacterClassEscape
 | CharacterEscape
 | BackReference
 | NamedBackReference
 | LinebreakMatcher // \R
 ;
