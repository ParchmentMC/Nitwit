package org.parchmentmc.nitwit.config.serializers;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationSerializer implements TypeSerializer<Duration> {
    private final boolean exact;

    public DurationSerializer(boolean exact) {
        this.exact = exact;
    }

    @Override
    public Duration deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final String str = node.getString();
        if (str == null) {
            return null;
        }

        if (exact) {
            return Duration.parse(str);
        } else {
            return deserialize(str);
        }
    }

    @Override
    public void serialize(Type type, @Nullable Duration obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }

        if (exact) {
            node.set(obj.toString());
        } else {
            node.set(serialize(obj));
        }
    }

    private static final Pattern DURATION =
            Pattern.compile("(-?)" + "(?:(?<days>\\d+)[dD])?" + "(?:(?<hours>\\d+)[hH])?" + "(?:(?<minutes>\\d+)[mM])?"
                    + "(?:(?<seconds>\\d+)[sS])?");

    @Nullable
    private static Duration deserialize(@Nullable String string) {
        if (string == null || string.isBlank()) {
            return null;
        }

        if (string.equals("0")) {
            return Duration.ZERO;
        }

        Duration duration = Duration.ZERO;
        final Matcher matcher = DURATION.matcher(string);
        if (!matcher.matches()) {
            return null;
        }

        final @Nullable String days = matcher.group("days");
        if (days != null) {
            duration = duration.plusDays(Long.parseLong(days));
        }

        final @Nullable String hours = matcher.group("hours");
        if (hours != null) {
            duration = duration.plusHours(Long.parseLong(hours));
        }

        final @Nullable String minutes = matcher.group("minutes");
        if (minutes != null) {
            duration = duration.plusMinutes(Long.parseLong(minutes));
        }

        final @Nullable String seconds = matcher.group("seconds");
        if (seconds != null) {
            duration = duration.plusSeconds(Long.parseLong(seconds));
        }

        return duration;
    }

    private static String serialize(Duration duration) {
        duration = duration.withNanos(0).minusMillis(duration.toMillisPart());
        if (duration.isZero()) {
            return "0";
        }
        final StringBuilder builder = new StringBuilder();
        if (duration.isNegative()) {
            builder.append('-');
        }
        final long days = duration.toDaysPart();
        if (days > 0) {
            builder.append(days).append('d');
        }
        final int hours = duration.toHoursPart();
        if (hours > 0) {
            builder.append(hours).append('h');
        }
        final int minutes = duration.toMinutesPart();
        if (minutes > 0) {
            builder.append(minutes).append('h');
        }
        final int seconds = duration.toSecondsPart();
        if (seconds > 0) {
            builder.append(seconds).append('h');
        }
        return builder.toString();
    }
}
