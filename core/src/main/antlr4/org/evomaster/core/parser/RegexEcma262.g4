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

@lexer::members {
/**
 * The total capture group count, required to correctly interpret decimal escapes.
 * A decimal escape's exact meaning relies on the total number of groups in the regex.
 * To address this, {@link RegexHandler#prepareEcma262Lexer} performs a preliminary
 * lexing pass to populate {@link #captureGroups}. The lexer is then reset, and this
 * field is initialized with that count before the primary lexing pass begins.
 * @see #backReferenceAhead
 */
public int totalCaptureGroups = 0;

/**
 * How many capture groups have been found so far in this pass, to be used as total in the next one.
 */
public int captureGroups = 0;

/**
 * Indicates whether the lexer is currently processing tokens within a character class.
 */
private boolean inClass = false;

@Override
public void reset() {
    super.reset();
    // we reset the lexer with super and also reset the fields we added
    inClass = false;
    captureGroups = 0;
    totalCaptureGroups = 0;
}

/**
 * Determines if the next token sequence forms a valid group backreference.
 * A backreference is valid if it is located outside a character class, begins with
 * a backslash followed by a sequence of digits (not starting with {@code 0}), and
 * its numeric value does not exceed the total capture group count.
 */
private boolean backReferenceAhead() {
    // _input.LA(1) is the next symbol, (2) is the following one and so on (if we go out of bounds LA returns EOF)
    boolean firstCharIsBackslash = _input.LA(1) == '\\';
    boolean secondCharIsNonZeroDigit = isNonZeroDigit(_input.LA(2));
    return !inClass && firstCharIsBackslash && secondCharIsNonZeroDigit &&
            isNumberAfterBackslashAtMost(totalCaptureGroups);
}

/**
 * Checks if the sequence of numeric digits immediately following the backslash
 * represents a value less than or equal to {@code max}.
 * @see #backReferenceAhead()
 */
private boolean isNumberAfterBackslashAtMost(int max) {
    long number = 0;
    for (int i = 2; isDigit(_input.LA(i)); i++) {
        number = number * 10 + (_input.LA(i) - '0');
        if (number > max) {
            return false;
        }
    }
    return true;
}

private static boolean isNonZeroDigit(int symbol) {
    return symbol >= '1' && symbol <= '9';
}

private static boolean isDigit(int symbol) {
    return symbol >= '0' && symbol <= '9';
}
}


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
 // capturing and non capturing groups
 | PAREN_open disjunction PAREN_close // capturing
 | PAREN_open QUESTION COLON disjunction PAREN_close // non capturing
 ;


BackReference
 // We evaluate the predicate first which tells us whether what follows should be a back reference or not. If its not a
 // back reference then it is either a IdentityEscape (like \8) or OctalEscapeSequence (like \377), both of which are
 // CharacterEscape. This rule must be defined before CharacterEscape.
 : {backReferenceAhead()}? SLASH [1-9] [0-9]*
 ;
// TODO named back references

CharacterEscape
 : SLASH ControlEscape
 | SLASH HexEscapeSequence
 | SLASH UnicodeEscapeSequence
 | SLASH OctalEscapeSequence // legacy octal escapes are deprecated, but this also works for null escape (\u0000)
 | SLASH ControlLetterEscape
 | SLASH IdentityEscape
 ;

fragment ControlLetterEscape
 // This handles both control letter escapes (\ca, \cZ, etc.) and literal interpretations of \c.
 // As in JS: "\c" + [^a-zA-Z]? is taken literally as "\c" + [^a-zA-Z]? outside charclasses
 // while "\c" + [^a-zA-Z0-9_]? is taken literally as "\c" + [^a-zA-Z0-9_]? within charclasses.
 // Therefore, as all characters following "\c" (or none) are permitted we accept "\c" + .? here
 // and handle each case in visitor.
 : 'c' .?   // matches \c, \c<anything>
 ;

fragment ControlEscape
 //one of f n r t v
 : [fnrtv]
 ;

fragment IdentityEscape
 // In JS escape sequences that are not one of the above (excluding backreferences) become identity escapes:
 // they represent the character that follows the backslash. (e.g.: "\a" becomes "a")
 // see: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Regular_expressions/Character_escape#:~:text=identity%20escapes
 : ~[dDsSwWfnrtvxuc0-7bB]
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
 | COMMA
 | MINUS
 | DecimalDigit
 | COLON
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
 | COMMA | COLON | CARET | DOLLAR | DOT | STAR | PLUS | QUESTION
 | PAREN_open | PAREN_close | BRACKET_open | BRACE_open | BRACE_close | OR;


classEscape
 : CharacterClassEscape
 | CharacterEscape
 | LowerCaseBEscape
 ;

atomEscape
 : CharacterClassEscape
 | BackReference // not part of classEscape as there are no back references within classes
 | CharacterEscape
 ;

decimalDigits
 : DecimalDigit+
 ;

//------ LEXER ------------------------------
// Lexer rules have first letter in upper-case

DecimalDigit
 : [0-9]
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
PAREN_open                 : '(' {if (!inClass && _input.LA(1) != '?') captureGroups++;}; // literal within class,
                                // non capturing when this token is followed by "?", opens capture group otherwise
PAREN_close                : ')';
BRACKET_open               : '[' {inClass = true;}; // opens class or is literal within class, either way set true
BRACKET_close              : ']' {inClass = false;}; // closes class or literal outside, either way set false
BRACE_open                 : '{';
BRACE_close                : '}';
OR                         : '|';
MINUS                      : '-';
COMMA                      : ',';
COLON                      : ':';


BaseChar
 // practically all chars but the ones used for control and digits
 : ~[0-9,:^$\\.*+?()[\]{}|-]
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








