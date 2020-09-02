package com.google.re2j;

import java.util.*;

public class Track implements Comparable<Track>  {
    static final String EscapeText = "escape";
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
        OpKeyRuneMap.put(Regexp.Op.STAR, "repeated zero or many times");
        OpKeyRuneMap.put(Regexp.Op.PLUS, "repeated once or many times");
        OpKeyRuneMap.put(Regexp.Op.QUEST, "repeated zero or once");
        OpKeyRuneMap.put(Regexp.Op.REPEAT, "{");
        OpKeyRuneMap.put(Regexp.Op.CONCAT, "sequence");
        OpKeyRuneMap.put(Regexp.Op.ALTERNATE, "alternation");
        OpKeyRuneMap.put(Regexp.Op.CAPTURE, "capturing group");
        OpKeyRuneMap.put(Regexp.Op.CHAR_CLASS, "character class");
    }

    static final HashMap<String, String> CommentMap = new HashMap<String, String>();
    static {
        CommentMap.put(".:s", "any characters including \"\\n\"");
        CommentMap.put(".", "any characters excluding \"\\n\"");
        CommentMap.put("^:m", "line start");
        CommentMap.put("$:m", "line end");
        CommentMap.put("^", "line start");
        CommentMap.put("$", "line end");
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
        CommentMap.put("\\b", "word boundary");
        CommentMap.put("\\B", "non-word boundary");
        CommentMap.put(":", "mod modifier end");
        CommentMap.put(")", "capturing group end");
        CommentMap.put("(", "capturing group");
        CommentMap.put("(?", "non-capturing group start");
        CommentMap.put("*", "quantifier: repeated zero or many times");
        CommentMap.put("*?", "quantifier: repeated zero or many times(non-greedy)");
        CommentMap.put("+", "quantifier: repeated once or many times");
        CommentMap.put("+?", "quantifier: repeated once or many times(non-greedy)");
        CommentMap.put("?", "quantifier: repeated zero or once");
        CommentMap.put("??", "quantifier: repeated zero or once(non-greedy)");
        CommentMap.put("|", "alternation");
        CommentMap.put(":i", "case insensitive");
        CommentMap.put(":m", "multi-line: '^' and '$' match at the start and end of each line");
        CommentMap.put(":s", "single-line: dot also matches line breaks");
        CommentMap.put(":U", "ungreedy quantifiers");
        CommentMap.put(":-", "negative modifier");
        CommentMap.put("[", "character class");
        CommentMap.put("[^", "negated character class");
        CommentMap.put("]", "character class end");
        CommentMap.put("\\", EscapeText);
    }

    static Track NewComposedTrack(ArrayList<Track> tracks, Regexp re) {
        if (tracks.size() <= 1) {
            throw new IllegalStateException("need to compose at least 2 tracks");
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
    private boolean omitInComposed;
    private boolean placeholder;
    private boolean negated;
    private boolean posix;

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

    void FreezePlainText(int end, String text) {
        if (text.isEmpty()) {
            throw new IllegalStateException("Can't freeze a empty track");
        }

        End = end;
        this.text = text;
        if (text.length() > 1) {
            this.Comments = text;
        } else {
            this.Comments = "literal '" + text + "'";
        }
    }

    void FreezeRepetition(int end, Regexp re) {
        if (re.op != Regexp.Op.REPEAT) {
            throw new IllegalStateException("only repeat is supported but " + re.op);
        }

        End = end;
        omitInComposed = true;
        Comments = "quantifier: " + GenRepeatedRangeComments(re.min, re.max);
        if ((re.flags & RE2.NON_GREEDY) != 0) {
            Comments += "(non-greedy)";
        }
    }

    void Freeze(int end, String text) {
        if (text.isEmpty()) {
            throw new IllegalStateException("Can't freeze a empty track");
        }

        End = end;
        this.text = text;
        if (text.length() == 1) {
            switch (text.charAt(0)) {
                case '[':
                case ']':
                case ':':
                case '(':
                case ')':
                case '|':
                case '*':
                case '+':
                case '?':
                    omitInComposed = true;
            }
        } else if (text.length() == 2) {
            if (text.equals("(?")
                    || text.equals("*?")
                    || text.equals("+?")
                    || text.equals("??")) {
                omitInComposed = true;
            }

            if (text.equals("[^")) {
                negated = true;
                omitInComposed = true;
            }
        } else {
            if (text.equals(EscapeText)) {
                omitInComposed = true;
            }

            if (text.startsWith("[:")) {
                posix = true;
            }
        }

        Comments = CommentMap.get(text);
        if (Comments == null) {
            if (text.length() > 1) {
                Comments = "string \"" + text + "\"";
            } else {
                Comments = "literal '" + text + "'";
            }
        }

        if (posix) {
            // composed track reads comments from this.text
            this.text = Comments;
            Comments = "POSIX class:" + Comments;
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

    Track(int start, int end, String comments) {
        Start = start;
        End = end;
        Comments = comments;
    }

    boolean IsNothing() {
        return (text == null || text.isEmpty()) && (Comments == null || Comments.isEmpty());
    }
    boolean IsPlaceholder() {
        return placeholder;
    }

    private String getComments(ArrayList<Track> tracks, Regexp re) {
        StringBuilder b = new StringBuilder();
        Regexp.Op op = re.op;

        switch (op) {
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
            case CONCAT:
            case ALTERNATE:
            case CAPTURE:
            case CHAR_CLASS:
                if (op == Regexp.Op.CHAR_CLASS && re.Tracks.IsFromAlternation()) {
                    op = Regexp.Op.ALTERNATE;
                }
                // FIXME comments must be in order
                if (op == Regexp.Op.CHAR_CLASS && tracks.get(0).negated) {
                    b.append("negated ");
                }

                b.append(OpKeyRuneMap.get(op))
                        .append(" of [")
                        .append(joinComments(tracks, op == Regexp.Op.ALTERNATE))
                        .append("]");
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
            case LEFT_PAREN:
                // FIXME omit in composed tracks if no flags set
                omitInComposed = true;
                b.append("non-capturing group");
                break;
            case ANY_CHAR:
                b.append(CommentMap.get("."));
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

    private String joinComments(ArrayList<Track> tracks, boolean unique) {
        StringBuilder value = new StringBuilder();
        Set<String> uniqueMap = null;
        if (unique) {
            uniqueMap = new HashSet<String>(tracks.size());
        }

        if (tracks != null) {
            for (Track track : tracks) {
                if (track.omitInComposed) {
                    continue;
                }

                if (uniqueMap != null) {
                    if (uniqueMap.contains(track.Comments)) {
                        continue;
                    }

                    uniqueMap.add(track.Comments);
                }

                if (value.length() > 0) {
                    value.append(",");
                }

                value.append(track.Comments);
            }
        }

        return value.toString();
    }

    private String joinComments(ArrayList<Track> tracks) {
        StringBuilder value = new StringBuilder();

        if (tracks != null) {
            for (Track track : tracks) {
                if (track.omitInComposed) {
                    continue;
                }

                if (value.length() > 0) {
                    value.append(",");
                }

                if (track.posix) {
                    value.append(track.text);
                } else {
                    value.append(track.Comments);
                }
            }
        }

        return value.toString();
    }

    @Override
    public int compareTo(Track o) {
        if (Start != o.Start) {
            return Integer.compare(Start, o.Start);
        }

        int oRange = o.End - o.Start, range = End - Start;
        if (oRange > 1 || range > 1) {
            return Integer.compare(o.End - o.Start, End - Start);
        }

        return Integer.compare(End, o.End);
    }
}