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

    static boolean AllLiterals(ArrayList<Track> tracks) {
        // FIXME optimize
        for (Track track : tracks) {
            if (track.Comments.startsWith("literal") || track.Comments.startsWith("string")) {
                continue;
            }

            return false;
        }

        return true;
    }

    public int Start;
    public int End;
    public String Comments;

    Track() {
        Start = Integer.MAX_VALUE;
        End = 0;
    }

    Track(int start, int end, String comments) {
        Start = start;
        End = end;
        Comments = comments;
    }

    Track(int start) {
        Start = start;
    }

    Track(int start, int end, Regexp re) {
        Start = start;
        End = end;
        UpdateComments(re);
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
            default:
                Comments = "literal '" + Utils.runeToString(rune) + "'";
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
                // FIXME comments must be in order
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
            case END_LINE:
                b.append("line end");
                break;
            case CHAR_CLASS:
                // a. converted form alternation
                if (re.HasJoinTrack()) {
                    b.append("alternation of [").append(joinComments(re.GetDirectTracks())).append("]");
                    break;
                }

                b.append("character class of [").append(joinComments(re.GetDirectTracks())).append("]");
                break;
            case CAPTURE:
                b.append("capturing group (").append(joinComments(re.subs)).append(")");
                break;
            case LEFT_PAREN:
                b.append("capturing group");
                break;
            case STAR:
                b.append(joinComments(re.subs)).append(" repeated zero or many times");
                if ((re.flags & RE2.NON_GREEDY) != 0) {
                    b.append("(non-greedy)");
                }
                break;
            case PLUS:
                b.append(joinComments(re.subs)).append(" repeated once or many times");
                if ((re.flags & RE2.NON_GREEDY) != 0) {
                    b.append("(non-greedy)");
                }
                break;
            case QUEST:
                b.append(joinComments(re.subs)).append(" repeated zero or once");
                if ((re.flags & RE2.NON_GREEDY) != 0) {
                    b.append("(non-greedy)");
                }
                break;
            case REPEAT:
                b.append(joinComments(re.subs)).append(" ").append(GenRepeatedRangeComments(re.min, re.max));
                if ((re.flags & RE2.NON_GREEDY) != 0) {
                    b.append("(non-greedy)");
                }
                break;
        }

        if (b.length() > 0) {
            Comments = b.toString();
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
                    || comments.equals("capturing group end")) {
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

    private String joinComments(Regexp[] subs) {
        if (subs == null) {
            return "";
        }

        Track emptyTrack = null;
        StringBuilder value = new StringBuilder();
        for (Regexp sub : subs) {
            if (sub.op == Regexp.Op.EMPTY_MATCH) {
                emptyTrack = sub.GetTopmostTrack();
                continue;
            }

            if (value.length() > 0) {
                value.append(",");
            }

            // We need the topmost track here
            value.append(sub.GetTopmostTrack().Comments);
        }

        if (value.length() > 0 && emptyTrack != null) {
            value.append(",").append(emptyTrack.Comments);
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
