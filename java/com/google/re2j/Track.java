package com.google.re2j;

public class Track {
    public int Start;
    public int End;
    public String Info;

    private int flag;

    Track(int start) {
        Start = start;
    }

    void Freeze(int end, int flag) {
        End = end;
        this.flag = flag;
    }
}
