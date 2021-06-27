package org.parchmentmc.nitwit.webhook;

import com.fasterxml.jackson.core.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GitHub;
import org.parchmentmc.nitwit.util.io.CallbackInputStream;
import org.parchmentmc.nitwit.util.io.MacInputStream;
import org.parchmentmc.nitwit.webhook.events.WebhookEvent;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.parchmentmc.nitwit.util.LambdaUtil.rethrow;

// https://docs.github.com/en/developers/webhooks-and-events/webhooks/webhook-events-and-payloads#webhook-payload-object-common-properties
public class WebhookHttpHandler implements HttpHandler {
    public static final String GITHUB_DELIVERY_GUID_HEADER = "X-GitHub-Delivery";
    public static final String GITHUB_EVENT_HEADER = "X-GitHub-Event";
    public static final String GITHUB_SIGNATURE_HEADER = "X-Hub-Signature-256";

    @Nullable
    private final byte[] secretToken;
    private final boolean errorOnSignatureMismatch;
    private final Map<String, WebhookEventHandler<?>> eventHandlers = new HashMap<>();

    public WebhookHttpHandler(@Nullable byte[] secretToken, boolean errorOnSignatureMismatch) {
        this.secretToken = secretToken;
        this.errorOnSignatureMismatch = errorOnSignatureMismatch;
    }

    public WebhookHttpHandler() {
        this(null, false);
    }

    public WebhookHttpHandler addHandler(WebhookEventHandler<?> eventHandler) {
        if (eventHandlers.containsKey(eventHandler.getEventName())) {
            throw new IllegalArgumentException("Handler for event '" + eventHandler.getEventName() + "' is already set");
        }
        return setHandler(eventHandler);
    }

    public WebhookHttpHandler setHandler(WebhookEventHandler<?> eventHandler) {
        eventHandlers.put(eventHandler.getEventName(), eventHandler);
        return this;
    }

    public WebhookHttpHandler removeHandler(WebhookEventHandler<?> eventHandler) {
        eventHandlers.remove(eventHandler.getEventName(), eventHandler);
        return this;
    }

    public WebhookHttpHandler removeHandler(String eventName) {
        eventHandlers.remove(eventName);
        return this;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        validateSignatures(exchange);

        final String event = exchange.getRequestHeaders().getFirst(GITHUB_EVENT_HEADER);
        if (event == null) {
            String response = "Missing " + GITHUB_EVENT_HEADER + " request header.\n";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length());
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        final String guid = exchange.getRequestHeaders().getFirst(GITHUB_DELIVERY_GUID_HEADER);
        if (guid == null) {
            String response = "Missing " + GITHUB_DELIVERY_GUID_HEADER + " request header.\n";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length());
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }
        final UUID deliveryID = UUID.fromString(guid);

        final WebhookEventHandler<?> handler = eventHandlers.get(event);
        if (handler == null) {
            String response = "This webhook handler does not recognize '" + event + "' events.\n";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, response.length());
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        doHandle(exchange, deliveryID, handler);
    }

    private <T extends WebhookEvent> void doHandle(HttpExchange exchange, UUID deliveryID, WebhookEventHandler<T> handler) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody();
             JsonParser parser = GitHub.getMappingObjectReader().createParser(inputStream)) {
            final T eventPayload = parser.readValueAs(handler.getEventClass());

            if (inputStream.available() > 0) {
                // Data remains, read it all now (so if validating, it reaches end of stream)
                // We dump it as we don't care about it
                System.err.println("Bytes remaining after reading event payload: " + inputStream.available());
                inputStream.transferTo(OutputStream.nullOutputStream());
            }

            handler.handleEvent(deliveryID, eventPayload);
        }
    }

    private static final String HMAC_SHA256 = "HmacSHA256";

    private void validateSignatures(HttpExchange exchange) throws IOException {
        if (secretToken != null) { // Validate signatures
            Mac mac;
            try {
                mac = Mac.getInstance(HMAC_SHA256);
                mac.init(new SecretKeySpec(secretToken, HMAC_SHA256));
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Failed to create mac for SHA-256; should be impossible as it's JDK-mandated to exist", e);
            } catch (InvalidKeyException e) {
                throw new IOException("Failed to initialize mac with key; should be impossible as they both are for SHA-256", e);
            }

            final String ghSignature = exchange.getRequestHeaders().getFirst(GITHUB_SIGNATURE_HEADER);
            exchange.setStreams(
                    new CallbackInputStream<>(new MacInputStream(mac, exchange.getRequestBody()),
                            rethrow((MacInputStream in) -> compareSignatures(in, ghSignature))),
                    exchange.getResponseBody());
        }
    }

    private void compareSignatures(MacInputStream inputStream, String expected) throws IOException {
        String actual = "sha256=" + MacInputStream.bytesToHex(inputStream.getMac().doFinal());
        if (!actual.equals(expected)) {
            System.err.printf("Signatures do not match: expected '%s', actual '%s'%n", expected, actual);
            if (errorOnSignatureMismatch) {
                throw new IOException("Signatures do not match: expected '" + expected + "', actual '" + actual + "'");
            }
        }
    }
}
