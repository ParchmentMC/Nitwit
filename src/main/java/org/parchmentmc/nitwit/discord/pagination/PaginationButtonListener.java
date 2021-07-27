package org.parchmentmc.nitwit.discord.pagination;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.parchmentmc.nitwit.discord.pagination.PaginationHelper.*;

public class PaginationButtonListener extends ListenerAdapter {
    private final Duration buttonLifetime;
    private final Map<String, CustomButtonListener> customListeners = new HashMap<>();

    public PaginationButtonListener(Duration buttonLifetime) {
        this.buttonLifetime = buttonLifetime;
    }

    public PaginationButtonListener add(CustomButtonListener listener, String... names) {
        for (String name : names) {
            customListeners.put(name, listener);
        }
        return this;
    }

    public PaginationButtonListener add(String name, CustomButtonListener listener) {
        customListeners.put(name, listener);
        return this;
    }

    public PaginationButtonListener remove(String name) {
        customListeners.remove(name);
        return this;
    }

    public Duration getButtonLifetime() {
        return buttonLifetime;
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        final Button button = event.getButton();
        if (event.getMessage() == null || button == null) return;

        final String customID = button.getId();
        if (customID == null) return;

        final PaginationButtonInfo info = getInfo(customID);
        if (info == null) return;

        OffsetDateTime beginTime = event.getMessage().getTimeCreated();
        if (event.getMessage().getTimeEdited() != null) {
            beginTime = event.getMessage().getTimeEdited();
        }
        final Duration gap = Duration.between(beginTime, OffsetDateTime.now());
        // We ignore the deletion button
        if (gap.compareTo(buttonLifetime) > 0 && !info.paginationID().equals(DELETION_ID)) {
            // It's been there for more than the time limit, lock it down lads
            disableComponents(event.getMessage())
                    .queue();
            return;
        }

        if (event.getUser().getId().equals(info.userID())) {
            if (info.paginationID().equals(DELETION_ID)) {
                deleteMessageAndReferrer(event.getUser(), event.getMessage())
                        .queue();
            } else {
                final CustomButtonListener listener = customListeners.get(info.paginationID());
                if (listener != null) {
                    listener.onButtonClick(event, info);
                }
            }
        }
    }

    public static MessageAction disableComponents(Message message) {
        final List<ActionRow> rows = message.getActionRows();
        final List<ActionRow> newRows = new ArrayList<>();

        List<Component> components = new ArrayList<>();
        for (ActionRow row : rows) {
            components.clear();
            for (Component component : row.getComponents()) {
                if (component instanceof Button button) {
                    component = button.asDisabled();
                }
                components.add(component);
            }
            newRows.add(ActionRow.of(components));
        }
        return message.editMessage(new MessageBuilder(message).setActionRows(newRows).build());
    }

    public static RestAction<Void> deleteMessageAndReferrer(User deleter, Message message) {
        final String deletionReason = "Deleted by request of user " + deleter.getAsTag() + " (" + deleter.getId() + ")";
        // We retrieve the message to get the 'full' message object (including the referenced message)
        return message.getChannel().retrieveMessageById(message.getId())
                .flatMap(original -> { // TODO: find a way to make this less ugly
                    final RestAction<Void> deletion = original.delete()
                            .reason(deletionReason);
                    RestAction<Void> ret = deletion;

                    if (original.isFromType(ChannelType.TEXT)
                            && original.getGuild().getSelfMember().hasPermission(original.getTextChannel(), Permission.MESSAGE_MANAGE)
                            && original.getReferencedMessage() != null) {
                        ret = original.getReferencedMessage().delete()
                                .reason(deletionReason)
                                .onErrorMap(ErrorResponse.MISSING_PERMISSIONS::test, t -> null) // Ignore if we can't delete
                                .flatMap(s -> deletion);
                    }
                    return ret;
                });
    }

    @FunctionalInterface
    public interface CustomButtonListener {
        void onButtonClick(ButtonClickEvent event, PaginationButtonInfo info);
    }
}
