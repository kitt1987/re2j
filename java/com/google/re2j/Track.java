package com.google.re2j;

import java.util.ArrayList;
import java.util.HashMap;

public class Track {
    static final HashMap<String, String> POSIX_GROUPS = new HashMap<String, String>();

    static {
        POSIX_GROUPS.put("[:alnum:]", new CharGroup(+1, code4));
        POSIX_GROUPS.put("[:^alnum:]", new CharGroup(-1, code4));
        POSIX_GROUPS.put("[:alpha:]", new CharGroup(+1, code5));
        POSIX_GROUPS.put("[:^alpha:]", new CharGroup(-1, code5));
        POSIX_GROUPS.put("[:ascii:]", new CharGroup(+1, code6));
        POSIX_GROUPS.put("[:^ascii:]", new CharGroup(-1, code6));
        POSIX_GROUPS.put("[:blank:]", new CharGroup(+1, code7));
        POSIX_GROUPS.put("[:^blank:]", new CharGroup(-1, code7));
        POSIX_GROUPS.put("[:cntrl:]", new CharGroup(+1, code8));
        POSIX_GROUPS.put("[:^cntrl:]", new CharGroup(-1, code8));
        POSIX_GROUPS.put("[:digit:]", new CharGroup(+1, code9));
        POSIX_GROUPS.put("[:^digit:]", new CharGroup(-1, code9));
        POSIX_GROUPS.put("[:graph:]", new CharGroup(+1, code10));
        POSIX_GROUPS.put("[:^graph:]", new CharGroup(-1, code10));
        POSIX_GROUPS.put("[:lower:]", new CharGroup(+1, code11));
        POSIX_GROUPS.put("[:^lower:]", new CharGroup(-1, code11));
        POSIX_GROUPS.put("[:print:]", new CharGroup(+1, code12));
        POSIX_GROUPS.put("[:^print:]", new CharGroup(-1, code12));
        POSIX_GROUPS.put("[:punct:]", new CharGroup(+1, code13));
        POSIX_GROUPS.put("[:^punct:]", new CharGroup(-1, code13));
        POSIX_GROUPS.put("[:space:]", new CharGroup(+1, code14));
        POSIX_GROUPS.put("[:^space:]", new CharGroup(-1, code14));
        POSIX_GROUPS.put("[:upper:]", new CharGroup(+1, code15));
        POSIX_GROUPS.put("[:^upper:]", new CharGroup(-1, code15));
        POSIX_GROUPS.put("[:word:]", new CharGroup(+1, code16));
        POSIX_GROUPS.put("[:^word:]", new CharGroup(-1, code16));
        POSIX_GROUPS.put("[:xdigit:]", new CharGroup(+1, code17));
        POSIX_GROUPS.put("[:^xdigit:]", new CharGroup(-1, code17));
    }

    static final HashMap<String, String> PERL_GROUPS = new HashMap<String, String>();
    static {
        PERL_GROUPS.put("\\d", new CharGroup(+1, code1));
        PERL_GROUPS.put("\\D", new CharGroup(-1, code1));
        PERL_GROUPS.put("\\s", new CharGroup(+1, code2));
        PERL_GROUPS.put("\\S", new CharGroup(-1, code2));
        PERL_GROUPS.put("\\w", new CharGroup(+1, code3));
        PERL_GROUPS.put("\\W", new CharGroup(-1, code3));
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

    Track(int start, int end, Regexp re) {
        Start = start;
        End = end;
        UpdateComments(re);
    }

    void End(int end) {
        End = end;
    }

    void End(int end, String comments) {
        End = end;
        Comments = comments;
    }

    void End(int end, int rune) {
        End = end;
        UpdateComments(rune);
    }

    void UpdateComments(int rune) {
        switch (rune) {
            case ':':
                Comments = "mod modifier end";
                break;
            case ')':
                Comments = "group end";
                break;
        }
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
            value.append(sub.GetTopmostTrack().Comments);
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
