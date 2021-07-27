package org.parchmentmc.nitwit.discord.pagination;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class PaginationHelper {
    private PaginationHelper() { // Prevent instantiation
    }

    public static final char SEPARATOR_CHAR = ';';
    public static final String SEPARATOR = String.valueOf(SEPARATOR_CHAR);
    public static final String PAGINATION_PREFIX = "pager";
    public static final String DELETION_ID = "del";

    public static Message createPaginationMessage(String userID, Consumer<EmbedBuilder> customizer, String prevID,
                                                  String nextID, Iterable<String> entries, String paginationID,
                                                  @Nullable PageDirection direction, PaginationInfo pageInfo) {
        char sep = SEPARATOR_CHAR;
        String prefix = PAGINATION_PREFIX + sep + userID + sep;

        Button deleteButton = Button.secondary(prefix + DELETION_ID, Emoji.fromUnicode("U+274C")); // :x:

        Button prevButton = Button.secondary(prefix + paginationID + sep + prevID + sep + pageInfo.startCursor(),
                Emoji.fromUnicode("U+2B05")); // :left_arrow:
        Button nextButton = Button.secondary(prefix + paginationID + sep + nextID + sep + pageInfo.endCursor(),
                Emoji.fromUnicode("U+27A1")); // :right_arrow:

        if (!pageInfo.hasPreviousPage() && direction != PageDirection.FORWARDS) {
            prevButton = prevButton.asDisabled();
        }
        if (!pageInfo.hasNextPage() && direction != PageDirection.BACKWARDS) {
            nextButton = nextButton.asDisabled();
        }

        final EmbedBuilder embed = new EmbedBuilder();
        customizer.accept(embed);

        embed.setFooter("Total items: " + pageInfo.totalItems());
        embed.appendDescription(StreamSupport.stream(entries.spliterator(), false)
                .collect(Collectors.joining("\n")));

        return new MessageBuilder()
                .setActionRows(ActionRow.of(prevButton, deleteButton, nextButton))
                .setEmbeds(embed.build())
                .build();
    }

    public static String createPaginationButtonID(String userID, String paginationID, String... tokens) {
        tokens = tokens != null ? tokens : new String[0];
        return String.join(SEPARATOR, PAGINATION_PREFIX, userID, paginationID, String.join(SEPARATOR, tokens));
    }

    @Nullable
    public static PaginationButtonInfo getInfo(String paginationButtonID) {
        final String[] rawTokens = paginationButtonID.split(SEPARATOR);
        if (rawTokens.length < 3 || !rawTokens[0].equals(PAGINATION_PREFIX)) return null;
        final String[] extraTokens = Arrays.copyOfRange(rawTokens, 3, rawTokens.length);
        return new PaginationButtonInfo(rawTokens[1], rawTokens[2], extraTokens);
    }

    public enum PageDirection {
        FORWARDS,
        BACKWARDS
    }

    public static record PaginationButtonInfo(String userID, String paginationID, String[] tokens) {
        @Override
        public String toString() {
            return "PaginationButtonInfo{" +
                    "userID='" + userID + '\'' +
                    ", paginationID='" + paginationID + '\'' +
                    ", tokens=" + Arrays.toString(tokens) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PaginationButtonInfo that = (PaginationButtonInfo) o;
            return Objects.equals(userID, that.userID) && Objects.equals(paginationID, that.paginationID) && Arrays.equals(tokens, that.tokens);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(userID, paginationID);
            result = 31 * result + Arrays.hashCode(tokens);
            return result;
        }
    }
}
