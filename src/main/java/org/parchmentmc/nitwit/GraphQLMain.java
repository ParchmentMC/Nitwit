package org.parchmentmc.nitwit;

import com.apollographql.apollo.ApolloClient;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Nullable;
import org.parchmentmc.nitwit.graphql.GetLabelsQuery;
import org.parchmentmc.nitwit.graphql.LabelPullRequestMutation;
import org.parchmentmc.nitwit.graphql.PullRequestsQuery;
import org.parchmentmc.nitwit.util.AuthenticationInterceptor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import static org.parchmentmc.nitwit.util.ConsumerBackedCallback.response;

public class GraphQLMain {
    static ApolloClient client;

    public static void main(String[] args) {
        client = ApolloClient.builder()
                .serverUrl("https://api.github.com/graphql")
                .okHttpClient(new OkHttpClient.Builder()
                        .cache(RESTMain.OKHTTP_CACHE)
                        .addInterceptor(AuthenticationInterceptor.bearer(RESTMain.TOKEN))
                        .build())
                .build();

        String labelName = "ready to merge";
        client.query(new GetLabelsQuery("Parchment", "ParchmentMC", labelName))
                .enqueue(response(response -> {
                    System.out.println("labels query");
                    Objects.requireNonNull(response.getData(), "Response has no data");
                    final GetLabelsQuery.Repository repository = Objects.requireNonNull(response.getData().repository(), "No repository");
                    final GetLabelsQuery.Labels labels = repository.labels();
                    if (labels == null) {
                        System.out.println("No labels");
                        return;
                    }
                    final List<GetLabelsQuery.Node> nodes = labels.nodes();
                    if (nodes == null) {
                        System.out.println("No nodes");
                        return;
                    }
                    String labelID = null;
                    for (GetLabelsQuery.Node label : nodes) {
                        System.out.print("Label: " + label.name());
                        if (label.name().equals(labelName)) {
                            System.out.print(" [found it]");
                            labelID = label.id();
                        }
                        System.out.println();
                    }
                    if (labelID != null) {
                        checkPRs(labelID);
                    }
                }));

    }

    static void checkPRs(String mergeLabelID) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        client.query(new PullRequestsQuery("Parchment", "ParchmentMC", "mappings"))
                .enqueue(response(response -> {
                    Objects.requireNonNull(response.getData(), "Response has no data");
                    final PullRequestsQuery.Repository repository = Objects.requireNonNull(response.getData().repository(), "No repository");
                    final List<PullRequestsQuery.Edge> edges = repository.pullRequests().edges();
                    if (edges == null) return;
                    for (PullRequestsQuery.Edge edge : edges) {
                        final PullRequestsQuery.Node pr = edge.node();
                        if (pr == null) continue;

                        System.out.print("PR #" + pr.number());
                        final PullRequestsQuery.LatestReviews reviews = pr.latestReviews();
                        if (reviews != null) {
                            final List<PullRequestsQuery.Edge1> reviewEdges = reviews.edges();
                            if (reviewEdges != null) {
                                boolean hasRejects = false;
                                int approvals = 0;
                                OffsetDateTime secondApproveDate = null;
                                final ListIterator<PullRequestsQuery.Edge1> iterator = reviewEdges.listIterator(reviewEdges.size());
                                while (iterator.hasPrevious()) {
                                    final PullRequestsQuery.Node1 review = iterator.previous().node();
                                    if (review == null) continue;

                                    switch (review.state()) {
                                        case APPROVED -> {
                                            approvals++;
                                            if (approvals == 2) {
                                                secondApproveDate = asDateTime(review.submittedAt());
                                                Objects.requireNonNull(secondApproveDate, "Second approval review has no submission datetime");
                                            }
                                        }
                                        case CHANGES_REQUESTED -> hasRejects = true;
                                    }
                                }
                                if (approvals >= 2 && secondApproveDate != null && secondApproveDate.plusDays(2).isBefore(now) && !hasRejects) {
                                    System.out.print(" ready for merge");

                                    if (false) { // Disabled to not bo any mutators
                                        client.mutate(new LabelPullRequestMutation(pr.id(), List.of(mergeLabelID)))
                                                .enqueue(response(resp -> System.err.println("Labelled PR #" + pr.number())));
                                    }
                                }
                            }
                        }
                        System.out.println();
                    }

                }));
    }

    @Nullable
    static OffsetDateTime asDateTime(@Nullable Object obj) {
        if (!(obj instanceof String str)) return null;
        return OffsetDateTime.parse(str);
    }
}
