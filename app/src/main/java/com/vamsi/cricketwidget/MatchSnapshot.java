package com.vamsi.cricketwidget;

public final class MatchSnapshot {
    public final String id;
    public final String competition;
    public final String team1;
    public final String team2;
    public final String flag1;
    public final String flag2;
    public final String score1;
    public final String score2;
    public final String state;
    public final String summary;
    public final String detail;
    public final long updatedAt;

    public MatchSnapshot(
            String team1,
            String team2,
            String score1,
            String score2,
            String state,
            String summary,
            String detail,
            long updatedAt) {
        this("", "", team1, team2, "", "", score1, score2, state, summary, detail, updatedAt);
    }

    public MatchSnapshot(
            String id,
            String competition,
            String team1,
            String team2,
            String flag1,
            String flag2,
            String score1,
            String score2,
            String state,
            String summary,
            String detail,
            long updatedAt) {
        this.id = safe(id, team1 + "-" + team2);
        this.competition = safe(competition, "Cricket");
        this.team1 = safe(team1, "—");
        this.team2 = safe(team2, "—");
        this.flag1 = safe(flag1, "🏏");
        this.flag2 = safe(flag2, "🏏");
        this.score1 = safe(score1, "—");
        this.score2 = safe(score2, "—");
        this.state = safe(state, "unknown");
        this.summary = safe(summary, "");
        this.detail = safe(detail, "");
        this.updatedAt = updatedAt;
    }

    public boolean isLive() {
        return "in".equalsIgnoreCase(state);
    }

    public boolean isUpcoming() {
        return "pre".equalsIgnoreCase(state);
    }

    public String statusLabel() {
        if (isLive()) return "● LIVE";
        if ("post".equalsIgnoreCase(state)) return "FINAL";
        if (isUpcoming()) return "UPCOMING";
        return "SCORE";
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        return value.trim();
    }
}
