package io.sqm.parser.oracle;

import io.sqm.parser.ansi.NotPredicateParser;
import io.sqm.parser.ansi.PowerArithmeticExprParser;
import io.sqm.parser.spi.ParsersRepository;

import static io.sqm.parser.ansi.Parsers.ansiCopy;

/**
 * Entry point for Oracle parser registrations.
 */
public final class Parsers {

    private static final ParsersRepository defaultRepository = registerOracleOverrides(ansiCopy());

    private Parsers() {
    }

    /**
     * Returns the shared Oracle parsers repository.
     *
     * @return Oracle parsers repository.
     */
    public static ParsersRepository oracle() {
        return defaultRepository;
    }

    /**
     * Returns a new Oracle parsers repository instance with default registrations.
     *
     * @return isolated Oracle parsers repository.
     */
    public static ParsersRepository oracleCopy() {
        return registerOracleOverrides(ansiCopy());
    }

    private static ParsersRepository registerOracleOverrides(ParsersRepository repository) {
        var postfix = new PostfixExprParser(new AtomicExprParser());
        return repository
            .register(new PowerArithmeticExprParser(postfix))
            .register(new NotPredicateParser(new AtomicPredicateParser()))
            .register(new ReferenceModelParser())
            .register(new CellSelectorConditionParser())
            .register(new CellForParser())
            .register(new CellForValuesParser())
            .register(new CellForRangeParser())
            .register(new ModelAggregateExprParser())
            .register(new IterationSpecParser())
            .register(new CurrentDimensionExprParser())
            .register(new IterationNumberExprParser())
            .register(new PreviousModelValueExprParser())
            .register(new PresenceValueExprParser())
            .register(new CellPresentPredicateParser())
            .register(new IsAnyPredicateParser())
            .register(new ModelClauseParser())
            .register(new MainModelParser())
            .register(new ModelColumnParser())
            .register(new ModelRulesParser())
            .register(new ModelRuleParser())
            .register(new CellTargetParser())
            .register(new CellAddressParser())
            .register(new CellSelectorParser())
            .register(new CellSelectorValueParser())
            .register(new CellRefExprParser())
            .register(new InsertStatementParser())
            .register(new UpdateStatementParser())
            .register(new DeleteStatementParser())
            .register(new MergeStatementParser())
            .register(new MergeClauseParser())
            .register(new MergeUpdateActionParser())
            .register(new MergeInsertActionParser())
            .register(new HierarchicalQueryClauseParser())
            .register(new SequenceValueExprParser())
            .register(new SampledTableParser())
            .register(new PriorExprParser())
            .register(new PatternRecognitionTableParser())
            .register(new LimitOffsetParser());
    }
}
