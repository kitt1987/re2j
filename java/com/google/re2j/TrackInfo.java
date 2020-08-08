package com.google.re2j;

public class TrackInfo {
    public int Start;
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
