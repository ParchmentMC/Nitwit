package org.parchmentmc.nitwit.util.graphql;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Error;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx3.Rx3Apollo;
import io.reactivex.rxjava3.core.Observable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GraphQLHelper {
    private GraphQLHelper() { // Prevent instantiation
    }

    public static <D> Observable<Response<D>> queryRxJava(ApolloClient client, Query<?, D, ?> query) {
        return Rx3Apollo.from(client.query(query))
                .map(resp -> {
                    if (resp.hasErrors()) {
                        throw new GraphQLException("**Operation " + resp.getOperation().name() + " has errors.**", resp.getErrors());
                    }
                    return resp;
                });
    }

    public static class GraphQLException extends RuntimeException {
        private final List<Error> errors;

        public GraphQLException(String message, List<Error> errors) {
            super(message);
            this.errors = List.copyOf(errors);
        }

        public List<Error> getErrors() {
            return errors;
        }

        @Override
        public String getMessage() {
            return Stream.concat(Stream.of(super.getMessage()),
                    errors.stream().map(err -> Stream.concat(
                            Stream.of(err.getMessage()),
                            Stream.concat(
                                    err.getLocations().stream()
                                            .map(s -> "    at line %s, column %s".formatted(s.getLine(), s.getColumn())),
                                    err.getCustomAttributes().entrySet().stream()
                                            .map(s -> "    with %s as %s".formatted(s.getKey(), Objects.toString(s.getValue())))
                            )).collect(Collectors.joining("\n"))))
                    .collect(Collectors.joining("\n"));
        }
    }
}
