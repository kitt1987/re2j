package com.google.re2j;

public class TrackInfo {
    public int Start;
    // End could be updated after parsed for captures.
    public int End;
    public String Info;

    static TrackInfo EmptyMatchTrack() {
        return new TrackInfo(0, 0, "empty");
    }

    TrackInfo(int start) {
        Start = start;
        End = start + 1;
    }

    TrackInfo(int start, int end) {
        Start = start;
        End = end;
    }

    TrackInfo(int start, int end, String info) {
        Start = start;
        End = end;
        Info = info;
    }

    void UpdateStart(TrackInfo that) {
        Start = that.Start;
    }

    void UpdateEnd(TrackInfo that) {
        End = that.End;
    }

    void UpdateEnd(int end) {
        End = end;
    }

    TrackInfo Adjacent(int n) {
        return new TrackInfo(End, End + n);
    }

    void UpdateInfo(String info) {
        Info = info;
    }
}
