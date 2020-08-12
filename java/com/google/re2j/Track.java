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
    private String value = "";
    private int flag;
    private boolean frozen;

    static Track ConcatLiteralOrString(ArrayList<Track> sortedLiterals) {
        if (sortedLiterals.size() == 1) {
            return sortedLiterals.get(0);
        }

        Track track = new Track(sortedLiterals.get(0).Start);
        track.type = Type.String;

        for (Track e : sortedLiterals) {
            if (e.type == Type.String) {
                continue;
            }

            if (e.type != Type.Literal) {
                throw new IllegalStateException("must be literal but " + e.type.name());
            }
            track.value += e.value;
        }

        track.Comments = track.buildComments();
        track.Freeze(sortedLiterals.get(sortedLiterals.size()-1).End, 0);
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
        switch (re.op) {
            case LITERAL:
                if (re.runes.length > 1) {
                    type = Type.String;
                    StringBuilder b = new StringBuilder();
                    for (int r : re.runes) {
                        b.appendCodePoint(r);
                    }
                    value = b.toString();
                } else {
                    type = Type.Literal;
                    value = Utils.runeToString(re.runes[0]);
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
                b.append(value);
                b.append("'");
                break;
        }

        return b.toString();
    }
}
