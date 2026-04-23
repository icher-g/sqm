package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class QuantifiedSourceMixinTest {

    @Test
    void constructorAndAnnotationsAreReachable() {
        var mixin = new QuantifiedSourceMixin() {
        };

        assertNotNull(mixin);
        assertNotNull(QuantifiedSourceMixin.class.getAnnotation(JsonTypeInfo.class));
        assertNotNull(QuantifiedSourceMixin.class.getAnnotation(JsonSubTypes.class));
    }
}
