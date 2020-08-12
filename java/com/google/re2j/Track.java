package com.google.re2j;

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

    Track(int start, int end, String comments) {
        Start = start;
        End = end;
        Comments = comments;
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
                    b.appendCodePoint(re.runes[0]);
                    b.append("'");
                }

                break;
        }

        return b.toString();
    }
}
