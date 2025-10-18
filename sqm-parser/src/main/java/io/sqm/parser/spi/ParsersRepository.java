package io.sqm.parser.spi;

import io.sqm.core.Entity;
import io.sqm.core.repos.HandlersRepository;

/**
 * A base interface for the spec parsers' repository.
 */
public interface ParsersRepository extends HandlersRepository<Parser<?>> {
    /**
     * Returns a SpecParser for the specific class.
     * @param type the type of the class which parser is needed.
     * @return a SpecParser for the required type.
     * @param <T> the actual type of the entity to be parsed.
     */
    @Override
    <T extends Entity> Parser<T> get(Class<T> type);

    /**
     * Returns a SpecParser for the provided entity.
     * @param instance an instance of the entity that needs to be parsed.
     * @return a SpecParser.
     * @param <T> the actual type of the entity to be parsed.
     */
    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> Parser<T> getFor(T instance) {
        return (Parser<T>) HandlersRepository.super.getFor(instance);
    }

    /**
     * Returns a SpecParser if found or throws an exception otherwise.
     * @param type the type of the class which parser is needed.
     * @return a SpecParser
     * @param <T> the actual type of the entity to be parsed.
     * @exception IllegalArgumentException if no parser found.
     */
    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> Parser<T> require(Class<T> type) {
        return (Parser<T>) HandlersRepository.super.require(type);
    }

    /**
     * Returns a SpecParser if found or throws an exception otherwise.
     * @param instance an instance of the entity that needs to be parsed.
     * @return a SpecParser
     * @param <T> the actual type of the entity to be parsed.
     * @exception IllegalArgumentException if no spec parser found.
     */
    @Override
    @SuppressWarnings("unchecked")
    default <T extends Entity> Parser<T> requireFor(T instance) {
        return (Parser<T>) HandlersRepository.super.requireFor(instance);
    }

    /**
     * Registers a new spec parser for a type.
     * @param handler the spec parser implementation
     * @return this.
     */
    @Override
    default ParsersRepository register(Parser<?> handler) {
        return register(handler.targetType(), handler);
    }

    /**
     * Registers a new spec parser for a type.
     * @param type the type of the entity to be parsed.
     * @param handler the spec parser implementation
     * @return this.
     * @param <T> the type of the entity.
     */
    <T extends Entity> ParsersRepository register(Class<T> type, Parser<?> handler);
}
