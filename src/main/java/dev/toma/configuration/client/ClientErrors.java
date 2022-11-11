package dev.toma.configuration.client;

import dev.toma.configuration.config.value.DecimalValue;
import dev.toma.configuration.config.value.IntegerValue;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.regex.Pattern;

public final class ClientErrors {

    public static final MutableComponent CHAR_VALUE_EMPTY = new TranslatableComponent("text.configuration.error.character_value_empty");

    private static final String KEY_NAN = "text.configuration.error.nan";
    private static final String KEY_NUM_BOUNDS = "text.configuration.error.num_bounds";
    private static final String KEY_MISMATCHED_PATTERN = "text.configuration.error.pattern_mismatch";

    public static MutableComponent notANumber(String value) {
        return new TranslatableComponent(KEY_NAN, value);
    }

    public static MutableComponent outOfBounds(int i, IntegerValue.Range range) {
        return new TranslatableComponent(KEY_NUM_BOUNDS, i, range.min(), range.max());
    }

    public static MutableComponent outOfBounds(long i, IntegerValue.Range range) {
        return new TranslatableComponent(KEY_NUM_BOUNDS, i, range.min(), range.max());
    }

    public static MutableComponent outOfBounds(float i, DecimalValue.Range range) {
        return new TranslatableComponent(KEY_NUM_BOUNDS, i, range.min(), range.max());
    }

    public static MutableComponent outOfBounds(double i, DecimalValue.Range range) {
        return new TranslatableComponent(KEY_NUM_BOUNDS, i, range.min(), range.max());
    }

    public static MutableComponent invalidText(String text, Pattern pattern) {
        return new TranslatableComponent(KEY_MISMATCHED_PATTERN, text, pattern);
    }
}
