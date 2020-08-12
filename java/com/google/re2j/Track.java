package com.google.re2j;

import java.util.ArrayList;

public class Track {
    enum Type {
        String,
        Literal,
    }

    public int Start;
    public int End;
    public String Comments;

    private Type type;
    private String value;
    private int flag;
    private boolean frozen;

    static Track ConcatLiterals(ArrayList<Track> sortedLiterals) {
        if (sortedLiterals.size() == 1) {
            return sortedLiterals.get(0);
        }

        Track track = new Track(sortedLiterals.get(0).Start);


        return track;
    }

    Track() {
    }

    Track(int start) {
        Start = start;
    }

    Track(int start, int end, String comments) {
        Start = start;
        End = end;
        Comments = comments;
    }

    void Freeze(int end, int flag) {
        // √ also calculate Coments
        assureUnfrozen();
        frozen = true;
        End = end;
        this.flag = flag;
    }

    void Freeze(int end, Regexp re) {
        // √ also calculate Coments
        assureUnfrozen();
        frozen = true;
        End = end;
        Comments = genComments(re);
    }

    private void assureUnfrozen() {
        if (frozen) {
            throw new IllegalStateException("Track is already frozen");
        }
    }

    private String genComments(Regexp re) {
        StringBuilder b = new StringBuilder();

        switch (re.op) {
            case LITERAL:
                if (re.runes.length > 1) {
                    type = Type.String;
                    b.append("string ");
                    b.append("\"");
                    for (int r : re.runes) {
                        b.appendCodePoint(r);
                    }
                    b.append("\"");
                } else {
                    type = Type.Literal;
                    b.append("literal ");
                    b.append("'");
                    b.appendCodePoint(re.runes[0]);
                    b.append("'");
                }

                break;
        }

        return buildComments();
    }

    private String buildComments() {
        StringBuilder b = new StringBuilder();

        switch (type) {
            case String:
                b.append("string ");
                b.append("\"");
                b.append(value);
                b.append("\"");
                break;
            case Literal:
                b.append("literal ");
                b.append("'");
                b.appendCodePoint(value);
                b.append("'");
                break;
        }
    }
}
