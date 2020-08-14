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
        AnchorBeginLine,
        CharClass,
        CapturingGroup,
        CapturingGroupStart,
        Quantifier,
    }

    public int Start;
    public int End;
    public String Comments;

    private Type type;
    private String[] value;

    static Track BuildCapturingEndTrack(int start) {
        return new Track(start, start+1, "capturing group end");
    }

    static Track BuildCaptureTopTrack(Regexp re) {
        ArrayList<Track> tracks = re.GetRawTracks();
        if (tracks.size() != 2) {
            throw new IllegalStateException("count of self tracks of capture must be 2");
        }

        Track track = new Track(tracks.get(0).Start);
        track.Freeze(tracks.get(1).End, re);
        return track;
    }

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

    void Close(int end) {
        End = end;
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
                break;
            case BEGIN_LINE:
                type = Type.AnchorBeginLine;
                break;
            case CHAR_CLASS:
                if (re.HasJoinTrack()) {
                    type = Type.Alternation;
                    // GetRawTracks will never call funcs to manipulate Track.
                    value = joinComments(re.GetRawTracks());
                    break;
                }

                type = Type.CharClass;
                break;
            case CAPTURE:
                type = Type.CapturingGroup;
                value = joinComments(re.subs);
                break;
            case LEFT_PAREN:
                type = Type.CapturingGroupStart;
                break;
            case STAR:
            case PLUS:
            case QUEST:
                type = Type.Quantifier;
                break;
        }

        return buildComments();
    }

    private String joinComments(Regexp[] subs) {
        StringBuilder value = new StringBuilder();
        for (Regexp sub : subs) {
            if (value.length() > 0) {
                value.append(",");
            }

            value.append(sub.GetTopTrack().Comments);
        }

        return value.toString();
    }

    private String joinComments(ArrayList<Track> tracks) {
        StringBuilder value = new StringBuilder();
        for (Track track : tracks) {
            if (value.length() > 0) {
                value.append(",");
            }

            value.append(track.Comments);
        }

        return value.toString();
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
            case AnchorBeginLine:
                b.append("line start");
                break;
            case CharClass:
                b.append("character class");
                break;
            case CapturingGroup:
                b.append("capturing group (").append(value).append(")");
                break;
            case CapturingGroupStart:
                b.append("capturing group");
                break;
            case Quantifier:
                b.append("quantifier");
                break;
        }

        return b.toString();
    }
}
