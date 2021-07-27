package org.parchmentmc.nitwit.config.serializers;

import net.dv8tion.jda.api.entities.Emoji;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class EmojiSerializer implements TypeSerializer<Emoji> {
    @Override
    public Emoji deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final String str = node.getString();
        if (str == null) {
            return null;
        }
        try {
            return Emoji.fromMarkdown(str);
        } catch (RuntimeException e) {
            throw new SerializationException(node, type, "Failed to deserialize emoji", e);
        }
    }

    @Override
    public void serialize(Type type, @Nullable Emoji obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }

        node.set(obj.getAsMention());
    }
}
