grammar DynamoDbConditionExpression;

expression
    : orExpr EOF
    ;

orExpr
    : andExpr (OR andExpr)*
    ;

andExpr
    : notExpr (AND notExpr)*
    ;

notExpr
    : NOT notExpr            #negatedExpr
    | primary                #primaryExpr
    ;

primary
    : LPAREN orExpr RPAREN   #parenthesizedPrimary
    | predicate              #predicatePrimary
    ;

predicate
    : ATTRIBUTE_EXISTS LPAREN path RPAREN                         #attributeExistsPredicate
    | ATTRIBUTE_NOT_EXISTS LPAREN path RPAREN                     #attributeNotExistsPredicate
    | ATTRIBUTE_TYPE LPAREN path COMMA value RPAREN               #attributeTypePredicate
    | BEGINS_WITH LPAREN path COMMA value RPAREN                  #beginsWithPredicate
    | CONTAINS LPAREN path COMMA value RPAREN                     #containsPredicate
    | SIZE LPAREN path RPAREN comparator value                    #sizePredicate
    | path BETWEEN value AND value                                #betweenPredicate
    | path IN LPAREN value (COMMA value)* RPAREN                  #inPredicate
    | path comparator value                                       #comparisonPredicate
    ;

comparator
    : EQ
    | NE
    | LT
    | LTE
    | GT
    | GTE
    ;

path
    : IDENTIFIER
    ;

value
    : PLACEHOLDER      #placeholderValue
    | STRING_LITERAL   #stringValue
    | NUMBER_LITERAL   #numberValue
    | BOOLEAN_LITERAL  #booleanValue
    | NULL_LITERAL     #nullValue
    | IDENTIFIER       #identifierValue
    ;

LPAREN                      : '(';
RPAREN                      : ')';
COMMA                       : ',';
EQ                          : '=';
NE                          : '<>';
LTE                         : '<=';
GTE                         : '>=';
LT                          : '<';
GT                          : '>';

AND                         : A N D;
OR                          : O R;
NOT                         : N O T;
BETWEEN                     : B E T W E E N;
IN                          : I N;
ATTRIBUTE_EXISTS            : A T T R I B U T E '_' E X I S T S;
ATTRIBUTE_NOT_EXISTS        : A T T R I B U T E '_' N O T '_' E X I S T S;
ATTRIBUTE_TYPE              : A T T R I B U T E '_' T Y P E;
BEGINS_WITH                 : B E G I N S '_' W I T H;
CONTAINS                    : C O N T A I N S;
SIZE                        : S I Z E;

BOOLEAN_LITERAL             : T R U E | F A L S E;
NULL_LITERAL                : N U L L;

PLACEHOLDER                 : ':' IDENT_START IDENT_PART*;
NUMBER_LITERAL              : '-'? DIGIT+ ('.' DIGIT+)? EXPONENT?;
STRING_LITERAL              : '\'' ~['\r\n]* '\'';

IDENTIFIER                  : IDENT_START IDENT_PART* INDEX* ('.' IDENT_START IDENT_PART* INDEX*)*;

WS                          : [ \t\r\n]+ -> skip;

fragment INDEX              : '[' DIGIT+ ']';
fragment EXPONENT           : [eE] [+\-]? DIGIT+;
fragment IDENT_START        : [a-zA-Z_#];
fragment IDENT_PART         : [a-zA-Z0-9_];
fragment DIGIT              : [0-9];

fragment A                  : [aA];
fragment B                  : [bB];
fragment C                  : [cC];
fragment D                  : [dD];
fragment E                  : [eE];
fragment F                  : [fF];
fragment G                  : [gG];
fragment H                  : [hH];
fragment I                  : [iI];
fragment J                  : [jJ];
fragment K                  : [kK];
fragment L                  : [lL];
fragment M                  : [mM];
fragment N                  : [nN];
fragment O                  : [oO];
fragment P                  : [pP];
fragment Q                  : [qQ];
fragment R                  : [rR];
fragment S                  : [sS];
fragment T                  : [tT];
fragment U                  : [uU];
fragment V                  : [vV];
fragment W                  : [wW];
fragment X                  : [xX];
fragment Y                  : [yY];
fragment Z                  : [zZ];
