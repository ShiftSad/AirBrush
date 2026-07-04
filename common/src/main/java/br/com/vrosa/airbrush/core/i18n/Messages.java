package br.com.vrosa.airbrush.core.i18n;

import br.com.vrosa.airbrush.core.glyph.model.Reason;
import br.com.vrosa.airbrush.core.ink.InkType;
import br.com.vrosa.airbrush.core.ink.SolventType;
import br.com.vrosa.airbrush.platform.ToolType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class Messages {

    public enum Key {
        PENCIL("pencil"),
        ERASER("eraser"),
        PALETTE("palette"),
        MARKER("amethyst_dye"),
        MARKER_DRY("marker.dry"),
        QUILL("quill"),
        QUILL_DRY("quill.dry"),
        CLOTH("cloth"),
        CLOTH_DRY("cloth.dry"),
        CLOTH_DRY_STATE("cloth.dry-state"),
        CLOTH_WET("cloth.wet-state"),
        CAULDRON_RECHARGED("cauldron.recharged"),
        CAULDRON_SOAKED("cauldron.soaked"),
        CAULDRON_WRONG_LIQUID("cauldron.wrong-liquid"),
        CAULDRON_REPLACE("cauldron.replace"),
        INK_COMMON("ink.common"),
        INK_LUMINOUS("ink.luminous"),
        INK_AMETHYST("ink.amethyst"),
        INK_INDELIBLE("ink.indelible"),
        INK_INDELIBLE_LUMINOUS("ink.indelible_luminous"),
        SOLVENT_COMMON("solvent.common"),
        GLYPH_CRAFT_SUCCESS("glyph.craft.success"),
        GLYPH_TEST_RESULT("glyph.test.result"),
        GLYPH_TEST_NONE("glyph.test.none"),
        GLYPH_TEST_REASON_NOT_CLOSED("glyph.test.reason.not_closed"),
        GLYPH_TEST_REASON_NOT_PLANAR("glyph.test.reason.not_planar"),
        GLYPH_TEST_REASON_UNRECOGNIZED_SHAPE("glyph.test.reason.unrecognized_shape"),
        GLYPH_TEST_REASON_TOO_SMALL("glyph.test.reason.too_small"),
        GLYPH_TEST_REASON_TOO_LARGE("glyph.test.reason.too_large"),
        GLYPH_TEST_REASON_MIXED_WORLDS("glyph.test.reason.mixed_worlds"),
        COLOR("color"),
        THICKNESS("thickness"),
        ERASER_AREA("eraser.area"),
        ERASER_STROKE("eraser.stroke"),
        RADIUS("radius"),
        ERASING("erasing"),
        IDLE("idle"),
        COLOR_CHOSEN("color.chosen"),
        PALETTE_CLOSED("palette.closed"),
        PALETTE_HINT("palette.hint"),
        PLAYERS_ONLY("players.only"),
        COLOR_INVALID("color.invalid"),
        COLOR_CHANGED("color.changed"),
        DRAWITEM_INVALID("drawitem.invalid"),
        DRAWITEM_GIVEN("drawitem.given"),
        UNDO_NOTHING("undo.nothing"),
        UNDO_DONE("undo.done"),
        CONFIG_RELOADED("config.reloaded"),
        NO_PERMISSION("no.permission");

        private final String path;

        Key(String path) {
            this.path = path;
        }
    }

    private static final String[] LOCALES = {"en-us", "pt-br"};

    private static Map<String, String> en = bundle(null, "en-us");
    private static Map<String, String> pt = bundle(null, "pt-br");

    private Messages() {}

    public static void load(@NotNull Path directory) {
        try {
            Files.createDirectories(directory);
            for (final var locale : LOCALES) {
                final var file = directory.resolve(locale + ".properties");
                if (!Files.exists(file)) copyDefault(locale, file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        en = bundle(directory, "en-us");
        pt = bundle(directory, "pt-br");
    }

    public static @NotNull String get(@NotNull Locale locale, @NotNull Key key) {
        final var bundle = isPortuguese(locale) ? pt : en;
        return bundle.getOrDefault(key.path, en.getOrDefault(key.path, key.path));
    }

    public static @NotNull String format(@NotNull Locale locale, @NotNull Key key, Object... args) {
        return String.format(Locale.ROOT, get(locale, key), args);
    }

    public static @NotNull String toolName(@NotNull Locale locale, @NotNull ToolType tool) {
        return switch (tool) {
            case PENCIL -> get(locale, Key.PENCIL);
            case ERASER -> get(locale, Key.ERASER);
            case PALETTE -> get(locale, Key.PALETTE);
            case MARKER -> get(locale, Key.MARKER);
            case QUILL -> get(locale, Key.QUILL);
            case CLOTH -> get(locale, Key.CLOTH);
        };
    }

    public static @NotNull String solventName(@NotNull Locale locale, @NotNull SolventType solvent) {
        return switch (solvent) {
            case COMMON -> get(locale, Key.SOLVENT_COMMON);
        };
    }

    public static @NotNull String inkName(@NotNull Locale locale, @NotNull InkType ink) {
        return switch (ink) {
            case COMMON -> get(locale, Key.INK_COMMON);
            case LUMINOUS -> get(locale, Key.INK_LUMINOUS);
            case AMETHYST -> get(locale, Key.INK_AMETHYST);
            case INDELIBLE -> get(locale, Key.INK_INDELIBLE);
            case INDELIBLE_LUMINOUS -> get(locale, Key.INK_INDELIBLE_LUMINOUS);
        };
    }

    public static @NotNull String glyphReason(@NotNull Locale locale, @NotNull Reason reason) {
        return switch (reason) {
            case OK -> "";
            case NO_STROKES -> get(locale, Key.GLYPH_TEST_NONE);
            case NOT_CLOSED -> get(locale, Key.GLYPH_TEST_REASON_NOT_CLOSED);
            case NOT_PLANAR -> get(locale, Key.GLYPH_TEST_REASON_NOT_PLANAR);
            case UNRECOGNIZED_SHAPE -> get(locale, Key.GLYPH_TEST_REASON_UNRECOGNIZED_SHAPE);
            case TOO_SMALL -> get(locale, Key.GLYPH_TEST_REASON_TOO_SMALL);
            case TOO_LARGE -> get(locale, Key.GLYPH_TEST_REASON_TOO_LARGE);
            case MIXED_WORLDS -> get(locale, Key.GLYPH_TEST_REASON_MIXED_WORLDS);
        };
    }

    private static boolean isPortuguese(@NotNull Locale locale) {
        return "pt".equalsIgnoreCase(locale.getLanguage());
    }

    private static @NotNull Map<String, String> bundle(@Nullable Path directory, @NotNull String name) {
        final var map = new HashMap<String, String>();
        try (var in = defaultStream(name)) {
            if (in != null) read(new InputStreamReader(in, StandardCharsets.UTF_8), map);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (directory != null) {
            final var file = directory.resolve(name + ".properties");
            if (Files.exists(file)) {
                try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    read(reader, map);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return map;
    }

    private static void read(@NotNull Reader in, @NotNull Map<String, String> into) throws IOException {
        final var properties = new Properties();
        properties.load(in);
        properties.forEach((key, value) -> into.put(key.toString(), value.toString()));
    }

    private static void copyDefault(@NotNull String name, @NotNull Path target) throws IOException {
        try (var in = defaultStream(name)) {
            if (in != null) Files.copy(in, target);
        }
    }

    private static @Nullable InputStream defaultStream(@NotNull String name) {
        return Messages.class.getResourceAsStream("/lang/" + name + ".properties");
    }
}
