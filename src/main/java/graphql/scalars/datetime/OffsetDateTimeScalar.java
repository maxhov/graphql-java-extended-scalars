package graphql.scalars.datetime;


import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

import static graphql.scalars.util.Kit.typeName;
import static graphql.schema.GraphQLScalarType.newScalar;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

/**
 * A Scalar representing an Date and time and offset following the specification at https://www.graphql-scalars.com/date-time.
 */
public class OffsetDateTimeScalar {

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendFraction(MILLI_OF_SECOND, 3, 3, true)
            .appendPattern("XXX") // Offset in +HH:MM or 'Z'
            .parseStrict()
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    // package private for testing
    static Coercing<OffsetDateTime, String> coercing = new Coercing<OffsetDateTime, String>() {

        @Override
        public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
            if (dataFetcherResult instanceof OffsetDateTime) {
                return formatter.format((TemporalAccessor) dataFetcherResult);
            }
            throw new CoercingSerializeException("Not accepted value " + dataFetcherResult);
        }

        @Override
        public OffsetDateTime parseValue(Object input) throws CoercingParseValueException {
            return null;
        }

        @Override
        public OffsetDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
            if (!(input instanceof StringValue)) {
                throw new CoercingParseLiteralException(
                        "Expected AST type 'StringValue' but was '" + typeName(input) + "'."
                );
            }
            return parseOffsetDateTime(((StringValue) input).getValue(), CoercingParseLiteralException::new);
        }

        private OffsetDateTime parseOffsetDateTime(String s, Function<String, RuntimeException> exceptionMaker) {
            try {
                OffsetDateTime parse = OffsetDateTime.parse(s, formatter);
                if (parse.get(OFFSET_SECONDS) == 0 && s.endsWith("-00:00")) {
                    throw exceptionMaker.apply("Invalid value : '" + s + "'. because negative zero offset is not allowed");
                }
                return parse;
            } catch (DateTimeParseException e) {
                throw exceptionMaker.apply("Invalid value : '" + s + "'. because of : '" + e.getMessage() + "'");
            }
        }
    };

    public static GraphQLScalarType newOffsetDateTimeScalar(String name) {
        return newScalar()
                .name(name)
                .specifiedByUrl("https://www.graphql-scalars.com/date-time")
                .coercing(coercing)
                .build();
    }
}