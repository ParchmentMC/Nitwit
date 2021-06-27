package org.parchmentmc.nitwit.webhook;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RequiredHeadersFilter extends Filter {
    private final Set<String> requiredHeaders;

    public RequiredHeadersFilter(Set<String> requiredHeaders) {
        Objects.requireNonNull(requiredHeaders, "requiredHeaders == null");
        if (requiredHeaders.isEmpty()) throw new IllegalArgumentException("requiredHeaders needs to be non-empty");
        this.requiredHeaders = requiredHeaders;
    }

    public RequiredHeadersFilter(String... requiredHeaders) {
        this(new HashSet<>(Arrays.asList(requiredHeaders)));
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        final HashSet<String> missingHeaders = new HashSet<>(requiredHeaders);
        missingHeaders.removeIf(str -> exchange.getRequestHeaders().containsKey(str));
        if (!missingHeaders.isEmpty()) { // Missing required headers
            String response = "Missing required headers: " + missingHeaders + "\n";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length());
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        chain.doFilter(exchange);
    }

    @Override
    public String description() {
        return "requires the following headers: " + requiredHeaders;
    }
}
