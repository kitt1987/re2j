package com.google.re2j;

public class TrackInfo {
    public int Start;
    // End could be updated after parsed for captures.
    public int End;
    public String Info;

    TrackInfo(int start, int end) {
        Start = start;
        End = end;
    }

    TrackInfo(int start, int end, String info) {
        Start = start;
        End = end;
        Info = info;
    }
}