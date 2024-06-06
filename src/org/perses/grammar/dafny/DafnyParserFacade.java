package org.perses.grammar.dafny;

import com.google.common.primitives.ImmutableIntArray;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.perses.grammar.AbstractDefaultParserFacade;

public final class DafnyParserFacade
        extends AbstractDefaultParserFacade<PnfDafnyLexer, PnfDafnyParser> {

    public DafnyParserFacade() {
        super(
                LanguageDafny.INSTANCE,
                createCombinedAntlrGrammar("PnfDafny.g4", DafnyParserFacade.class),
                PnfDafnyLexer.class,
                PnfDafnyParser.class,
                ImmutableIntArray.of(PnfDafnyLexer.IDENTIFIER));
    }

    @Override
    protected PnfDafnyLexer createLexer(CharStream inputStream) {
        return new PnfDafnyLexer(inputStream);
    }

    @Override
    protected PnfDafnyParser createParser(CommonTokenStream tokens) {
        return new PnfDafnyParser(tokens);
    }

    @Override
    protected ParseTree startParsing(PnfDafnyParser parser) {
        return parser.translation_unit();
    }
}