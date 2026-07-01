package org.evomaster.client.java.controller.neo4j.parser.cypher25;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.evomaster.client.java.controller.neo4j.cypher25.Cypher25Lexer;
import org.evomaster.client.java.controller.neo4j.cypher25.Cypher25Parser;
import org.evomaster.client.java.controller.neo4j.parser.CypherParser;
import org.evomaster.client.java.controller.neo4j.parser.CypherParserException;
import org.evomaster.client.java.controller.neo4j.operations.MatchOperation;

/**
 * {@link CypherParser} implementation built on the official Neo4j {@code Cypher25}
 * ANTLR grammar. Because the grammar is the one Neo4j itself uses, any query this
 * parser accepts is guaranteed to be valid Cypher.
 * <br>
 * Syntax errors are not swallowed: the lexer and parser are wired with a listener
 * that fails fast, and the error (with line/column) is surfaced as a
 * {@link CypherParserException}.
 */
public class Cypher25AntlrParser implements CypherParser {

    private static final ThrowingErrorListener THROWING_LISTENER = new ThrowingErrorListener();

    @Override
    public MatchOperation parse(String query) throws CypherParserException {
        if (query == null || query.trim().isEmpty()) {
            throw new CypherParserException("Query is null or empty");
        }

        try {
            Cypher25Lexer lexer = new Cypher25Lexer(CharStreams.fromString(query));
            lexer.removeErrorListeners();
            lexer.addErrorListener(THROWING_LISTENER);

            Cypher25Parser parser = new Cypher25Parser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(THROWING_LISTENER);

            Cypher25Parser.StatementsContext tree = parser.statements();

            Cypher25MatchVisitor visitor = new Cypher25MatchVisitor();
            visitor.visit(tree);

            if (!visitor.foundMatch()) {
                throw new CypherParserException("Query does not contain a MATCH clause");
            }
            return visitor.toOperation();

        } catch (ParseCancellationException e) {
            throw new CypherParserException(e.getMessage(), e);
        }
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }
}
