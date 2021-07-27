package org.parchmentmc.nitwit.util.graphql;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Custom type adapter for {@link OffsetDateTime}.
 */
public class DateTimeAdapter implements CustomTypeAdapter<OffsetDateTime> {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Nullable
    private final ZoneOffset forcedOffset;

    public DateTimeAdapter(@Nullable ZoneOffset forcedOffset) {
        this.forcedOffset = forcedOffset;
    }

    @Override
    public OffsetDateTime decode(@NotNull final CustomTypeValue<?> customTypeValue) {
        OffsetDateTime dateTime = OffsetDateTime.parse(customTypeValue.value.toString(), formatter);
        if (forcedOffset != null) {
            dateTime = dateTime.withOffsetSameInstant(forcedOffset);
        }
        return dateTime;
    }

    @NotNull
    @Override
    public CustomTypeValue<?> encode(final OffsetDateTime offsetDateTime) {
        OffsetDateTime encodeDate = offsetDateTime;
        if (forcedOffset != null) {
            encodeDate = encodeDate.withOffsetSameInstant(forcedOffset);
        }
        return new CustomTypeValue.GraphQLString(formatter.format(encodeDate));
    }
}
