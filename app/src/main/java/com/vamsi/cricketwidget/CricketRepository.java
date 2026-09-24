package com.vamsi.cricketwidget;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Comparator;

public final class CricketRepository {
    private static final String HEADER_URL =
            "https://site.api.espn.com/apis/personalized/v2/scoreboard/header" +
            "?sport=cricket&region=in&tz=Asia%2FCalcutta";

    public MatchSnapshot fetchBestMatch(String preferredTeam) throws Exception {
        List<MatchSnapshot> matches = fetchMatches();
        return matches.get(0);
    }

    /** Returns every event from ESPN's cricket scoreboard, live matches first. */
    public List<MatchSnapshot> fetchMatches() throws Exception {
        String json = get(HEADER_URL);
        JSONObject root = new JSONObject(json);
        JSONArray sports = root.optJSONArray("sports");
        if (sports == null || sports.length() == 0) {
            throw new IllegalStateException("No cricket data returned");
        }

        JSONArray leagues = sports.getJSONObject(0).optJSONArray("leagues");
        if (leagues == null) throw new IllegalStateException("No competitions returned");

        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < leagues.length(); i++) {
            JSONObject league = leagues.optJSONObject(i);
            if (league == null) continue;
            JSONArray events = league.optJSONArray("events");
            if (events == null) continue;

            for (int j = 0; j < events.length(); j++) {
                JSONObject event = events.optJSONObject(j);
                if (event == null) continue;
                Candidate c = parseCandidate(event);
                if (c != null) candidates.add(c);
            }
        }

        if (candidates.isEmpty()) {
            return Collections.singletonList(new MatchSnapshot("empty", "Cricket", "CRIC", "", "🏏", "",
                    "No match", "", "unknown", "No current/upcoming matches found", "", System.currentTimeMillis()));
        }
        Collections.sort(candidates, (a, b) -> Integer.compare(b.rank, a.rank));
        List<MatchSnapshot> matches = new ArrayList<>();
        for (Candidate candidate : candidates) matches.add(candidate.snapshot);
        return matches;
    }

    private Candidate parseCandidate(JSONObject event) {
        JSONArray competitors = event.optJSONArray("competitors");
        if (competitors == null || competitors.length() < 2) return null;

        JSONObject c1 = competitors.optJSONObject(0);
        JSONObject c2 = competitors.optJSONObject(1);
        if (c1 == null || c2 == null) return null;

        String t1Display = c1.optString("displayName", c1.optString("name", "Team 1"));
        String t2Display = c2.optString("displayName", c2.optString("name", "Team 2"));
        String t1 = abbreviation(c1, t1Display);
        String t2 = abbreviation(c2, t2Display);
        String s1 = normalizeScore(c1.optString("score", ""));
        String s2 = normalizeScore(c2.optString("score", ""));

        JSONObject fullStatus = event.optJSONObject("fullStatus");
        JSONObject type = fullStatus == null ? null : fullStatus.optJSONObject("type");
        String state = type == null ? event.optString("status", "") : type.optString("state", "");
        String detail = type == null ? "" : type.optString("description", type.optString("shortDetail", ""));
        String summary = fullStatus == null ? event.optString("summary", "")
                : fullStatus.optString("summary", fullStatus.optString("longSummary", event.optString("summary", "")));

        String eventName = event.optString("shortName", event.optString("name", "Cricket"));
        String eventId = event.optString("id", t1 + "-" + t2 + "-" + eventName);
        MatchSnapshot snapshot = new MatchSnapshot(eventId, eventName, t1, t2,
                flag(c1), flag(c2), s1, s2, state, summary, detail, System.currentTimeMillis());

        int rank = stateRank(state);
        return new Candidate(snapshot, rank);
    }

    private String flag(JSONObject team) {
        String code = team.optString("country", team.optString("countryCode", "")).trim().toUpperCase(Locale.ROOT);
        if (code.length() == 2) return regionalFlag(code);
        String name = team.optString("displayName", team.optString("name", "")).toLowerCase(Locale.ROOT);
        if (name.contains("india")) return "🇮🇳";
        if (name.contains("australia")) return "🇦🇺";
        if (name.contains("south africa")) return "🇿🇦";
        if (name.contains("england")) return "🏴";
        if (name.contains("new zealand")) return "🇳🇿";
        if (name.contains("pakistan")) return "🇵🇰";
        if (name.contains("sri lanka")) return "🇱🇰";
        if (name.contains("bangladesh")) return "🇧🇩";
        if (name.contains("west indies")) return "🏏";
        return "🏏";
    }

    private String regionalFlag(String code) {
        int first = Character.codePointAt(code, 0) - 'A' + 0x1F1E6;
        int second = Character.codePointAt(code, 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

    private int preferenceRank(String pref, String displayName, String abbreviation, boolean national) {
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        String abbr = abbreviation == null ? "" : abbreviation.toLowerCase(Locale.ROOT);
        if (name.equals(pref) || abbr.equals(pref)) return national ? 3000 : 2600;
        if (name.startsWith(pref + " ") || name.contains(pref)) return national ? 2300 : 1800;
        return 0;
    }

    private int stateRank(String state) {
        if ("in".equalsIgnoreCase(state)) return 1000;
        if ("pre".equalsIgnoreCase(state)) return 500;
        if ("post".equalsIgnoreCase(state)) return 100;
        return 0;
    }

    private String abbreviation(JSONObject team, String fallback) {
        String abbr = team.optString("abbreviation", "").trim();
        if (!abbr.isEmpty() && abbr.length() <= 7) return abbr.toUpperCase(Locale.ROOT);
        String name = team.optString("name", "").trim();
        if (!name.isEmpty() && name.length() <= 7) return name.toUpperCase(Locale.ROOT);
        fallback = fallback == null ? "TEAM" : fallback.trim();
        if (fallback.length() <= 7) return fallback.toUpperCase(Locale.ROOT);
        String[] parts = fallback.split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) out.append(Character.toUpperCase(p.charAt(0)));
            if (out.length() >= 5) break;
        }
        return out.length() == 0 ? "TEAM" : out.toString();
    }

    private String normalizeScore(String score) {
        if (score == null || score.trim().isEmpty()) return "—";
        return score.trim()
                .replace(" overs", " ov")
                .replace(" over", " ov");
    }

    private String get(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "VamsiCricketWidget/1.0 (Android)");

        int code = connection.getResponseCode();
        InputStream in = code >= 200 && code < 300
                ? connection.getInputStream() : connection.getErrorStream();
        if (in == null) throw new IllegalStateException("HTTP " + code);

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        } finally {
            connection.disconnect();
        }

        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Score source returned HTTP " + code);
        }
        return body.toString();
    }

    private static final class Candidate {
        final MatchSnapshot snapshot;
        final int rank;

        Candidate(MatchSnapshot snapshot, int rank) {
            this.snapshot = snapshot;
            this.rank = rank;
        }
    }
}
