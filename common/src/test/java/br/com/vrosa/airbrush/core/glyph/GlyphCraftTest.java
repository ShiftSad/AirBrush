package br.com.vrosa.airbrush.core.glyph;

import br.com.vrosa.airbrush.core.config.AirBrushConfig;
import br.com.vrosa.airbrush.core.glyph.craft.GlyphCraftService;
import br.com.vrosa.airbrush.core.glyph.craft.GlyphRecipes;
import br.com.vrosa.airbrush.core.glyph.model.BaseShape;
import br.com.vrosa.airbrush.core.glyph.model.Modifier;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlyphCraftTest {

    private static final StrokeFactory.Basis FLOOR = StrokeFactory.floor();

    @Test
    void recipeMatchingRequiresExactGlyphAndIngredients() {
        final var available = Map.of("airbrush:quill", 1, "minecraft:gold_block", 1);

        assertTrue(GlyphRecipes.match(BaseShape.TRIANGLE, Set.of(Modifier.FOCUS_DOT), available).isPresent());
        assertTrue(GlyphRecipes.match(BaseShape.SQUARE, Set.of(Modifier.FOCUS_DOT), available).isEmpty());
        assertTrue(GlyphRecipes.match(BaseShape.TRIANGLE, Set.of(), available).isEmpty());
        assertTrue(GlyphRecipes.match(BaseShape.TRIANGLE, Set.of(Modifier.FOCUS_DOT, Modifier.RAYS), available).isEmpty());
        assertTrue(GlyphRecipes.match(BaseShape.TRIANGLE, Set.of(Modifier.FOCUS_DOT),
                Map.of("airbrush:quill", 1)).isEmpty());
    }

    @Test
    void hammerActivationCraftsConsumesAndClearsGlyph() {
        final var platform = new FakePlatform();
        final var store = new GlyphStrokeStore();
        final var service = new GlyphCraftService(platform, store, new AirBrushConfig());

        final var base = StrokeFactory.stroke(StrokeFactory.polygon(FLOOR,
                StrokeFactory.triangleVertices(2.2, 0), 120, 0, 0, 1));
        final var dot = StrokeFactory.stroke(StrokeFactory.circle(FLOOR, 0.15, 24, 0, 0, 0, 2));
        store.add(base);
        store.add(dot);

        final var quill = new FakePlatform.FakeDrop("airbrush:quill", 1, FLOOR.pose(0.2, 0.1).position());
        final var gold = new FakePlatform.FakeDrop("minecraft:gold_block", 2, FLOOR.pose(-0.4, 0.3).position());
        final var outside = new FakePlatform.FakeDrop("minecraft:gold_block", 4, FLOOR.pose(3.5, 0).position());
        platform.drops.add(quill);
        platform.drops.add(gold);
        platform.drops.add(outside);

        final var pointer = StrokeFactory.floor().pose(0, 0);
        assertTrue(service.attempt(pointer));

        assertEquals(1, platform.results.size());
        assertEquals(GlyphRecipes.QUILL_GOLD_ID, platform.results.getFirst().recipeId());
        assertTrue(platform.results.getFirst().quality() > 0.9);

        assertEquals(0, quill.amount());
        assertEquals(1, gold.amount());
        assertEquals(4, outside.amount());

        assertFalse(base.segments().getFirst().isValid());
        assertFalse(dot.segments().getFirst().isValid());

        assertFalse(service.attempt(pointer));
    }

    @Test
    void activationWithoutIngredientsDoesNothing() {
        final var platform = new FakePlatform();
        final var store = new GlyphStrokeStore();
        final var service = new GlyphCraftService(platform, store, new AirBrushConfig());

        final var base = StrokeFactory.stroke(StrokeFactory.circle(FLOOR, 2.0, 120, 0, 0, 0, 1));
        store.add(base);
        store.add(StrokeFactory.stroke(StrokeFactory.circle(FLOOR, 0.15, 24, 0, 0, 0, 2)));

        assertFalse(service.attempt(StrokeFactory.floor().pose(0, 0)));
        assertTrue(platform.results.isEmpty());
        assertTrue(base.segments().getFirst().isValid());
    }

    @Test
    void bareGlyphsCraftQuillAndCloth() {
        assertTrue(GlyphRecipes.match(BaseShape.TRIANGLE, Set.of(),
                Map.of("minecraft:feather", 1, "minecraft:charcoal", 1)).isPresent());
        assertTrue(GlyphRecipes.match(BaseShape.SQUARE, Set.of(),
                Map.of("minecraft:white_wool", 1)).isPresent());

        final var platform = new FakePlatform();
        final var store = new GlyphStrokeStore();
        final var service = new GlyphCraftService(platform, store, new AirBrushConfig());

        store.add(StrokeFactory.stroke(StrokeFactory.polygon(FLOOR,
                StrokeFactory.triangleVertices(2.2, 0), 120, 0, 0, 1)));
        platform.drops.add(new FakePlatform.FakeDrop("minecraft:feather", 1, FLOOR.pose(0.3, 0).position()));
        platform.drops.add(new FakePlatform.FakeDrop("minecraft:charcoal", 1, FLOOR.pose(-0.2, 0.2).position()));

        assertTrue(service.attempt(StrokeFactory.floor().pose(0, 0)));
        assertEquals(GlyphRecipes.QUILL_ID, platform.results.getFirst().recipeId());
    }

    @Test
    void quillUpgradeAcceptsInkedQuillAndCarriesInk() {
        final var platform = new FakePlatform();
        final var store = new GlyphStrokeStore();
        final var service = new GlyphCraftService(platform, store, new AirBrushConfig());

        store.add(StrokeFactory.stroke(StrokeFactory.polygon(FLOOR,
                StrokeFactory.triangleVertices(2.2, 0), 120, 0, 0, 1)));
        store.add(StrokeFactory.stroke(StrokeFactory.circle(FLOOR, 0.15, 24, 0, 0, 0, 2)));

        final var quill = new FakePlatform.FakeDrop("airbrush:quill", 1, FLOOR.pose(0.3, 0).position());
        quill.inkId = "luminous";
        quill.inkColor = 0x4DE3D1;
        quill.durability = 42;
        platform.drops.add(quill);
        platform.drops.add(new FakePlatform.FakeDrop("minecraft:gold_block", 1, FLOOR.pose(-0.2, 0.2).position()));

        assertTrue(service.attempt(StrokeFactory.floor().pose(0, 0)));
        assertEquals(GlyphRecipes.QUILL_GOLD_ID, platform.results.getFirst().recipeId());

        final var carried = platform.results.getFirst().ink();
        assertEquals("luminous", carried.inkId());
        assertEquals(0x4DE3D1, carried.rgb());
        assertEquals(42, carried.charge());
        assertEquals(0, quill.amount());
    }

    @Test
    void clothRecipeCrafts() {
        final var platform = new FakePlatform();
        final var store = new GlyphStrokeStore();
        final var service = new GlyphCraftService(platform, store, new AirBrushConfig());

        store.add(StrokeFactory.stroke(StrokeFactory.polygon(FLOOR,
                StrokeFactory.squareVertices(1.7, 0), 120, 0, 0, 1)));
        platform.drops.add(new FakePlatform.FakeDrop("minecraft:white_wool", 1, FLOOR.pose(0.3, 0).position()));

        assertTrue(service.attempt(StrokeFactory.floor().pose(0, 0)));
        assertEquals(GlyphRecipes.CLOTH_ID, platform.results.getFirst().recipeId());
    }
}
