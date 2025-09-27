package io.cherlabs.sqlmodel.core;

public record WhenThen(Filter when, Entity then) implements Entity {

    public static WhenThen when(Filter condition) {
        return new WhenThen(condition, null);
    }

    public WhenThen then(Entity value) {
        return new WhenThen(when, value);
    }
}
