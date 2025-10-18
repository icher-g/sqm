package io.cherlabs.sqm.parser.ansi;

import io.cherlabs.sqm.parser.*;
import io.cherlabs.sqm.parser.ansi.column.*;
import io.cherlabs.sqm.parser.ansi.filter.ColumnFilterParser;
import io.cherlabs.sqm.parser.ansi.filter.CompositeFilterParser;
import io.cherlabs.sqm.parser.ansi.filter.TupleFilterParser;
import io.cherlabs.sqm.parser.ansi.join.TableJoinParser;
import io.cherlabs.sqm.parser.ansi.query.CompositeQueryParser;
import io.cherlabs.sqm.parser.ansi.query.CteQueryParser;
import io.cherlabs.sqm.parser.ansi.query.SelectQueryParser;
import io.cherlabs.sqm.parser.ansi.query.WithQueryParser;
import io.cherlabs.sqm.parser.ansi.statement.*;
import io.cherlabs.sqm.parser.ansi.table.NamedTableParser;
import io.cherlabs.sqm.parser.ansi.table.QueryTableParser;
import io.cherlabs.sqm.parser.ansi.value.*;
import io.cherlabs.sqm.parser.spi.ParsersRepository;

public final class Parsers {

    private static final ParsersRepository defaultRepository = registerDefaults(new DefaultParsersRepository());

    private Parsers() {
    }

    public static ParsersRepository ansi() {
        return defaultRepository;
    }

    private static ParsersRepository registerDefaults(ParsersRepository r) {
        return r
            .register(new ColumnParser())
            .register(new FilterParser())
            .register(new JoinParser())
            .register(new TableParser())
            .register(new OrderParser())
            .register(new GroupParser())
            .register(new QueryParser())
            .register(new ValueColumnParser())
            .register(new NamedColumnParser())
            .register(new FunctionColumnParser())
            .register(new CaseColumnParser())
            .register(new StarColumnParser())
            .register(new NamedTableParser())
            .register(new GroupByParser())
            .register(new OrderByParser())
            .register(new SelectQueryParser())
            .register(new LimitOffsetParser())
            .register(new CteQueryParser())
            .register(new WithQueryParser())
            .register(new CompositeQueryParser())
            .register(new QueryColumnParser())
            .register(new QueryTableParser())
            .register(new CompositeFilterParser())
            .register(new ColumnFilterParser())
            .register(new TupleFilterParser())
            .register(new ValuesParser())
            .register(new ListValuesParser())
            .register(new SingleValueParser())
            .register(new TupleValuesParser())
            .register(new RangeValuesParser())
            .register(new ColumnValueParser())
            .register(new SubqueryValueParser())
            .register(new TableJoinParser());
    }
}
