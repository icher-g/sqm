package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MatchPatternMixinTest {

    @Test
    void constructorAndAnnotationsAreReachable() {
        var mixin = new MatchPatternMixin() {
        };

        assertNotNull(mixin);
        assertNotNull(MatchPatternMixin.class.getAnnotation(JsonTypeInfo.class));
        assertNotNull(MatchPatternMixin.class.getAnnotation(JsonSubTypes.class));
    }
}
