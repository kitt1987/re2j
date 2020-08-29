package com.google.re2j;

import java.util.ArrayList;
import java.util.HashMap;

public class Track implements Comparable<Track>  {
    static final HashMap<String, String> POSIX_GROUPS = new HashMap<String, String>();

    static {
        POSIX_GROUPS.put("[:alnum:]", "alphanumeric characters");
        POSIX_GROUPS.put("[:^alnum:]", "negated alphanumeric characters");
        POSIX_GROUPS.put("[:alpha:]", "alphabetic characters");
        POSIX_GROUPS.put("[:^alpha:]", "negated alphabetic characters");
        POSIX_GROUPS.put("[:ascii:]", "ASCII characters");
        POSIX_GROUPS.put("[:^ascii:]", "negated ASCII characters");
        POSIX_GROUPS.put("[:blank:]", "space and tab");
        POSIX_GROUPS.put("[:^blank:]", "negated space and tab");
        POSIX_GROUPS.put("[:cntrl:]", "control characters");
        POSIX_GROUPS.put("[:^cntrl:]", "negated control characters");
        POSIX_GROUPS.put("[:digit:]", "digits");
        POSIX_GROUPS.put("[:^digit:]", "negated digits");
        POSIX_GROUPS.put("[:graph:]", "visible characters");
        POSIX_GROUPS.put("[:^graph:]", "negated visible characters");
        POSIX_GROUPS.put("[:lower:]", "lowercase letters");
        POSIX_GROUPS.put("[:^lower:]", "negated lowercase letters");
        POSIX_GROUPS.put("[:print:]", "visible characters and spaces");
        POSIX_GROUPS.put("[:^print:]", "negated visible characters and spaces");
        POSIX_GROUPS.put("[:punct:]", "punctuation");
        POSIX_GROUPS.put("[:^punct:]", "negated punctuation");
        POSIX_GROUPS.put("[:space:]", "whitespace characters, including line breaks");
        POSIX_GROUPS.put("[:^space:]", "negated whitespace characters, including line breaks");
        POSIX_GROUPS.put("[:upper:]", "uppercase letters");
        POSIX_GROUPS.put("[:^upper:]", "negated uppercase letters");
        POSIX_GROUPS.put("[:word:]", "word characters");
        POSIX_GROUPS.put("[:^word:]", "negated word characters");
        POSIX_GROUPS.put("[:xdigit:]", "hexadecimal digits");
        POSIX_GROUPS.put("[:^xdigit:]", "negated hexadecimal digits");
    }

    static final HashMap<String, String> PERL_GROUPS = new HashMap<String, String>();
    static {
        PERL_GROUPS.put("\\d", "digits shorthand");
        PERL_GROUPS.put("\\D", "non-digits shorthand");
        PERL_GROUPS.put("\\s", "whitespace shorthand");
        PERL_GROUPS.put("\\S", "non-whitespace shorthand");
        PERL_GROUPS.put("\\w", "word character shorthand");
        PERL_GROUPS.put("\\W", "non-word character shorthand");
    }

    static final HashMap<Regexp.Op, String> OpKeyRuneMap = new HashMap<Regexp.Op, String>();
    static {
        OpKeyRuneMap.put(Regexp.Op.CHAR_CLASS, "[");
        OpKeyRuneMap.put(Regexp.Op.ANY_CHAR_NOT_NL, ".");
        OpKeyRuneMap.put(Regexp.Op.ANY_CHAR, ".:s");
        OpKeyRuneMap.put(Regexp.Op.BEGIN_LINE, "^:m");
        OpKeyRuneMap.put(Regexp.Op.END_LINE, "$:m");
        OpKeyRuneMap.put(Regexp.Op.BEGIN_TEXT, "^");
        OpKeyRuneMap.put(Regexp.Op.END_TEXT, "$");
        OpKeyRuneMap.put(Regexp.Op.WORD_BOUNDARY, "\\b");
        OpKeyRuneMap.put(Regexp.Op.NO_WORD_BOUNDARY, "\\B");
        OpKeyRuneMap.put(Regexp.Op.CAPTURE, "(");
        OpKeyRuneMap.put(Regexp.Op.STAR, "repeated zero or many times");
        OpKeyRuneMap.put(Regexp.Op.PLUS, "repeated once or many times");
        OpKeyRuneMap.put(Regexp.Op.QUEST, "repeated zero or once");
        OpKeyRuneMap.put(Regexp.Op.REPEAT, "{");
        OpKeyRuneMap.put(Regexp.Op.CONCAT, "sequence");
        OpKeyRuneMap.put(Regexp.Op.ALTERNATE, "alternation");
        OpKeyRuneMap.put(Regexp.Op.CAPTURE, "capturing group");
    }

    static final HashMap<String, String> CommentMap = new HashMap<String, String>();
    static {
        CommentMap.put(".:s", "any characters including \"\\n\"");
        CommentMap.put(".", "any characters excluding \"\\n\"");
        CommentMap.put("^:m", "line start");
        CommentMap.put("$:m", "line end");
        CommentMap.put("[:alnum:]", "alphanumeric characters");
        CommentMap.put("[:^alnum:]", "negated alphanumeric characters");
        CommentMap.put("[:alpha:]", "alphabetic characters");
        CommentMap.put("[:^alpha:]", "negated alphabetic characters");
        CommentMap.put("[:ascii:]", "ASCII characters");
        CommentMap.put("[:^ascii:]", "negated ASCII characters");
        CommentMap.put("[:blank:]", "space and tab");
        CommentMap.put("[:^blank:]", "negated space and tab");
        CommentMap.put("[:cntrl:]", "control characters");
        CommentMap.put("[:^cntrl:]", "negated control characters");
        CommentMap.put("[:digit:]", "digits");
        CommentMap.put("[:^digit:]", "negated digits");
        CommentMap.put("[:graph:]", "visible characters");
        CommentMap.put("[:^graph:]", "negated visible characters");
        CommentMap.put("[:lower:]", "lowercase letters");
        CommentMap.put("[:^lower:]", "negated lowercase letters");
        CommentMap.put("[:print:]", "visible characters and spaces");
        CommentMap.put("[:^print:]", "negated visible characters and spaces");
        CommentMap.put("[:punct:]", "punctuation");
        CommentMap.put("[:^punct:]", "negated punctuation");
        CommentMap.put("[:space:]", "whitespace characters, including line breaks");
        CommentMap.put("[:^space:]", "negated whitespace characters, including line breaks");
        CommentMap.put("[:upper:]", "uppercase letters");
        CommentMap.put("[:^upper:]", "negated uppercase letters");
        CommentMap.put("[:word:]", "word characters");
        CommentMap.put("[:^word:]", "negated word characters");
        CommentMap.put("[:xdigit:]", "hexadecimal digits");
        CommentMap.put("[:^xdigit:]", "negated hexadecimal digits");
        CommentMap.put("\\d", "digits shorthand");
        CommentMap.put("\\D", "non-digits shorthand");
        CommentMap.put("\\s", "whitespace shorthand");
        CommentMap.put("\\S", "non-whitespace shorthand");
        CommentMap.put("\\w", "word character shorthand");
        CommentMap.put("\\W", "non-word character shorthand");
        CommentMap.put(":", "mod modifier end");
        CommentMap.put(")", "capturing group end");
        CommentMap.put("(", "capturing group");
        CommentMap.put("(?", "non-capturing group");
        CommentMap.put("*", "quantifier: repeated zero or many times");
        CommentMap.put("+", "quantifier: repeated once or many times");
        CommentMap.put("?", "quantifier: repeated zero or once");
        CommentMap.put("|", "alternation");
        CommentMap.put(":i", "case insensitive");
        CommentMap.put(":m", "multi-line: '^' and '$' match at the start and end of each line");
        CommentMap.put(":s", "single-line: dot also matches line breaks");
        CommentMap.put(":U", "ungreedy quantifiers");
        CommentMap.put(":-", "negative modifier");
        CommentMap.put("[", "character class");
        CommentMap.put("]", "character class end");
    }

    static Track NewPlaceholder(ArrayList<Track> tracks) {
        int[] range = getTrackRange(tracks);
        return new Track(range[0], range[1], true);
    }

    static Track NewTopmost(ArrayList<Track> tracks, Regexp re) {
        if (tracks.size() <= 1) {
            return tracks.get(0);
        }

        int[] range = getTrackRange(tracks);
        Track topmost = new Track(range[0], range[1], false);
        topmost.Freeze(tracks, re);
        return topmost;
    }

    private static int[] getTrackRange(ArrayList<Track> tracks) {
        int start = Integer.MAX_VALUE, end = 0;
        for (Track track : tracks) {
            if (track.Start < start) {
                start = track.Start;
            }

            if (track.End > end) {
                end = track.End;
            }
        }

        if (start >= end) {
            throw new IllegalStateException("illegal range");
        }

        return new int[]{start, end};
    }

    public int Start = Integer.MAX_VALUE;
    public int End = Integer.MIN_VALUE;
    public String Comments;

    private String text;

    private boolean placeholder;

    Track(int start) {
        Start = start;
    }

    Track(int start, int end, boolean placeholder) {
        Start = start;
        End = end;
        this.placeholder = placeholder;
    }

    void Freeze(ArrayList<Track> tracks, Regexp re) {
        Comments = getComments(tracks, re);
        placeholder = false;
    }

    void Freeze(int end, String text) {
        if (text.isEmpty()) {
            throw new IllegalStateException("Can't freeze a empty track");
        }

        if (text.length() != end - Start) {
            throw new IllegalStateException("text '"+ text +"' doesn't match the position range[" + Start + "," + end + "]");
        }

        End = end;
        this.text = text;
        this.Comments = CommentMap.get(text);
        if (this.Comments == null) {
            if (text.length() > 1) {
                this.Comments = "string \"" + text + "\"";
            } else {
                this.Comments = "literal '" + text + "'";
            }
        }
    }

    void UpdateRange(int start, int end) {
        if (start >= end) {
            throw new IllegalStateException("illegal range");
        }

        Start = start;
        End = end;
        placeholder = true;
    }

    Track(String text) {
        Start = 0;
        Freeze(text.length(), text);
    }

    Track(int start, int end, String comments) {
        Start = start;
        End = end;
        Comments = comments;
    }

    boolean IsPlaceholder() {
        return placeholder;
    }

    private String getComments(ArrayList<Track> tracks, Regexp re) {
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
            case CONCAT:
                // FIXME comments must be in order
                b.append("sequence [").append(joinComments(tracks)).append("]");
                break;
            case ALTERNATE:
                b.append("alternation of [").append(joinComments(tracks)).append("]");
                break;
            case CHAR_CLASS:
                // FIXME converted form alternation
//                if (re.HasJoinTrack()) {
//                    b.append("alternation of [").append(joinComments(re.GetDirectTracks())).append("]");
//                    break;
//                }

                b.append("character class of [").append(joinComments(tracks)).append("]");
                break;
            case CAPTURE:
                b.append("capturing group [").append(joinComments(tracks)).append("]");
                break;
            case STAR:
            case PLUS:
            case QUEST:
                b.append(joinComments(tracks)).append(" ").append(OpKeyRuneMap.get(re.op));
                if ((re.flags & RE2.NON_GREEDY) != 0) {
                    b.append("(non-greedy)");
                }
                break;
            case REPEAT:
                b.append(joinComments(tracks)).append(" ").append(GenRepeatedRangeComments(re.min, re.max));
                if ((re.flags & RE2.NON_GREEDY) != 0) {
                    b.append("(non-greedy)");
                }
                break;
            default:
                throw new IllegalStateException("unsupported composed regexp " + re.op);
        }

        // FIXME considering flags here

        return b.toString();
    }

    private static String numberToFrequency(int num) {
        switch (num) {
            case 1:
                return "once";
            case 2:
                return "twice";
            default:
                return num + " times";
        }
    }

    public static String GenRepeatedRangeComments(int min, int max) {
        if (max == min) {
            return "repeated " + numberToFrequency(min);
        }

        if (min == -1) {
            return "repeated at most " + numberToFrequency(max);
        }

        if (max == -1) {
            return "repeated at least " + numberToFrequency(min);
        }

        return "repeated " + numberToFrequency(min) + " to " + numberToFrequency(max);
    }

    private String joinComments(ArrayList<Track> tracks) {
        StringBuilder value = new StringBuilder();

        if (tracks != null) {
            for (int i = 1; i < tracks.size(); i++) {
                String comments = tracks.get(i).Comments;
                if (comments.equals("character class")
                        || comments.equals("character class end")
                        || comments.equals("non-capturing group")
                        || comments.equals("capturing group end")
                        || comments.equals("mod modifier end")) {
                    continue;
                }

                if (value.length() > 0) {
                    value.append(",");
                }

                value.append(comments);
            }
        }

        return value.toString();
    }

    @Override
    public int compareTo(Track o) {
        if (Start != o.Start) {
            return Integer.compare(Start, o.Start);
        }

        return Integer.compare(o.End - o.Start, End - Start);
    }
}