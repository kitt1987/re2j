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
        OpKeyRuneMap.put(Regexp.Op.LITERAL, "rune");
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
        OpKeyRuneMap.put(Regexp.Op.STAR, "*");
        OpKeyRuneMap.put(Regexp.Op.PLUS, "+");
        OpKeyRuneMap.put(Regexp.Op.QUEST, "?");
        OpKeyRuneMap.put(Regexp.Op.REPEAT, "{");
        OpKeyRuneMap.put(Regexp.Op.CONCAT, "");
        OpKeyRuneMap.put(Regexp.Op.ALTERNATE, "|");
        OpKeyRuneMap.put(Regexp.Op.LEFT_PAREN, "(");
        OpKeyRuneMap.put(Regexp.Op.VERTICAL_BAR, "|");
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

        return new int[]{start, end};
    }

    static Track JoinTracks(ArrayList<Track> tracks) {
        if (tracks.isEmpty()) {
            return null;
        }

        int[] range = getTrackRange(tracks);
        // FIXME build topmost
        StringBuilder b = new StringBuilder();
        for (Track track : tracks) {
            b.append(track.Comments).append(",");
        }

        return new Track(range[0], range[1], b.toString());
    }

    public int Start;
    public int End;
    public String Comments;

    private String text;

    private boolean group;

    Track(int start) {
        Start = start;
    }

    void Freeze(int end, String text) {
        if (text.length() != end - Start) {
            throw new IllegalStateException("text '"+ text +"' doesn't match the position range[" + Start + "," + end + "]");
        }

        End = end;
        this.text = text;
        String comment = CommentMap.get(text);
        if (comment != null) {
            this.Comments = comment;
        } else {
            if (text.length() > 1) {
                this.Comments = "string \"" + text + "\"";
            } else {
                this.Comments = "literal '" + text + "'";
            }
        }
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

    boolean IsGroup() {
        return group;
    }

    void Update(Track that) {
        Start = that.Start;
        End = that.End;
        if (that.Comments != null && that.Comments.length() > 0) {
            Comments = that.Comments;
        }
    }

    void End(int end) {
        End = end;
    }

    void End(int end, String comments) {
        End(end, comments, false);
    }

    void End(int end, String comments, boolean group) {
        End = end;
        Comments = comments;
        this.group = group;
    }

    void End(int end, int rune) {
        End(end, rune, false);
    }

    void End(int end, int rune, boolean group) {
        End = end;
        UpdateComments(rune);
        this.group = group;
    }

    void UpdateComments(int rune) {
        switch (rune) {
            case ':':
                Comments = "mod modifier end";
                break;
            case ')':
                Comments = "capturing group end";
                break;
            case '(':
                Comments = "capturing group";
                break;
            case '*':
                Comments = "quantifier: repeated zero or many times";
                break;
            case '+':
                Comments = "quantifier: repeated once or many times";
                break;
            case '?':
                Comments = "quantifier: repeated zero or once";
                break;
            case '|':
                Comments = "alternation";
                break;
            default:
                Comments = "literal '" + Utils.runeToString(rune) + "'";
        }
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
