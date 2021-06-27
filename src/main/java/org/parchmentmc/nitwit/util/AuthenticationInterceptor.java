package org.parchmentmc.nitwit.util;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

/**
 * Interceptor which adds an Authorization token
 */
public class AuthenticationInterceptor implements Interceptor {
    private final String value;

    public static AuthenticationInterceptor bearer(String token) {
        Objects.requireNonNull(token, "token == null");
        return new AuthenticationInterceptor("Bearer " + token);
    }

    AuthenticationInterceptor(String value) {
        this.value = value;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        final Request request = chain.request().newBuilder()
                .addHeader("Authorization", value)
                .build();

        return chain.proceed(request);
    }
}
