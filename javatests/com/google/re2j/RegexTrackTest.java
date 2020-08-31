/*
 * Copyright (c) 2020 The Go Authors. All rights reserved.
 *
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
// Original Go source here:
// http://code.google.com/p/go/source/browse/src/pkg/regexp/syntax/parse_test.go

package com.google.re2j;

import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static com.google.re2j.RE2.*;
import static org.junit.Assert.*;

public class RegexTrackTest {

    private interface RunePredicate {
        boolean applies(int rune);
    }

    private static final RunePredicate IS_UPPER =
            new RunePredicate() {
                @Override
                public boolean applies(int r) {
                    return Unicode.isUpper(r);
                }
            };

    private static final RunePredicate IS_UPPER_FOLD =
            new RunePredicate() {
                @Override
                public boolean applies(int r) {
                    if (Unicode.isUpper(r)) {
                        return true;
                    }
                    for (int c = Unicode.simpleFold(r); c != r; c = Unicode.simpleFold(c)) {
                        if (Unicode.isUpper(c)) {
                            return true;
                        }
                    }
                    return false;
                }
            };

    private static final Map<Regexp.Op, String> OP_NAMES =
            new EnumMap<Regexp.Op, String>(Regexp.Op.class);

    static {
        OP_NAMES.put(Regexp.Op.NO_MATCH, "no");
        OP_NAMES.put(Regexp.Op.EMPTY_MATCH, "emp");
        OP_NAMES.put(Regexp.Op.LITERAL, "lit");
        OP_NAMES.put(Regexp.Op.CHAR_CLASS, "cc");
        OP_NAMES.put(Regexp.Op.ANY_CHAR_NOT_NL, "dnl");
        OP_NAMES.put(Regexp.Op.ANY_CHAR, "dot");
        OP_NAMES.put(Regexp.Op.BEGIN_LINE, "bol");
        OP_NAMES.put(Regexp.Op.END_LINE, "eol");
        OP_NAMES.put(Regexp.Op.BEGIN_TEXT, "bot");
        OP_NAMES.put(Regexp.Op.END_TEXT, "eot");
        OP_NAMES.put(Regexp.Op.WORD_BOUNDARY, "wb");
        OP_NAMES.put(Regexp.Op.NO_WORD_BOUNDARY, "nwb");
        OP_NAMES.put(Regexp.Op.CAPTURE, "cap");
        OP_NAMES.put(Regexp.Op.STAR, "star");
        OP_NAMES.put(Regexp.Op.PLUS, "plus");
        OP_NAMES.put(Regexp.Op.QUEST, "que");
        OP_NAMES.put(Regexp.Op.REPEAT, "rep");
        OP_NAMES.put(Regexp.Op.CONCAT, "cat");
        OP_NAMES.put(Regexp.Op.ALTERNATE, "alt");
    }

    private static final int TEST_FLAGS = MATCH_NL | PERL_X | UNICODE_GROUPS;

    private static final Map<String, Track[]> PARSE_TESTS = new HashMap<String, Track[]>() {{
        put("a", new Track[]{
                new Track(0, 1, "literal 'a'"),
        });
        put("a.", new Track[]{
                new Track(0, 2, "sequence [literal 'a',any characters including \"\\n\"]"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "any characters including \"\\n\""),
        });
        put("a.b", new Track[]{
                new Track(0, 3, "sequence [literal 'a',any characters including \"\\n\",literal 'b']"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "any characters including \"\\n\""),
                new Track(2, 3, "literal 'b'"),
        });
        put("ab", new Track[]{
                new Track(0, 2, "string \"ab\""),
        });
        put("a.b.c", new Track[]{
                new Track(0, 5, "sequence [literal 'a',any characters including \"\\n\",literal 'b',any characters including \"\\n\",literal 'c']"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "any characters including \"\\n\""),
                new Track(2, 3, "literal 'b'"),
                new Track(3, 4, "any characters including \"\\n\""),
                new Track(4, 5, "literal 'c'"),
        });
        put("abc", new Track[]{
                new Track(0, 3, "string \"abc\""),
        });
        put("a|^", new Track[]{
                new Track(0, 3, "alternation of [literal 'a',line start]"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "alternation"),
                new Track(2, 3, "line start"),
        });
        put("a|b", new Track[]{
                new Track(0, 3, "alternation of [literal 'a',literal 'b']"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "alternation"),
                new Track(2, 3, "literal 'b'"),
        });
        put("(a)", new Track[]{
                new Track(0, 3, "capturing group (literal 'a')"),
                new Track(0, 1, "capturing group"),
                new Track(1, 2, "literal 'a'"),
                new Track(2, 3, "capturing group end"),
        });
        put("(a)|b", new Track[]{
                new Track(0, 5, "alternation of [capturing group (literal 'a'),literal 'b']"),
                new Track(0, 3, "capturing group (literal 'a')"),
                new Track(0, 1, "capturing group"),
                new Track(1, 2, "literal 'a'"),
                new Track(2, 3, "capturing group end"),
                new Track(3, 4, "alternation"),
                new Track(4, 5, "literal 'b'"),
        });
        put("a*", new Track[]{
                new Track(0, 2, "literal 'a' repeated zero or many times"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "quantifier: repeated zero or many times"),
        });
        put("a+", new Track[]{
                new Track(0, 2, "literal 'a' repeated once or many times"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "quantifier: repeated once or many times"),
        });
        put("a?", new Track[]{
                new Track(0, 2, "literal 'a' repeated zero or once"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "quantifier: repeated zero or once"),
        });
        put("a{2}", new Track[]{
                new Track(0, 4, "literal 'a' repeated twice"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 4, "quantifier: repeated twice"),
        });
        put("a{2,3}", new Track[]{
                new Track(0, 6, "literal 'a' repeated twice to 3 times"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 6, "quantifier: repeated twice to 3 times"),
        });
        put("a{2,}", new Track[]{
                new Track(0, 5, "literal 'a' repeated at least twice"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 5, "quantifier: repeated at least twice"),
        });
        put("a*?", new Track[]{
                new Track(0, 3, "literal 'a' repeated zero or many times(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "quantifier: repeated zero or many times"),
                new Track(2, 3, "quantifier: non-greedy"),
        });
        put("a+?", new Track[]{
                new Track(0, 3, "literal 'a' repeated once or many times(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "quantifier: repeated once or many times"),
                new Track(2, 3, "quantifier: non-greedy"),
        });
        put("a??", new Track[]{
                new Track(0, 3, "literal 'a' repeated zero or once(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 3, "quantifier: repeated zero or once(non-greedy)"),
        });
        put("a{2}?", new Track[]{
                new Track(0, 5, "literal 'a' repeated twice(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 4, "quantifier: repeated twice"),
                new Track(4, 5, "quantifier: non-greedy"),
        });
        put("a{2,3}?", new Track[]{
                new Track(0, 7, "literal 'a' repeated twice to 3 times(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 6, "quantifier: repeated twice to 3 times"),
                new Track(6, 7, "quantifier: non-greedy"),
        });
        put("a{2,}?", new Track[]{
                new Track(0, 6, "literal 'a' repeated at least twice(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 5, "quantifier: repeated at least twice"),
                new Track(5, 6, "quantifier: non-greedy"),
        });
        put("x{1001", new Track[]{
                new Track(0, 6, "string \"x{1001\""),
        });
        put("x{9876543210", new Track[]{
                new Track(0, 12, "string \"x{9876543210\""),
        });
        put("x{9876543210,", new Track[]{
                new Track(0, 13, "string \"x{9876543210,\""),
        });
        put("x{2,1,", new Track[]{
                new Track(0, 6, "string \"x{2,1,\""),
        });
        put("x{1,9876543210", new Track[]{
                new Track(0, 14, "string \"x{1,9876543210\""),
        });
//        put("", new Track[]{
//                new Track(0, 0, "empty"),
//        });
        put("|", new Track[]{
                // FIXME
                new Track(0, 1, "alternation of []"),
                new Track(0, 1, "alternation"),
        });
        put("|x|", new Track[]{
                new Track(0, 3, "alternation of [empty string,literal 'x']"),
                new Track(0, 1, "alternation"),
                new Track(1, 2, "literal 'x'"),
                new Track(2, 3, "alternation"),
        });
        put(".", new Track[]{
                // FIXME talk about the Unicode
                new Track(0, 1, "any characters including \"\\n\""),
        });
        put("^", new Track[]{
                new Track(0, 1, "line start"),
        });
        put("$", new Track[]{
                new Track(0, 1, "line end"),
        });
        put("\\|", new Track[]{
                new Track(0, 2, "literal '|'"),
        });
        put("\\(", new Track[]{
                new Track(0, 2, "literal '('"),
        });
        put("\\)", new Track[]{
                new Track(0, 2, "literal ')'"),
        });
        put("\\*", new Track[]{
                new Track(0, 2, "literal '*'"),
        });
        put("\\+", new Track[]{
                new Track(0, 2, "literal '+'"),
        });
        put("\\?", new Track[]{
                new Track(0, 2, "literal '?'"),
        });
        put("{", new Track[]{
                new Track(0, 1, "literal '{'"),
        });
        put("}", new Track[]{
                new Track(0, 1, "literal '}'"),
        });
        put("\\.", new Track[]{
                new Track(0, 2, "literal '.'"),
        });
        put("\\^", new Track[]{
                new Track(0, 2, "literal '^'"),
        });
        put("\\$", new Track[]{
                new Track(0, 2, "literal '$'"),
        });
        put("\\\\", new Track[]{
                new Track(0, 2, "literal '\\'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '\\'"),
        });
        put("[ace]", new Track[]{
                new Track(0, 5, "character class of [literal 'a',literal 'c',literal 'e']"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "literal 'a'"),
                new Track(2, 3, "literal 'c'"),
                new Track(3, 4, "literal 'e'"),
                new Track(4, 5, "character class end"),
        });
        put("[abc]", new Track[]{
                new Track(0, 5, "character class of [literal 'a',literal 'b',literal 'c']"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "literal 'a'"),
                new Track(2, 3, "literal 'b'"),
                new Track(3, 4, "literal 'c'"),
                new Track(4, 5, "character class end"),
        });
        put("[a-z]", new Track[]{
                new Track(0, 5, "character class of [range a to z]"),
                new Track(0, 1, "character class"),
                new Track(1, 4, "range a to z"),
                new Track(4, 5, "character class end"),
        });
        put("[a]", new Track[]{
                new Track(0, 3, "literal 'a'"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "literal 'a'"),
                new Track(2, 3, "character class end"),
        });
        put("\\-", new Track[]{
                new Track(0, 2, "literal '-'"),
        });
        put("-", new Track[]{
                new Track(0, 1, "literal '-'"),
        });
        put("\\_", new Track[]{
                new Track(0, 2, "literal '_'"),
        });
        put("abc|def", new Track[]{
                new Track(0, 7, "alternation of [string \"abc\",string \"def\"]"),
                new Track(0, 3, "string \"abc\""),
                new Track(3, 4, "alternation"),
                new Track(4, 7, "string \"def\""),
        });
        put("abc|def|ghi", new Track[]{
                new Track(0, 11, "alternation of [string \"abc\",string \"def\",string \"ghi\"]"),
                new Track(0, 3, "string \"abc\""),
                new Track(3, 4, "alternation"),
                new Track(4, 7, "string \"def\""),
                new Track(7, 8, "alternation"),
                new Track(8, 11, "string \"ghi\""),
        });
        put("[[:lower:]]", new Track[]{
                new Track(0, 11, "character class of [POSIX class lowercase letters]"),
                new Track(0, 1, "character class"),
                new Track(1, 10, "POSIX class lowercase letters"),
                new Track(10, 11, "character class end"),
        });
        put("[^[:lower:]]", new Track[]{
                new Track(0, 12, "character class of [negated,POSIX class lowercase letters]"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "negated"),
                new Track(2, 11, "POSIX class lowercase letters"),
                new Track(11, 12, "character class end"),
        });
        put("[[:^lower:]]", new Track[]{
                new Track(0, 12, "character class of [POSIX class negated lowercase letters]"),
                new Track(0, 1, "character class"),
                new Track(1, 11, "POSIX class negated lowercase letters"),
                new Track(11, 12, "character class end"),
        });
        put("(?i)[[:lower:]]", new Track[]{
                new Track(0, 15, "character class of [case insensitive,POSIX class lowercase letters]"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "character class"),
                new Track(5, 14, "POSIX class lowercase letters"),
                new Track(14, 15, "character class end"),
        });
        put("(?i)[a-z]", new Track[]{
                new Track(0, 9, "character class of [case insensitive,range a to z]"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "character class"),
                new Track(5, 8, "range a to z"),
                new Track(8, 9, "character class end"),
        });
        put("(?i)[^[:lower:]]", new Track[]{
                new Track(0, 16, "character class of [case insensitive,negated,POSIX class lowercase letters]"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "character class"),
                new Track(5, 6, "negated"),
                new Track(6, 15, "POSIX class lowercase letters"),
                new Track(15, 16, "character class end"),
        });
        put("(?i)[[:^lower:]]", new Track[]{
                new Track(0, 16, "character class of [case insensitive,POSIX class negated lowercase letters]"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "character class"),
                new Track(5, 15, "POSIX class negated lowercase letters"),
                new Track(15, 16, "character class end"),
        });
        put("\\d", new Track[]{
                new Track(0, 2, "character class digits shorthand"),
        });
        put("\\D", new Track[]{
                new Track(0, 2, "character class non-digits shorthand"),
        });
        put("\\s", new Track[]{
                new Track(0, 2, "character class whitespace shorthand"),
        });
        put("\\S", new Track[]{
                new Track(0, 2, "character class non-whitespace shorthand"),
        });
        put("\\w", new Track[]{
                new Track(0, 2, "character class word character shorthand"),
        });
        put("\\W", new Track[]{
                new Track(0, 2, "character class non-word character shorthand"),
        });
        put("(?i)\\w", new Track[]{
                // FIXME the topmost track
                new Track(0, 6, "character class of [case insensitive,character class word character shorthand]"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 6, "character class word character shorthand"),
        });
        put("(?i)\\W", new Track[]{
                // FIXME the topmost track
                new Track(0, 6, "character class of [case insensitive,character class non-word character shorthand]"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 6, "character class non-word character shorthand"),
        });
        put("[^\\\\]", new Track[]{
                // FIXME the topmost track
                new Track(0, 5, "character class of [negated,literal '\\']"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "negated"),
                new Track(2, 4, "literal '\\'"),
                new Track(4, 5, "character class end"),
        });
//    //  { "\\C", "byte{}" },  // probably never
//
//    // Unicode, negatives, and a double negative.
//    {"\\p{Braille}", "cc{0x2800-0x28ff}"},
//    {"\\P{Braille}", "cc{0x0-0x27ff 0x2900-0x10ffff}"},
//    {"\\p{^Braille}", "cc{0x0-0x27ff 0x2900-0x10ffff}"},
//    {"\\P{^Braille}", "cc{0x2800-0x28ff}"},
//    {"\\pZ", "cc{0x20 0xa0 0x1680 0x180e 0x2000-0x200a 0x2028-0x2029 0x202f 0x205f 0x3000}"},
//    {"[\\p{Braille}]", "cc{0x2800-0x28ff}"},
//    {"[\\P{Braille}]", "cc{0x0-0x27ff 0x2900-0x10ffff}"},
//    {"[\\p{^Braille}]", "cc{0x0-0x27ff 0x2900-0x10ffff}"},
//    {"[\\P{^Braille}]", "cc{0x2800-0x28ff}"},
//    {"[\\pZ]", "cc{0x20 0xa0 0x1680 0x180e 0x2000-0x200a 0x2028-0x2029 0x202f 0x205f 0x3000}"},
//    {"\\p{Lu}", mkCharClass(IS_UPPER)},
//    {"[\\p{Lu}]", mkCharClass(IS_UPPER)},
//    {"(?i)[\\p{Lu}]", mkCharClass(IS_UPPER_FOLD)},
//    {"\\p{Any}", "dot{}"},
//    {"\\p{^Any}", "cc{}"},
//
//    // Hex, octal.
//    {"[\\012-\\234]\\141", "cat{cc{0xa-0x9c}lit{a}}"},
//    {"[\\x{41}-\\x7a]\\x61", "cat{cc{0x41-0x7a}lit{a}}"},

        put("a{,2}", new Track[]{
                // FIXME the topmost track
                new Track(0, 5, "string \"a{,2}\""),
        });
        put("\\.\\^\\$\\\\", new Track[]{
                // FIXME the topmost track
                new Track(0, 8, "string \".^$\\\""),
        });
        put("[a-zABC]", new Track[]{
                // FIXME the topmost track
                new Track(0, 8, "character class of [range a to z,literal 'A',literal 'B',literal 'C']"),
                new Track(0, 1, "character class"),
                new Track(1, 4, "range a to z"),
                new Track(4, 5, "literal 'A'"),
                new Track(5, 6, "literal 'B'"),
                new Track(6, 7, "literal 'C'"),
                new Track(7, 8, "character class end"),
        });
        put("[^a]", new Track[]{
                // FIXME the topmost track
                new Track(0, 4, "character class of [negated,literal 'a']"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "negated"),
                new Track(2, 3, "literal 'a'"),
                new Track(3, 4, "character class end"),
        });
        put("[α-ε☺]", new Track[]{
                new Track(0, 6, "character class of [range α to ε,literal '☺']"),
                new Track(0, 1, "character class"),
                new Track(1, 4, "range α to ε"),
                new Track(4, 5, "literal '☺'"),
                new Track(5, 6, "character class end"),
        });
        put("a*{", new Track[]{
                new Track(0, 3, "sequence [literal 'a' repeated zero or many times,literal '{']"),
                new Track(0, 2, "literal 'a' repeated zero or many times"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "quantifier: repeated zero or many times"),
                new Track(2, 3, "literal '{'"),
        });

        put("(?:ab)*", new Track[]{
                new Track(0, 7, "group of string \"ab\" repeated zero or many times"),
                new Track(0, 6, "group of string \"ab\""),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 5, "string \"ab\""),
                new Track(5, 6, "capturing group end"),
                new Track(6, 7, "quantifier: repeated zero or many times"),
        });

        put("(ab)*", new Track[]{
                new Track(0, 5, "capturing group (string \"ab\") repeated zero or many times"),
                new Track(0, 4, "capturing group (string \"ab\")"),
                new Track(0, 1, "capturing group"),
                new Track(1, 3, "string \"ab\""),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "quantifier: repeated zero or many times"),
        });

        put("ab|cd", new Track[]{
                new Track(0, 5, "alternation of [string \"ab\",string \"cd\"]"),
                new Track(0, 2, "string \"ab\""),
                new Track(2, 3, "alternation"),
                new Track(3, 5, "string \"cd\""),
        });

        put("a(b|c)d", new Track[]{
                new Track(0, 7, "sequence [literal 'a',capturing group (alternation of [literal 'b',literal 'c']),literal 'd']"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 6, "capturing group (alternation of [literal 'b',literal 'c'])"),
                new Track(1, 2, "capturing group"),
                new Track(2, 5, "alternation of [literal 'b',literal 'c']"),
                new Track(2, 3, "literal 'b'"),
                new Track(3, 4, "alternation"),
                new Track(4, 5, "literal 'c'"),
                new Track(5, 6, "capturing group end"),
                new Track(6, 7, "literal 'd'"),
        });

        put("(?:a)", new Track[]{
                new Track(0, 5, "group of literal 'a'"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "literal 'a'"),
                new Track(4, 5, "capturing group end"),
        });

        put("(?:ab)(?:cd)", new Track[]{
                new Track(0, 12, "group of string \"abcd\""),

                new Track(0, 6, "group of string \"ab\""),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 5, "string \"ab\""),
                new Track(5, 6, "capturing group end"),

                new Track(6, 12, "group of string \"cd\""),
                new Track(6, 9, "non-capturing group"),
                new Track(6, 8, "non-capturing group"),
                new Track(8, 9, "mod modifier end"),
                new Track(9, 11, "string \"cd\""),
                new Track(11, 12, "capturing group end"),
        });

        put("(?:a+b+)(?:c+d+)", new Track[]{
                new Track(0, 16, "group of sequence [literal 'a' repeated once or many times,literal 'b' repeated once or many times,literal 'c' repeated once or many times,literal 'd' repeated once or many times]"),
                new Track(0, 8, "group of sequence [literal 'a' repeated once or many times,literal 'b' repeated once or many times]"),

                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 5, "literal 'a' repeated once or many times"),
                new Track(3, 4, "literal 'a'"),
                new Track(4, 5, "quantifier: repeated once or many times"),
                new Track(5, 7, "literal 'b' repeated once or many times"),
                new Track(5, 6, "literal 'b'"),
                new Track(6, 7, "quantifier: repeated once or many times"),
                new Track(7, 8, "capturing group end"),

                new Track(8, 16, "group of sequence [literal 'c' repeated once or many times,literal 'd' repeated once or many times]"),

                new Track(8, 11, "non-capturing group"),
                new Track(8, 10, "non-capturing group"),
                new Track(10, 11, "mod modifier end"),
                new Track(11, 13, "literal 'c' repeated once or many times"),
                new Track(11, 12, "literal 'c'"),
                new Track(12, 13, "quantifier: repeated once or many times"),
                new Track(13, 15, "literal 'd' repeated once or many times"),
                new Track(13, 14, "literal 'd'"),
                new Track(14, 15, "quantifier: repeated once or many times"),
                new Track(15, 16, "capturing group end"),
        });
        put("(?:a+|b+)|(?:c+|d+)", new Track[]{
                new Track(0, 19, "group of alternation of [literal 'a' repeated once or many times,literal 'b' repeated once or many times,literal 'c' repeated once or many times,literal 'd' repeated once or many times]"),
                new Track(0, 9, "group of alternation of [literal 'a' repeated once or many times,literal 'b' repeated once or many times]"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 5, "literal 'a' repeated once or many times"),
                new Track(3, 4, "literal 'a'"),
                new Track(4, 5, "quantifier: repeated once or many times"),
                new Track(5, 6, "alternation"),
                new Track(6, 8, "literal 'b' repeated once or many times"),
                new Track(6, 7, "literal 'b'"),
                new Track(7, 8, "quantifier: repeated once or many times"),
                new Track(8, 9, "capturing group end"),
                new Track(9, 10, "alternation"),
                new Track(10, 19, "group of alternation of [literal 'c' repeated once or many times,literal 'd' repeated once or many times]"),
                new Track(10, 13, "non-capturing group"),
                new Track(10, 12, "non-capturing group"),
                new Track(12, 13, "mod modifier end"),
                new Track(13, 15, "literal 'c' repeated once or many times"),
                new Track(13, 14, "literal 'c'"),
                new Track(14, 15, "quantifier: repeated once or many times"),
                new Track(15, 16, "alternation"),
                new Track(16, 18, "literal 'd' repeated once or many times"),
                new Track(16, 17, "literal 'd'"),
                new Track(17, 18, "quantifier: repeated once or many times"),
                new Track(18, 19, "capturing group end"),
        });
        put("(?:a|b)|(?:c|d)", new Track[]{
                // FIXME literals should not exist in the topmost track again
                new Track(0, 15, "group of alternation of [group of alternation of [literal 'a',literal 'b'],literal 'a',literal 'b',group of alternation of [literal 'c',literal 'd'],literal 'c',alternation,literal 'd']"),
                new Track(0, 7, "group of alternation of [literal 'a',literal 'b']"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "literal 'a'"),
                new Track(4, 5, "alternation"),
                new Track(5, 6, "literal 'b'"),
                new Track(6, 7, "capturing group end"),
                new Track(7, 8, "alternation"),
                new Track(8, 15, "group of alternation of [literal 'c',literal 'd']"),
                new Track(8, 11, "non-capturing group"),
                new Track(8, 10, "non-capturing group"),
                new Track(10, 11, "mod modifier end"),
                new Track(11, 12, "literal 'c'"),
                new Track(12, 13, "alternation"),
                new Track(13, 14, "literal 'd'"),
                new Track(14, 15, "capturing group end"),
        });

        put("a|.", new Track[]{
                new Track(0, 3, "any characters including \"\\n\""),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "alternation"),
                new Track(2, 3, "any characters including \"\\n\""),
        });

        put(".|a", new Track[]{
                new Track(0, 3, "any characters including \"\\n\""),
                new Track(0, 1, "any characters including \"\\n\""),
                new Track(1, 2, "alternation"),
                new Track(2, 3, "literal 'a'"),
        });

        put("(?:[abc]|A|Z|hello|world)", new Track[]{
                new Track(0, 25, "alternation of [character class of [literal 'a',literal 'b',literal 'c'],literal 'A',literal 'Z',string \"hello\",string \"world\"]"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 8, "character class of [literal 'a',literal 'b',literal 'c']"),
                new Track(3, 4, "character class"),
                new Track(4, 5, "literal 'a'"),
                new Track(5, 6, "literal 'b'"),
                new Track(6, 7, "literal 'c'"),
                new Track(7, 8, "character class end"),
                new Track(8, 9, "alternation"),
                new Track(9, 10, "literal 'A'"),
                new Track(10, 11, "alternation"),
                new Track(11, 12, "literal 'Z'"),
                new Track(12, 13, "alternation"),
                new Track(13, 18, "string \"hello\""),
                new Track(18, 19, "alternation"),
                new Track(19, 24, "string \"world\""),
                new Track(24, 25, "capturing group end"),
        });
    }};

//  {
//
//    // Test flattening.

//    {"(?:[abc]|A|Z|hello|world)", "alt{cc{0x41 0x5a 0x61-0x63}str{hello}str{world}}"},
//    {"(?:[abc]|A|Z)", "cc{0x41 0x5a 0x61-0x63}"},
//
//    // Test Perl quoted literals
//    {"\\Q+|*?{[\\E", "str{+|*?{[}"},
//    {"\\Q+\\E+", "plus{lit{+}}"},
//    {"\\Qab\\E+", "cat{lit{a}plus{lit{b}}}"},
//    {"\\Q\\\\E", "lit{\\}"},
//    {"\\Q\\\\\\E", "str{\\\\}"},
//
//    // Test Perl \A and \z
//    {"(?m)^", "bol{}"},
//    {"(?m)$", "eol{}"},
//    {"(?-m)^", "bot{}"},
//    {"(?-m)$", "eot{}"},
//    {"(?m)\\A", "bot{}"},
//    {"(?m)\\z", "eot{\\z}"},
//    {"(?-m)\\A", "bot{}"},
//    {"(?-m)\\z", "eot{\\z}"},
//
//    // Test named captures
//    {"(?P<name>a)", "cap{name:lit{a}}"},
//
//    // Case-folded literals
//    {"[Aa]", "litfold{A}"},
//    {"[\\x{100}\\x{101}]", "litfold{Ā}"},
//    {"[Δδ]", "litfold{Δ}"},
//
//    // Strings
//    {"abcde", "str{abcde}"},
//    {"[Aa][Bb]cd", "cat{strfold{AB}str{cd}}"},
//
//    // Factoring.
//    {
//      "abc|abd|aef|bcx|bcy",
//      "alt{cat{lit{a}alt{cat{lit{b}cc{0x63-0x64}}str{ef}}}cat{str{bc}cc{0x78-0x79}}}"
//    },
//    {
//      "ax+y|ax+z|ay+w",
//      "cat{lit{a}alt{cat{plus{lit{x}}lit{y}}cat{plus{lit{x}}lit{z}}cat{plus{lit{y}}lit{w}}}}"
//    },
//
//    // Bug fixes.
//
//    {"(?:.)", "dot{}"},
//    {"(?:x|(?:xa))", "cat{lit{x}alt{emp{}lit{a}}}"},
//    {"(?:.|(?:.a))", "cat{dot{}alt{emp{}lit{a}}}"},
//    {"(?:A(?:A|a))", "cat{lit{A}litfold{A}}"},
//    {"(?:A|a)", "litfold{A}"},
//    {"A|(?:A|a)", "litfold{A}"},
//    {"(?s).", "dot{}"},
//    {"(?-s).", "dnl{}"},
//    {"(?:(?:^).)", "cat{bol{}dot{}}"},
//    {"(?-s)(?:(?:^).)", "cat{bol{}dnl{}}"},
//    {"[\\x00-\\x{10FFFF}]", "dot{}"},
//    {"[^\\x00-\\x{10FFFF}]", "cc{}"},
//    {"(?:[a][a-])", "cat{lit{a}cc{0x2d 0x61}}"},
//
//    // RE2 prefix_tests
//    {"abc|abd", "cat{str{ab}cc{0x63-0x64}}"},
//    {"a(?:b)c|abd", "cat{str{ab}cc{0x63-0x64}}"},
//    {
//      "abc|abd|aef|bcx|bcy",
//      "alt{cat{lit{a}alt{cat{lit{b}cc{0x63-0x64}}str{ef}}}" + "cat{str{bc}cc{0x78-0x79}}}"
//    },
//    {"abc|x|abd", "alt{str{abc}lit{x}str{abd}}"},
//    {"(?i)abc|ABD", "cat{strfold{AB}cc{0x43-0x44 0x63-0x64}}"},
//    {"[ab]c|[ab]d", "cat{cc{0x61-0x62}cc{0x63-0x64}}"},
//    {".c|.d", "cat{dot{}cc{0x63-0x64}}"},
//    {"x{2}|x{2}[0-9]", "cat{rep{2,2 lit{x}}alt{emp{}cc{0x30-0x39}}}"},
//    {"x{2}y|x{2}[0-9]y", "cat{rep{2,2 lit{x}}alt{lit{y}cat{cc{0x30-0x39}lit{y}}}}"},
//    {"a.*?c|a.*?b", "cat{lit{a}alt{cat{nstar{dot{}}lit{c}}cat{nstar{dot{}}lit{b}}}}"},
//  };

    // TODO(adonovan): add some tests for:
    // - ending a regexp with "\\"
    // - Java UTF-16 things.

//  @Test
//  public void testParseSimple() {
//    testParseDump(PARSE_TESTS, TEST_FLAGS);
//  }
//
//  private static final String[][] FOLDCASE_TESTS = {
//    {"AbCdE", "strfold{ABCDE}"},
//    {"[Aa]", "litfold{A}"},
//    {"a", "litfold{A}"},
//
//    // 0x17F is an old English long s (looks like an f) and folds to s.
//    // 0x212A is the Kelvin symbol and folds to k.
//    {"A[F-g]", "cat{litfold{A}cc{0x41-0x7a 0x17f 0x212a}}"}, // [Aa][A-z...]
//    {"[[:upper:]]", "cc{0x41-0x5a 0x61-0x7a 0x17f 0x212a}"},
//    {"[[:lower:]]", "cc{0x41-0x5a 0x61-0x7a 0x17f 0x212a}"}
//  };
//
//  @Test
//  public void testParseFoldCase() {
//    testParseDump(FOLDCASE_TESTS, FOLD_CASE);
//  }
//
//  private static final String[][] LITERAL_TESTS = {
//    {"(|)^$.[*+?]{5,10},\\", "str{(|)^$.[*+?]{5,10},\\}"},
//  };
//
//  @Test
//  public void testParseLiteral() {
//    testParseDump(LITERAL_TESTS, LITERAL);
//  }
//
//  private static final String[][] MATCHNL_TESTS = {
//    {".", "dot{}"},
//    {"\n", "lit{\n}"},
//    {"[^a]", "cc{0x0-0x60 0x62-0x10ffff}"},
//    {"[a\\n]", "cc{0xa 0x61}"},
//  };
//
//  @Test
//  public void testParseMatchNL() {
//    testParseDump(MATCHNL_TESTS, MATCH_NL);
//  }
//
//  private static final String[][] NOMATCHNL_TESTS = {
//    {".", "dnl{}"},
//    {"\n", "lit{\n}"},
//    {"[^a]", "cc{0x0-0x9 0xb-0x60 0x62-0x10ffff}"},
//    {"[a\\n]", "cc{0xa 0x61}"},
//  };

//  @Test
//  public void testParseNoMatchNL() {
//    testParseDump(NOMATCHNL_TESTS, 0);
//  }

    // Test Parse -> Dump.
//  private void testParseDump(String[][] tests, int flags) {
//    for (String[] test : tests) {
//      try {
//        Regexp re = Parser.parse(test[0], flags);
//        String d = dump(re);
//        Truth.assertWithMessage("parse/dump of " + test[0]).that(d).isEqualTo(test[1]);
//      } catch (PatternSyntaxException e) {
//        throw new RuntimeException("Parsing failed: " + test[0], e);
//      }
//    }
//  }

    // dump prints a string representation of the regexp showing
    // the structure explicitly.
    private static String dump(Regexp re) {
        StringBuilder b = new StringBuilder();
        dumpRegexp(b, re);
        return b.toString();
    }

    // dumpRegexp writes an encoding of the syntax tree for the regexp |re|
    // to |b|.  It is used during testing to distinguish between parses that
    // might print the same using re's toString() method.
    private static void dumpRegexp(StringBuilder b, Regexp re) {
        String name = OP_NAMES.get(re.op);
        if (name == null) {
            b.append("op").append(re.op);
        } else {
            switch (re.op) {
                case STAR:
                case PLUS:
                case QUEST:
                case REPEAT:
                    if ((re.flags & NON_GREEDY) != 0) {
                        b.append('n');
                    }
                    b.append(name);
                    break;
                case LITERAL:
                    if (re.runes.length > 1) {
                        b.append("str");
                    } else {
                        b.append("lit");
                    }
                    if ((re.flags & FOLD_CASE) != 0) {
                        for (int r : re.runes) {
                            if (Unicode.simpleFold(r) != r) {
                                b.append("fold");
                                break;
                            }
                        }
                    }
                    break;
                default:
                    b.append(name);
                    break;
            }
        }
        b.append('{');
        switch (re.op) {
            case END_TEXT:
                if ((re.flags & WAS_DOLLAR) == 0) {
                    b.append("\\z");
                }
                break;
            case LITERAL:
                for (int r : re.runes) {
                    b.appendCodePoint(r);
                }
                break;
            case CONCAT:
            case ALTERNATE:
                for (Regexp sub : re.subs) {
                    dumpRegexp(b, sub);
                }
                break;
            case STAR:
            case PLUS:
            case QUEST:
                dumpRegexp(b, re.subs[0]);
                break;
            case REPEAT:
                b.append(re.min).append(',').append(re.max).append(' ');
                dumpRegexp(b, re.subs[0]);
                break;
            case CAPTURE:
                if (re.name != null && !re.name.isEmpty()) {
                    b.append(re.name);
                    b.append(':');
                }
                dumpRegexp(b, re.subs[0]);
                break;
            case CHAR_CLASS:
            {
                String sep = "";
                for (int i = 0; i < re.runes.length; i += 2) {
                    b.append(sep);
                    sep = " ";
                    int lo = re.runes[i], hi = re.runes[i + 1];
                    if (lo == hi) {
                        b.append(String.format("%#x", lo));
                    } else {
                        b.append(String.format("%#x-%#x", lo, hi));
                    }
                }
                break;
            }
        }
        b.append('}');
    }

//    private static String mkCharClass(RunePredicate f) {
//        Regexp re = new Regexp(Regexp.Op.CHAR_CLASS, null);
//        ArrayList<Integer> runes = new ArrayList<Integer>();
//        int lo = -1;
//        for (int i = 0; i <= Unicode.MAX_RUNE; i++) {
//            if (f.applies(i)) {
//                if (lo < 0) {
//                    lo = i;
//                }
//            } else {
//                if (lo >= 0) {
//                    runes.add(lo);
//                    runes.add(i - 1);
//                    lo = -1;
//                }
//            }
//        }
//        if (lo >= 0) {
//            runes.add(lo);
//            runes.add(Unicode.MAX_RUNE);
//        }
//        re.runes = new int[runes.size()];
//        int j = 0;
//        for (Integer i : runes) {
//            re.runes[j++] = i;
//        }
//        return dump(re);
//    }

//  @Test
//  public void testAppendRangeCollapse() {
//    // AppendRange should collapse each of the new ranges
//    // into the earlier ones (it looks back two ranges), so that
//    // the slice never grows very large.
//    // Note that we are not calling cleanClass.
//    CharClass cc = new CharClass();
//    // Add 'A', 'a', 'B', 'b', etc.
//    for (int i = 'A'; i <= 'Z'; i++) {
//      cc.appendRange(i, i);
//      cc.appendRange(i + 'a' - 'A', i + 'a' - 'A');
//    }
//    assertEquals("AZaz", runesToString(cc.toArray()));
//  }
//
//  // Converts an array of Unicode runes to a Java UTF-16 string.
//  private static String runesToString(int[] runes) {
//    StringBuilder out = new StringBuilder();
//    for (int rune : runes) {
//      out.appendCodePoint(rune);
//    }
//    return out.toString();
//  }

//  private static final String[] INVALID_REGEXPS = {
//    "(",
//    ")",
//    "(a",
//    "(a|b|",
//    "(a|b",
//    "[a-z",
//    "([a-z)",
//    "x{1001}",
//    "x{9876543210}",
//    "x{2,1}",
//    "x{1,9876543210}",
//    // Java string literals can't contain Invalid UTF-8.
//    // "\\xff",
//    // "[\xff]",
//    // "[\\\xff]",
//    // "\\\xff",
//    "(?P<name>a",
//    "(?P<name>",
//    "(?P<name",
//    "(?P<x y>a)",
//    "(?P<>a)",
//    "[a-Z]",
//    "(?i)[a-Z]",
//    "a{100000}",
//    "a{100000,}",
//    // Group names may not be repeated
//    "(?P<foo>bar)(?P<foo>baz)",
//    "\\x", // https://github.com/google/re2j/issues/103
//    "\\xv", // https://github.com/google/re2j/issues/103
//  };
//
//  private static final String[] ONLY_PERL = {
//    "[a-b-c]",
//    "\\Qabc\\E",
//    "\\Q*+?{[\\E",
//    "\\Q\\\\E",
//    "\\Q\\\\\\E",
//    "\\Q\\\\\\\\E",
//    "\\Q\\\\\\\\\\E",
//    "(?:a)",
//    "(?P<name>a)",
//  };
//
//  private static final String[] ONLY_POSIX = {
//    "a++", "a**", "a?*", "a+*", "a{1}*", ".{1}{2}.{3}",
//  };

//  @Test
//  public void testParseInvalidRegexps() throws PatternSyntaxException {
//    for (String regexp : INVALID_REGEXPS) {
//      try {
//        Regexp re = Parser.parse(regexp, PERL);
//        fail("Parsing (PERL) " + regexp + " should have failed, instead got " + dump(re));
//      } catch (PatternSyntaxException e) {
//        /* ok */
//      }
//      try {
//        Regexp re = Parser.parse(regexp, POSIX);
//        fail("parsing (POSIX) " + regexp + " should have failed, instead got " + dump(re));
//      } catch (PatternSyntaxException e) {
//        /* ok */
//      }
//    }
//    for (String regexp : ONLY_PERL) {
//      Parser.parse(regexp, PERL);
//      try {
//        Regexp re = Parser.parse(regexp, POSIX);
//        fail("parsing (POSIX) " + regexp + " should have failed, instead got " + dump(re));
//      } catch (PatternSyntaxException e) {
//        /* ok */
//      }
//    }
//    for (String regexp : ONLY_POSIX) {
//      try {
//        Regexp re = Parser.parse(regexp, PERL);
//        fail("parsing (PERL) " + regexp + " should have failed, instead got " + dump(re));
//      } catch (PatternSyntaxException e) {
//        /* ok */
//      }
//      Parser.parse(regexp, POSIX);
//    }
//  }

    @Test
    public void testToStringEquivalentParse() throws PatternSyntaxException {
        testRegexpTrack("|x|");

        for (String regexp : PARSE_TESTS.keySet()) {
            testRegexpTrack(regexp);
        }
    }

    private void testRegexpTrack(String regexp) throws PatternSyntaxException {
        Regexp re = Parser.parse(regexp, TEST_FLAGS);
        Track[] testTracks = PARSE_TESTS.get(regexp);
        ArrayList<Track> tracks = re.GetAllTracks();
        assertEquals(regexp, testTracks.length, tracks.size());
        for (int i = 0; i < testTracks.length; i++) {
            assertEquals(regexp+"@"+i+":start", testTracks[i].Start, tracks.get(i).Start);
            assertEquals(regexp+"@"+i+":end", testTracks[i].End, tracks.get(i).End);
            assertEquals(regexp+"@"+i+":info", testTracks[i].Comments, tracks.get(i).Comments);
        }
    }
}
