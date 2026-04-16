package de.uniko.bas.cnx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RestBroker {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public String doGetAuth(String url) throws IOException, InterruptedException {
        String auth = Config.AUTH.username() + ":" + Config.AUTH.password();
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + encodedAuth)
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    public byte[] doGetAuthBytes(String url) throws IOException, InterruptedException {
        String auth = Config.AUTH.username() + ":" + Config.AUTH.password();
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Basic " + encodedAuth) // adapt to your config
                .GET()
                .build();

        HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

        if (res.statusCode() / 100 != 2) {
            String ct = res.headers().firstValue("content-type").orElse("");
            throw new RuntimeException("GET failed: " + res.statusCode() + " content-type=" + ct + " url=" + url);
        }

        return res.body();
    }

    public String doGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }
}
