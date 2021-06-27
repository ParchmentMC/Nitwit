package org.parchmentmc.nitwit.util;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

public class ConsumerBackedCallback<D> extends ApolloCall.Callback<D> {
    private final Consumer<Response<D>> onResponse;
    private final Consumer<ApolloException> onFailure;

    protected ConsumerBackedCallback(Consumer<Response<D>> onResponse, Consumer<ApolloException> onFailure) {
        Objects.requireNonNull(onResponse, "onResponse == null");
        Objects.requireNonNull(onFailure, "onFailure == null");
        this.onResponse = onResponse;
        this.onFailure = onFailure;
    }

    @Override
    public void onResponse(@NotNull Response<D> response) {
        onResponse.accept(response);
    }

    @Override
    public void onFailure(@NotNull ApolloException e) {
        onFailure.accept(e);
    }

    public static <D> ApolloCall.Callback<D> response(Consumer<Response<D>> onResponse) {
        return callback(onResponse, LambdaUtil::sneakyThrow);
    }

    public static <D> ApolloCall.Callback<D> callback(Consumer<Response<D>> onResponse, Consumer<ApolloException> onFailure) {
        return new ConsumerBackedCallback<>(onResponse, onFailure);
    }
}
