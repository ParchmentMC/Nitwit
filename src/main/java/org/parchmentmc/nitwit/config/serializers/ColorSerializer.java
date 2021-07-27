package org.parchmentmc.nitwit.config.serializers;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.awt.*;
import java.lang.reflect.Type;

public class ColorSerializer implements TypeSerializer<Color> {
    @Override
    public Color deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final String str = node.getString();
        if (str == null) {
            return null;
        }

        try {
            return Color.decode(str);
        } catch (NumberFormatException e) {
            throw new SerializationException(node, type, "Failed to deserialize color", e);
        }
    }

    @Override
    public void serialize(Type type, @Nullable Color obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.raw(null);
            return;
        }

        node.set("0x%06x".formatted(obj.getRGB()));
    }
}
