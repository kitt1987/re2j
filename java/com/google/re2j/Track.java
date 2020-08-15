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

    // Update type, value and calculate the top most Track
    void Close(int end, Regexp re) {
        End = end;
        UpdateComments(re);
    }

    // Update type and value
    void Close(int end, String text) {
        End = end;
    }

    void UpdateComments(Regexp re) {
        StringBuilder b = new StringBuilder();

        switch (re.op) {
            case LITERAL:
                if (re.runes.length > 1) {
                    b.append("string ");
                    b.append("\"");
                    for (int r : re.runes) {
                        b.appendCodePoint(r);
                    }
                    b.append("\"");
                } else {
                    b.append("literal ");
                    b.append("'");
                    b.append(Utils.runeToString(re.runes[0]));
                    b.append("'");
                }

                break;
            case ANY_CHAR:
                b.append("any characters including \"\\n\"");
                break;
            case ANY_CHAR_NOT_NL:
                b.append("any characters excluding \"\\n\"");
                break;
            case CONCAT:
                b.append("sequence [").append(joinComments(re.subs)).append("]");
                break;
            case ALTERNATE:
                b.append("alternation of [").append(joinComments(re.subs)).append("]");
                break;
            case VERTICAL_BAR:
                b.append("alternation");
                break;
            case BEGIN_LINE:
                b.append("line start");
                break;
            case CHAR_CLASS:
                if (re.HasJoinTrack()) {
                    b.append("alternation of [").append(joinComments(re.subs)).append("]");
                    break;
                }

                b.append("character class");
                break;
            case CAPTURE:
                b.append("capturing group (").append(joinComments(re.subs)).append(")");
                break;
            case LEFT_PAREN:
                b.append("capturing group");
                break;
            case STAR:
            case PLUS:
            case QUEST:
                b.append("quantifier");
                break;
        }

        Comments = b.toString();
    }

    private String joinComments(Regexp[] subs) {
        StringBuilder value = new StringBuilder();
        for (Regexp sub : subs) {
            if (value.length() > 0) {
                value.append(",");
            }

            // We need the topmost track here
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
}
