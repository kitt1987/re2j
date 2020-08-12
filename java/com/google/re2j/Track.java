package com.google.re2j;

import static com.google.re2j.RE2.FOLD_CASE;

public class Track {
    public int Start;
    public int End;
    public String Comments;

    private int flag;
    private boolean frozen;

    Track() {
    }

    Track(int start) {
        Start = start;
    }

    void Freeze(int end, int flag) {
        // √ also calculate Coments
        assureUnfrozen();
        frozen = true;
        End = end;
        this.flag = flag;
    }

    void Freeze(int end, Regexp regexp) {
        // √ also calculate Coments
        assureUnfrozen();
        frozen = true;
        End = end;
    }

    private void assureUnfrozen() {
        if (frozen) {
            throw new IllegalStateException("Track is already frozen");
        }
    }

    private String parseInfo(Regexp re) {
        StringBuilder b = new StringBuilder();

        switch (re.op) {
            case LITERAL:
                if (re.runes.length > 1) {
                    b.append("string");
                } else {
                    b.append("literal");
                }

                if ((re.flags & FOLD_CASE) != 0) {
                    for (int r : re.runes) {
                        if (Unicode.simpleFold(r) != r) {
                            b.append("case insensitive");
                            break;
                        }
                    }
                }
                break;
        }

        return b.toString();
    }
}
