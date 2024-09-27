package net.bladehunt.rpp;

import cloud.prefab.sse.SSEHandler;
import cloud.prefab.sse.events.DataEvent;
import cloud.prefab.sse.events.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.BiConsumer;

public class RppClient {
    private final HttpClient httpClient;

    private final URI uri;

    private final BiConsumer<URI, String> packConsumer;

    private final HttpRequest request;

    private String latestHash = null;

    public RppClient(
            @NotNull URI uri,
            @NotNull BiConsumer<URI, String> packConsumer
    ) {
        this.httpClient = HttpClient.newBuilder().build();
        this.uri = uri;
        this.packConsumer = packConsumer;

        this.request = HttpRequest
                .newBuilder()
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofSeconds(5))
                .uri(uri.resolve("sse"))
                .build();
    }

    @NotNull
    public CompletableFuture<HttpResponse<Void>> start() {
        SSEHandler sseHandler = new SSEHandler();
        sseHandler.subscribe(new FlowSubscriber());

        return httpClient.sendAsync(
                request,
                HttpResponse.BodyHandlers.fromLineSubscriber(sseHandler)
        );
    }

    @Nullable
    public String getLatestHash() {
        return latestHash;
        }

    private class FlowSubscriber implements Flow.Subscriber<Event> {
        private Flow.Subscription subscription = null;
        private final URI packUri = uri.resolve("pack");

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(Event event) {
            data: if (event instanceof DataEvent dataEvent) {
                if (!dataEvent.getEventName().equals("update")) break data;
                String hash = dataEvent.getData();

                latestHash = hash;

                try {
                    packConsumer.accept(packUri, hash);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {}

        @Override
        public void onComplete() {}
    }

    @NotNull
    public URI getUri() {
        return uri;
    }
}
