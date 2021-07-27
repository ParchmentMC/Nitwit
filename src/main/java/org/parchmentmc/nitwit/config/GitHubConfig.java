package org.parchmentmc.nitwit.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.time.Duration;

@ConfigSerializable
public class GitHubConfig {

    @Setting("organization")
    @Required
    @Comment("The owning organization of the repository and reviewers team")
    public String organization;
    @Setting("repository")
    @Required
    @Comment("The name of the home repository")
    public String repositoryName;
    @Setting("reviewers_team")
    @Required
    @Comment("The slug of the reviewers team")
    public String reviewersTeam;

    @Setting("target_label")
    @Comment("The label for PRs which are considered by Nitwit")
    public String mappingsLabel = "mappings";
    @Setting("short_review_label")
    @Comment("The label for PRs which have the short duration PR review wait time")
    @Nullable
    public String shortDurationLabel = null;

    @Setting("review_duration")
    @Comment("The normal review duration for PR review wait time")
    public Duration reviewDuration = Duration.ofHours(48);
    @Setting("short_review_duration")
    @Comment("The shortened review duration for PR review wait time")
    public Duration shortDuration = Duration.ofHours(24);

    GitHubConfig() {
    }
}
