package org.parchmentmc.nitwit;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.parchmentmc.nitwit.webhook.RequestMethodFilter;
import org.parchmentmc.nitwit.webhook.RequiredHeadersFilter;
import org.parchmentmc.nitwit.webhook.WebhookHttpHandler;
import org.parchmentmc.nitwit.webhook.events.PingEvent;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;

import static org.parchmentmc.nitwit.webhook.WebhookHttpHandler.GITHUB_DELIVERY_GUID_HEADER;
import static org.parchmentmc.nitwit.webhook.WebhookHttpHandler.GITHUB_EVENT_HEADER;

public class WebhookMain {
    private static final int PORT = 3000;

    public static void main(String[] args) throws Exception {
        final SSLContext sslContext = createSSLContext();
        SSLContext.setDefault(sslContext); // Set as default SSL context

        final HttpsServer server = HttpsServer.create(new InetSocketAddress(PORT), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));

        server.setExecutor(Executors.newSingleThreadExecutor()); // Single thread executor

        final HttpContext webhookContext = server.createContext("/webhook");

        // We only care about POSTs
        webhookContext.getFilters().add(new RequestMethodFilter("POST"));
        // We require the github event and github delivery GUID headers
        webhookContext.getFilters().add(new RequiredHeadersFilter(GITHUB_EVENT_HEADER, GITHUB_DELIVERY_GUID_HEADER));

        webhookContext.setHandler(new WebhookHttpHandler().addHandler(new PingEvent.Handler()));

        server.start();
    }

    static SSLContext createSSLContext() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        trustManagerFactory.init((KeyStore) null); // Initialize with default trust store

        final SSLContext sslContext = SSLContext.getInstance("TLS");

        // No additional authentication keys, default trust store, no custom random source
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        return sslContext;
    }
}
