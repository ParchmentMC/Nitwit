package org.parchmentmc.nitwit.discord.commands.impl;

import com.apollographql.apollo.ApolloClient;
import com.github.api.fragment.PullRequestInfo;
import com.github.api.type.MergeableState;
import com.github.api.type.PullRequestReviewState;
import com.github.api.type.RepositoryPermission;
import com.github.api.type.StatusState;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.reactivex.rxjava3.core.Single;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.parchmentmc.nitwit.config.CosmeticConfig;
import org.parchmentmc.nitwit.discord.DiscordClient;
import org.parchmentmc.nitwit.discord.commands.CommandHelper;
import org.parchmentmc.nitwit.discord.commands.MessageContext;
import org.parchmentmc.nitwit.util.pr.RequirementCalculator;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class QueryPullRequestCommand {
    public static void register(CommandDispatcher<MessageContext> dispatcher) {
        dispatcher.register(CommandHelper.literal("query")
                .then(CommandHelper.argument("number", IntegerArgumentType.integer(1))
                        .executes(QueryPullRequestCommand::doQuery)
                )
        );
    }

    static record MembersInfo(List<String> reviewers, List<String> collaborators) {
    }

    private static int doQuery(CommandContext<MessageContext> ctx) {
        final int number = IntegerArgumentType.getInteger(ctx, "number");
        final MessageContext msgCtx = ctx.getSource();
        final DiscordClient client = msgCtx.getClient();
        final Message message = msgCtx.getMessage();

        final ApolloClient graphQL = client.getGraphQLClient();
        final String organization = client.config().github.organization;
        final String repository = client.config().github.repositoryName;
        final String teamName = client.config().github.reviewersTeam;

        final Single<Message> replySingle = Single.fromFuture(message.replyEmbeds(CommandHelper.createProcessingEmbed()).submit());

        final Single<List<String>> teamMembersSingle =
                RequirementCalculator.getTeamMembers(graphQL, organization, teamName);
        final Single<List<String>> collaboratorsSingle =
                RequirementCalculator.getCollaborators(graphQL, organization, repository, RepositoryPermission.WRITE);

        final Single<MembersInfo> membersInfoSingle = Single.zip(teamMembersSingle, collaboratorsSingle, MembersInfo::new);

        final Single<RequirementCalculator.PullRequestState> prStateSingle = RequirementCalculator.queryState(graphQL, organization, repository, number);

        final Single<Message> messageSingle = Single
                .zip(prStateSingle, membersInfoSingle, (prState, membersInfo) -> createMessage(client, prState, membersInfo))
                .onErrorReturn(CommandHelper::createErrorMessage);

        Single.zip(replySingle, messageSingle, (reply, newMessage) -> Single.fromFuture(reply.editMessage(newMessage).submit()))
                .blockingSubscribe();

        return Command.SINGLE_SUCCESS;
    }

    private static Message createMessage(DiscordClient client, RequirementCalculator.PullRequestState prState,
                                         MembersInfo membersInfo) {
        final MessageBuilder builder = new MessageBuilder();

        final MessageEmbed mainEmbed = createMainEmbed(client, prState, membersInfo);
        final MessageEmbed reviewsEmbed = createReviewsEmbed(client, membersInfo, prState);

        if (reviewsEmbed != null) {
            builder.setEmbeds(mainEmbed, reviewsEmbed);
        } else {
            builder.setEmbeds(mainEmbed);
        }

        return builder.build();
    }

    @Nullable
    private static MessageEmbed createReviewsEmbed(DiscordClient client, MembersInfo membersInfo,
                                                   RequirementCalculator.PullRequestState prState) {
        final List<RequirementCalculator.Review> changeRequestingReviews = prState.latestReviewsWithState(PullRequestReviewState.CHANGES_REQUESTED);
        final List<RequirementCalculator.Review> approvingReviews = prState.latestReviewsWithState(PullRequestReviewState.APPROVED);
        if (changeRequestingReviews.size() == 0 && approvingReviews.size() == 0) {
            return null;
        }

        final EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.GRAY)
                .setTitle("Pull Request Reviews")
                .setFooter("italics: mappings team, bold: reviewers sub-team");

        final StringBuilder description = builder.getDescriptionBuilder();

        final CosmeticConfig.EmoteConfig emotes = client.config().cosmetic.emotes;
        for (RequirementCalculator.Review review : prState.latestReviews()) {
            Emoji stateEmote = switch (review.state()) {
                case APPROVED -> emotes.reviewApprove;
                case CHANGES_REQUESTED -> emotes.reviewChanges;
                case COMMENTED, DISMISSED -> emotes.reviewNeutral;
                default -> emotes.pending;
            };

            description.append(stateEmote.getAsMention())
                    .append(" from ")
                    .append(prefixAndMarkdown(review.userLogin(), membersInfo))
                    .append(" on ")
                    .append(TimeFormat.DATE_TIME_SHORT.format(review.submitTime()))
                    .append(" (")
                    .append(MarkdownUtil.maskedLink("link", review.url().toASCIIString()))
                    .append(')')
                    .append('\n');
        }

        return builder.build();
    }

    private static MessageEmbed createMainEmbed(DiscordClient client, RequirementCalculator.PullRequestState prState,
                                                MembersInfo membersInfo) {
        final RequirementCalculator.Review baseReview = RequirementCalculator.getTimeBaseReview(prState.latestReviews(),
                membersInfo.collaborators()::contains, membersInfo.reviewers()::contains);

        final PullRequestInfo info = prState.pullRequest().fragments().pullRequestInfo();

        final EmbedBuilder builder = new EmbedBuilder();

        final CosmeticConfig.EmoteConfig emotes = client.config().cosmetic.emotes;
        final Emoji stateEmote = switch (info.state()) {
            case OPEN -> info.isDraft() ? emotes.prDraft : emotes.prOpen;
            case CLOSED -> emotes.prClosed;
            case MERGED -> emotes.prMerged;
            default -> emotes.pending;
        };

        final CosmeticConfig.ColorsConfig colors = client.config().cosmetic.colors;
        builder.setColor(switch (info.state()) {
            case OPEN -> info.isDraft() ? colors.neutral : colors.success;
            case CLOSED -> colors.danger;
            case MERGED -> colors.merged;
            default -> null;
        });

        builder.setTitle(stateEmote.getAsMention() + " Pull Request #" + info.number(), info.url().toASCIIString());
        builder.appendDescription(MarkdownUtil.codeblock(info.title()) + '\n');
        final PullRequestInfo.Author author = info.author();
        if (author != null) {
            builder.setAuthor('@' + author.login(), author.url().toASCIIString(), author.avatarUrl().toASCIIString());
        }

        final MergeableState mergeableState = prState.mergeableState();
        final StringBuilder fieldBuilder = new StringBuilder();

        final String labels = Optional.ofNullable(info.labels())
                .map(PullRequestInfo.Labels::nodes)
                .stream()
                .flatMap(Collection::stream)
                .map(PullRequestInfo.Node::name)
                .map(MarkdownUtil::monospace)
                .collect(Collectors.joining(", "));
        if (!labels.isBlank()) {
            builder.appendDescription("Labels: " + labels);
        }

        //

        fieldBuilder
                .append(CommandHelper.getIcon(client, mergeableState == MergeableState.MERGEABLE).getAsMention())
                .append(' ')
                .append(switch (mergeableState) {
                    case MERGEABLE -> "Can be cleanly merged";
                    case CONFLICTING -> "Contains merge conflicts";
                    case UNKNOWN -> "Under calculation";
                    default -> "Unknown mergeability state: " + mergeableState.rawValue();
                })
                .append('\n');

        final StatusState statusState = prState.commitStatus();
        fieldBuilder
                .append(CommandHelper.getIcon(client, statusState == StatusState.SUCCESS).getAsMention())
                .append(' ')
                .append(switch (statusState) {
                    case EXPECTED -> "Status checks are expected to run";
                    case ERROR -> "Errors in status checks";
                    case FAILURE -> "Failed status checks";
                    case PENDING -> "Pending status checks";
                    case SUCCESS -> "Successful status checks";
                    default -> "Unknown status check state: " + statusState.rawValue();
                })
                .append('\n');

        final long unresolved = prState.unresolvedThreads();
        fieldBuilder
                .append(CommandHelper.getIcon(client, unresolved == 0).getAsMention())
                .append(' ')
                .append(unresolved == 0 ? "No" : "**" + unresolved + "**")
                .append(" unresolved conversation")
                .append(unresolved == 1 ? "" : "s")
                .append('\n');

        final List<RequirementCalculator.Review> changeRequestingReviews = prState.latestReviewsWithState(PullRequestReviewState.CHANGES_REQUESTED);
        fieldBuilder
                .append(CommandHelper.getIcon(client, changeRequestingReviews.size() == 0).getAsMention())
                .append(' ')
                .append(changeRequestingReviews.size() == 0 ? "No" : "**" + changeRequestingReviews.size() + "**")
                .append(" change requesting review")
                .append(changeRequestingReviews.size() == 1 ? "" : "s")
                .append('\n');

        final List<RequirementCalculator.Review> approvingReviews = prState.latestReviewsWithState(PullRequestReviewState.APPROVED);
        fieldBuilder
                .append(CommandHelper.getIcon(client, approvingReviews.size() >= 2).getAsMention())
                .append(' ')
                .append(approvingReviews.size() == 0 ? "No" : "**" + approvingReviews.size() + "**")
                .append(" approving review")
                .append(approvingReviews.size() == 1 ? "" : "s")
                .append('\n');

        builder.addField("Requirements", fieldBuilder.toString(), false);

        if (baseReview != null) {
            Duration lastCallDuration = client.config().github.reviewDuration;
            final String shortDurationLabel = client.config().github.shortDurationLabel;
            if (shortDurationLabel != null) {
                if (Optional.ofNullable(info.labels())
                        .map(PullRequestInfo.Labels::nodes)
                        .stream()
                        .flatMap(Collection::stream)
                        .map(PullRequestInfo.Node::name)
                        .anyMatch(shortDurationLabel::equals)) {

                    lastCallDuration = client.config().github.shortDuration;
                }
            }

            final OffsetDateTime timeBase = baseReview.submitTime();
            final OffsetDateTime mergeReadyTime = timeBase.plus(lastCallDuration);
            final Duration duration = Duration.between(mergeReadyTime, OffsetDateTime.now(ZoneOffset.UTC));

            builder.addField("Time until Ready for Merge",
                    CommandHelper.getIcon(client, !duration.isNegative()).getAsMention()
                            + ' ' + TimeFormat.RELATIVE.format(mergeReadyTime), true);
        }

        return builder.build();
    }

    private static String prefixAndMarkdown(String login, MembersInfo info) {
        String ret = '@' + login;

        if (info.collaborators().contains(login)) {
            ret = MarkdownUtil.italics(ret);
        }
        if (info.reviewers().contains(login)) {
            ret = MarkdownUtil.bold(ret);
        }

        return ret;
    }
}
