package com.google.re2j;

import java.util.ArrayList;
import java.util.HashMap;

public class Track {
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
        PERL_GROUPS.put("\\d", "Digits shorthand");
        PERL_GROUPS.put("\\D", "non-digits shorthand");
        PERL_GROUPS.put("\\s", "whitespace shorthand");
        PERL_GROUPS.put("\\S", "non-whitespace shorthand");
        PERL_GROUPS.put("\\w", "word character shorthand");
        PERL_GROUPS.put("\\W", "non-word character shorthand");
    }

    public int Start;
    public int End;
    public String Comments;

    Track() {
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
            case '(':
                Comments = "capturing group";
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
