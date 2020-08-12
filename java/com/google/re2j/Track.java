package com.google.re2j;

public class Track {
    public int Start;
    public int End;
    public String Comments;

    private int flag;

    Track(int start) {
        Start = start;
    }

    void Freeze(int end, int flag) {
        // √ also calculate Coments
        End = end;
        this.flag = flag;
    }
}
