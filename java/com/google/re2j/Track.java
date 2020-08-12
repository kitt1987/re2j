package com.google.re2j;

public class Track {
    public int Start;
    public int End;
    public String Comments;

    private int flag;
    private boolean frozen;

    Track(int start) {
        Start = start;
    }

    void Freeze(int end, int flag) {
        // √ also calculate Coments
        assureUnfrozen();
        frozen = true;
        End = end;
        this.flag = flag;
    }

    void Freeze(int end, Regexp regexp) {
        // √ also calculate Coments
        assureUnfrozen();
        frozen = true;
        End = end;
    }

    private void assureUnfrozen() {
        if (frozen) {
            throw new IllegalStateException("Track is already frozen");
        }
    }
}
