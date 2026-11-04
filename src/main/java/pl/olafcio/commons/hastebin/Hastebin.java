package pl.olafcio.commons.hastebin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public enum Hastebin {
    ;

    private static final Gson GSON = new Gson();

    public static String create(Iterable<BinFile> files, boolean comments_enabled, long expires_at) throws IOException {
        var connection = (HttpURLConnection) URI.create("https://backend.ianhon.com/hastebin/paste/create")
                                                .toURL()
                                                .openConnection();

        connection.setRequestMethod("POST");

        connection.addRequestProperty("Accept", "application/json, text/plain, */*");
        connection.addRequestProperty("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8");
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("Sec-Ch-Ua", "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Google Chrome\";v=\"144\"");
        connection.addRequestProperty("Sec-Ch-Ua-Mobile", "?0");
        connection.addRequestProperty("Sec-Ch-Ua-Platform", "\"Windows\"");
        connection.addRequestProperty("Sec-Fetch-Dest", "empty");
        connection.addRequestProperty("Sec-Fetch-Mode", "cors");
        connection.addRequestProperty("Sec-Fetch-Site", "same-site");
        connection.addRequestProperty("Referer", "https://hastebin.ianhon.com/");

        connection.setDoInput(true);
        connection.setDoOutput(true);

        var json = new JsonObject();
        var content = new JsonArray();

        for (var file : files) {
            var obj = new JsonObject();

            obj.addProperty("filename", file.filename());
            obj.addProperty("content", file.content());

            content.add(obj);
        }

        json.addProperty("content", GSON.toJson(content));
        json.addProperty("comments_enabled", comments_enabled);
        json.addProperty("expires_at", expires_at);

        connection.getOutputStream().write(GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
        connection.connect();

        return Integer.toHexString(GSON.fromJson(new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8), JsonObject.class)
                                       .get("id")
                                       .getAsInt());
    }
}
