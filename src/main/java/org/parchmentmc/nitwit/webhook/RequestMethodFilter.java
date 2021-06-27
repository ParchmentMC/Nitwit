package org.parchmentmc.nitwit.webhook;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

public class RequestMethodFilter extends Filter {
    private final String requestMethod;

    public RequestMethodFilter(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (!exchange.getRequestMethod().equals(requestMethod)) { // We only understand POSTs
            String response = "Unknown request method, we only understand " + requestMethod + "\n";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, response.length());
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        chain.doFilter(exchange);
    }

    @Override
    public String description() {
        return "rejects any method other than " + requestMethod;
    }
}
