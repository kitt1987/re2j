package com.google.re2j;

import java.util.ArrayList;

public class Track {
    enum Type {
        String,
        Literal,
        DotAll,
        DotInLine,
        Seq,
        Alternation,
        VerticalBar,
    }

    public int Start;
    public int End;
    public String Comments;

    private Type type;
    private String value = "";
    private int flag;
    private boolean frozen;

    static ArrayList<Track> FilterOnlyLiteral(ArrayList<Track> sortedLiterals) {
        int lastLiteralPos = 0;
        for (int i = 0; i < sortedLiterals.size(); i++) {
            Track e = sortedLiterals.get(i);
            if (e.type != Type.Literal) {
                continue;
            }

            if (i != lastLiteralPos) {
                sortedLiterals.set(lastLiteralPos, e);
            }

            lastLiteralPos++;
        }

        for (; lastLiteralPos < sortedLiterals.size(); lastLiteralPos++) {
            sortedLiterals.remove(sortedLiterals);
        }

        return sortedLiterals;
    }

    static Track ConcatOnlyLiterals(ArrayList<Track> sortedLiterals) {
        if (sortedLiterals.size() == 1) {
            return sortedLiterals.get(0);
        }

        Track track = new Track(sortedLiterals.get(0).Start);
        track.type = Type.String;

        for (Track e : sortedLiterals) {
//            if (e.type == Type.String) {
//                continue;
//            }

            if (e.type != Type.Literal) {
                throw new IllegalStateException("must be literal but " + e.type.name());
            }
            track.value += e.value;
        }

        track.Comments = track.buildComments();
        track.Freeze(sortedLiterals.get(sortedLiterals.size()-1).End, 0);
        return track;
    }

    static ArrayList<Track> ConcatOnlyLiterals(ArrayList<Track> dst, ArrayList<Track> src) {
        if (dst.get(0).type == Type.String) {
            dst.remove(0);
        }

        dst.addAll(Track.FilterOnlyLiteral(src));
        dst.add(0, Track.ConcatOnlyLiterals(dst));
        return dst;
    }

    static Track CombineSubTracks(Regexp re) {
        ArrayList<Track> firstTracks = re.subs[0].GetTracks();
        ArrayList<Track> lastTracks = re.subs[re.subs.length-1].GetTracks();
        Track track = new Track(firstTracks.get(0).Start);
        track.Freeze(lastTracks.get(0).End, re);
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

    void Freeze(int end) {
        Freeze(end, 0);
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
            case ANY_CHAR:
                type = Type.DotAll;
                break;
            case ANY_CHAR_NOT_NL:
                type = Type.DotInLine;
                break;
            case CONCAT:
                type = Type.Seq;
                value = joinComments(re.subs);
                break;
            case ALTERNATE:
                type = Type.Alternation;
                value = joinComments(re.subs);
                break;
            case VERTICAL_BAR:
                type = Type.VerticalBar;
        }

        return buildComments();
    }

    private String joinComments(Regexp[] subs) {
        String value = "";
        for (Regexp sub : subs) {
            if (value.length() > 0) {
                value += ",";
            }

            value += sub.GetTopTrack().Comments;
        }

        return value;
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
            case DotAll:
                b.append("any characters including \"\\n\"");
                break;
            case DotInLine:
                b.append("any characters excluding \"\\n\"");
                break;
            case Seq:
                b.append("sequence [").append(value).append("]");
                break;
            case Alternation:
                b.append("alternation of [").append(value).append("]");
                break;
            case VerticalBar:
                b.append("alternation");
                break;
        }

        return b.toString();
    }
}
