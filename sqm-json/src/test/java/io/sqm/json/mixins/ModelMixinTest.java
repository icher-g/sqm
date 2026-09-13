package io.sqm.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelMixinTest {
    @Test
    void familyMetadataHasUniqueStableDiscriminators() {
        var mixins = List.of(new CellAddressMixin() {}, new CellSelectorMixin() {},
            new CellForMixin() {});
        var expectedCounts = List.of(4, 2, 2);
        for (int i = 0; i < mixins.size(); i++) {
            var type = mixins.get(i).getClass().getSuperclass();
            var info = type.getAnnotation(JsonTypeInfo.class);
            assertEquals("kind", info.property());
            assertEquals(JsonTypeInfo.Id.NAME, info.use());
            var variants = type.getAnnotation(JsonSubTypes.class).value();
            assertEquals(expectedCounts.get(i), variants.length);
            assertEquals(variants.length, Arrays.stream(variants).map(JsonSubTypes.Type::name).distinct().count());
            assertTrue(Arrays.stream(variants).allMatch(v -> v.name().startsWith("cell-")));
        }
    }
}
