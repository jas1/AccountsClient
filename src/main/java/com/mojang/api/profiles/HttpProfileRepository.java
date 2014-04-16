
package com.mojang.api.profiles;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.heldplayer.util.json.JSONArray;
import me.heldplayer.util.json.JSONObject;
import me.heldplayer.util.json.JSONWriter;

import com.mojang.api.http.BasicHttpClient;
import com.mojang.api.http.HttpBody;
import com.mojang.api.http.HttpClient;
import com.mojang.api.http.HttpHeader;

public class HttpProfileRepository implements ProfileRepository {

    // You're not allowed to request more than 100 profiles per go.
    private static final int PROFILES_PER_REQUEST = 100;

    private final String agent;
    private HttpClient client;

    public HttpProfileRepository(String agent) {
        this(agent, BasicHttpClient.getInstance());
    }

    public HttpProfileRepository(String agent, HttpClient client) {
        this.agent = agent;
        this.client = client;
    }

    @Override
    public Profile[] findProfilesByNames(String... names) {
        List<Profile> profiles = new ArrayList<Profile>();
        try {

            List<HttpHeader> headers = new ArrayList<HttpHeader>();
            headers.add(new HttpHeader("Content-Type", "application/json"));

            int namesCount = names.length;
            int start = 0;
            int i = 0;
            do {
                int end = PROFILES_PER_REQUEST * (i + 1);
                if (end > namesCount) {
                    end = namesCount;
                }
                String[] namesBatch = Arrays.copyOfRange(names, start, end);
                HttpBody body = getHttpBody(namesBatch);
                Profile[] result = this.post(this.getProfilesUrl(), body, headers);
                profiles.addAll(Arrays.asList(result));

                start = end;
                i++;
            }
            while (start < namesCount);
        }
        catch (Exception e) {
            e.printStackTrace();
            // TODO: logging and allowing consumer to react?
        }

        return profiles.toArray(new Profile[profiles.size()]);
    }

    private URL getProfilesUrl() throws MalformedURLException {
        // To lookup Minecraft profiles, agent should be "minecraft"
        return new URL("https://api.mojang.com/profiles/" + this.agent);
    }

    private Profile[] post(URL url, HttpBody body, List<HttpHeader> headers) throws IOException {
        String response = this.client.post(url, body, headers);
        JSONArray array = new JSONArray(response);
        int size = 0;
        for (Object obj : array.values) {
            if (obj instanceof JSONObject) {
                size++;
            }
        }
        Profile[] profiles = new Profile[size];
        int i = 0;
        for (Object obj : array.values) {
            if (obj instanceof JSONObject) {
                JSONObject entry = (JSONObject) obj;

                Profile profile = profiles[i++] = new Profile();

                profile.setId(entry.getString("id"));
                profile.setName(entry.getString("name"));
            }
        }
        return profiles;
    }

    private static HttpBody getHttpBody(String... namesBatch) {
        JSONArray array = new JSONArray();
        for (String name : namesBatch) {
            array.values.add(name);
        }
        return new HttpBody(JSONWriter.write(array));
    }

}
