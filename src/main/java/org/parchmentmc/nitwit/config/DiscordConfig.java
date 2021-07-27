package org.parchmentmc.nitwit.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.Duration;

@ConfigSerializable
public class DiscordConfig {

    @Setting("prefix_main")
    @Comment("The recognized prefix for commands")
    public String commandPrefix = "&";

    @Setting("prefix_alt")
    @Comment("The alternative prefix for commands")
    @Nullable
    public String alternativePrefix = null;

    @Setting("ignore_dms")
    @Comment("Whether to ignore commands from DMs to the bot")
    public boolean ignoreDMs = true;

    @Setting("commander")
    @Comment("The snowflake ID of the bot commander/admin")
    public long commanderID = 607058472709652501L; // @sciwhiz12

    @Setting("commander_only_mode")
    @Comment("Whether to only recognize commands from the bot commander/admin")
    public boolean commanderOnlyMode = false;

    @Setting("button_limit")
    @Comment("The amount of time that buttons on messages will be 'active' for after message send")
    public Duration buttonTimeLimit = Duration.ofMinutes(30);

    DiscordConfig() {
    }
}
