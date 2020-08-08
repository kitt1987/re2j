package com.google.re2j;

public class TrackInfo {
    public int Start;
    public int Len;
    public String Info;

    TrackInfo(int start, int len) {
        Start = start;
        Len = len;
    }

    TrackInfo(int start, int len, String info) {
        Start = start;
        Len = len;
        Info = info;
    }
}
