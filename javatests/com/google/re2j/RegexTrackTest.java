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
                new Track(0, 2, "sequence of [literal 'a',any characters excluding \"\\n\"]"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "any characters excluding \"\\n\""),
        });
        put("a.b", new Track[]{
                new Track(0, 3, "sequence of [literal 'a',any characters excluding \"\\n\",literal 'b']"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "any characters excluding \"\\n\""),
                new Track(2, 3, "literal 'b'"),
        });
        put("ab", new Track[]{
                new Track(0, 2, "string \"ab\""),
        });
        put("a.b.c", new Track[]{
                new Track(0, 5, "sequence of [literal 'a',any characters excluding \"\\n\",literal 'b',any characters excluding \"\\n\",literal 'c']"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "any characters excluding \"\\n\""),
                new Track(2, 3, "literal 'b'"),
                new Track(3, 4, "any characters excluding \"\\n\""),
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
                new Track(0, 3, "capturing group of [literal 'a']"),
                new Track(0, 1, "capturing group"),
                new Track(1, 2, "literal 'a'"),
                new Track(2, 3, "capturing group end"),
        });
        put("(a)|b", new Track[]{
                new Track(0, 5, "alternation of [capturing group of [literal 'a'],literal 'b']"),
                new Track(0, 3, "capturing group of [literal 'a']"),
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
                new Track(1, 3, "quantifier: repeated zero or many times(non-greedy)"),
        });
        put("a+?", new Track[]{
                new Track(0, 3, "literal 'a' repeated once or many times(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 3, "quantifier: repeated once or many times(non-greedy)"),
        });
        put("a??", new Track[]{
                new Track(0, 3, "literal 'a' repeated zero or once(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 3, "quantifier: repeated zero or once(non-greedy)"),
        });
        put("a{2}?", new Track[]{
                new Track(0, 5, "literal 'a' repeated twice(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 5, "quantifier: repeated twice(non-greedy)"),
        });
        put("a{2,3}?", new Track[]{
                new Track(0, 7, "literal 'a' repeated twice to 3 times(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 7, "quantifier: repeated twice to 3 times(non-greedy)"),
        });
        put("a{2,}?", new Track[]{
                new Track(0, 6, "literal 'a' repeated at least twice(non-greedy)"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 6, "quantifier: repeated at least twice(non-greedy)"),
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
        put("|", new Track[]{
                new Track(0, 1, "empty string"),
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
                new Track(0, 1, "any characters excluding \"\\n\""),
        });
        put("^", new Track[]{
                new Track(0, 1, "line start"),
        });
        put("$", new Track[]{
                new Track(0, 1, "line end"),
        });
        put("\\|", new Track[]{
                new Track(0, 2, "literal '|'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '|'"),
        });
        put("\\(", new Track[]{
                new Track(0, 2, "literal '('"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '('"),
        });
        put("\\)", new Track[]{
                new Track(0, 2, "literal ')'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal ')'"),
        });
        put("\\*", new Track[]{
                new Track(0, 2, "literal '*'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '*'"),
        });
        put("\\+", new Track[]{
                new Track(0, 2, "literal '+'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '+'"),
        });
        put("\\?", new Track[]{
                new Track(0, 2, "literal '?'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '?'"),
        });
        put("{", new Track[]{
                new Track(0, 1, "literal '{'"),
        });
        put("}", new Track[]{
                new Track(0, 1, "literal '}'"),
        });
        put("\\.", new Track[]{
                new Track(0, 2, "literal '.'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '.'"),
        });
        put("\\^", new Track[]{
                new Track(0, 2, "literal '^'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '^'"),
        });
        put("\\$", new Track[]{
                new Track(0, 2, "literal '$'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '$'"),
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
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '-'"),
        });
        put("-", new Track[]{
                new Track(0, 1, "literal '-'"),
        });
        put("\\_", new Track[]{
                new Track(0, 2, "literal '_'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '_'"),
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
                new Track(0, 11, "character class of [lowercase letters]"),
                new Track(0, 1, "character class"),
                new Track(1, 10, "POSIX class:lowercase letters"),
                new Track(10, 11, "character class end"),
        });
        put("[^[:lower:]]", new Track[]{
                new Track(0, 12, "negated character class of [lowercase letters]"),
                new Track(0, 2, "negated character class"),
                new Track(2, 11, "POSIX class:lowercase letters"),
                new Track(11, 12, "character class end"),
        });
        put("[[:^lower:]]", new Track[]{
                new Track(0, 12, "character class of [negated lowercase letters]"),
                new Track(0, 1, "character class"),
                new Track(1, 11, "POSIX class:negated lowercase letters"),
                new Track(11, 12, "character class end"),
        });
        put("(?i)[[:lower:]]", new Track[]{
                new Track(0, 15, "character class of [case insensitive,lowercase letters]"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "character class"),
                new Track(5, 14, "POSIX class:lowercase letters"),
                new Track(14, 15, "character class end"),
        });
        put("(?i)[a-z]", new Track[]{
                new Track(0, 9, "character class of [case insensitive,range a to z]"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "character class"),
                new Track(5, 8, "range a to z"),
                new Track(8, 9, "character class end"),
        });
        put("(?i)[^[:lower:]]", new Track[]{
                new Track(0, 16, "negated character class of [case insensitive,lowercase letters]"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 6, "negated character class"),
                new Track(6, 15, "POSIX class:lowercase letters"),
                new Track(15, 16, "character class end"),
        });
        put("(?i)[[:^lower:]]", new Track[]{
                new Track(0, 16, "character class of [case insensitive,negated lowercase letters]"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "character class"),
                new Track(5, 15, "POSIX class:negated lowercase letters"),
                new Track(15, 16, "character class end"),
        });
        put("\\d", new Track[]{
                new Track(0, 2, "digits"),
        });
        put("\\D", new Track[]{
                new Track(0, 2, "non-digits"),
        });
        put("\\s", new Track[]{
                new Track(0, 2, "whitespace"),
        });
        put("\\S", new Track[]{
                new Track(0, 2, "non-whitespace"),
        });
        put("\\w", new Track[]{
                new Track(0, 2, "word character"),
        });
        put("\\W", new Track[]{
                new Track(0, 2, "non-word character"),
        });
        put("(?i)\\w", new Track[]{
                // FIXME the topmost track
                new Track(0, 6, "character class of [case insensitive,word character]"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 6, "word character"),
        });
        put("(?i)\\W", new Track[]{
                // FIXME the topmost track
                new Track(0, 6, "character class of [case insensitive,non-word character]"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 6, "non-word character"),
        });
        put("[^\\\\]", new Track[]{
                // FIXME the topmost track
                new Track(0, 5, "negated character class of [literal '\\']"),
                new Track(0, 2, "negated character class"),
                new Track(2, 3, "escape"),
                new Track(3, 4, "literal '\\'"),
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
                new Track(0, 8, "string \".^$\\\""),
                new Track(0, 2, "literal '.'"),
                new Track(0, 1, "escape"),
                new Track(1, 2, "literal '.'"),
                new Track(2, 4, "literal '^'"),
                new Track(2, 3, "escape"),
                new Track(3, 4, "literal '^'"),
                new Track(4, 6, "literal '$'"),
                new Track(4, 5, "escape"),
                new Track(5, 6, "literal '$'"),
                new Track(6, 8, "literal '\\'"),
                new Track(6, 7, "escape"),
                new Track(7, 8, "literal '\\'"),
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
                new Track(0, 4, "negated character class of [literal 'a']"),
                new Track(0, 2, "negated character class"),
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
                new Track(0, 3, "sequence of [literal 'a' repeated zero or many times,literal '{']"),
                new Track(0, 2, "literal 'a' repeated zero or many times"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "quantifier: repeated zero or many times"),
                new Track(2, 3, "literal '{'"),
        });

        put("(?:ab)*", new Track[]{
                new Track(0, 7, "string \"ab\" repeated zero or many times"),
                new Track(0, 6, "string \"ab\""),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 5, "string \"ab\""),
                new Track(5, 6, "capturing group end"),
                new Track(6, 7, "quantifier: repeated zero or many times"),
        });

        put("(ab)*", new Track[]{
                new Track(0, 5, "capturing group of [string \"ab\"] repeated zero or many times"),
                new Track(0, 4, "capturing group of [string \"ab\"]"),
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
                new Track(0, 7, "sequence of [literal 'a',capturing group of [alternation of [literal 'b',literal 'c']],literal 'd']"),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 6, "capturing group of [alternation of [literal 'b',literal 'c']]"),
                new Track(1, 2, "capturing group"),
                new Track(2, 5, "alternation of [literal 'b',literal 'c']"),
                new Track(2, 3, "literal 'b'"),
                new Track(3, 4, "alternation"),
                new Track(4, 5, "literal 'c'"),
                new Track(5, 6, "capturing group end"),
                new Track(6, 7, "literal 'd'"),
        });

        put("(?:a)", new Track[]{
                new Track(0, 5, "literal 'a'"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "literal 'a'"),
                new Track(4, 5, "capturing group end"),
        });

        put("(?:ab)(?:cd)", new Track[]{
                new Track(0, 12, "string \"abcd\""),

                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 5, "string \"ab\""),
                new Track(5, 6, "capturing group end"),

                new Track(6, 9, "non-capturing group"),
                new Track(6, 8, "non-capturing group start"),
                new Track(8, 9, "mod modifier end"),
                new Track(9, 11, "string \"cd\""),
                new Track(11, 12, "capturing group end"),
        });

        put("(?:a+b+)(?:c+d+)", new Track[]{
                new Track(0, 16, "sequence of [sequence of [literal 'a' repeated once or many times,literal 'b' repeated once or many times],sequence of [literal 'c' repeated once or many times,literal 'd' repeated once or many times]]"),
                new Track(0, 8, "sequence of [literal 'a' repeated once or many times,literal 'b' repeated once or many times]"),

                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 5, "literal 'a' repeated once or many times"),
                new Track(3, 4, "literal 'a'"),
                new Track(4, 5, "quantifier: repeated once or many times"),
                new Track(5, 7, "literal 'b' repeated once or many times"),
                new Track(5, 6, "literal 'b'"),
                new Track(6, 7, "quantifier: repeated once or many times"),
                new Track(7, 8, "capturing group end"),

                new Track(8, 16, "sequence of [literal 'c' repeated once or many times,literal 'd' repeated once or many times]"),

                new Track(8, 11, "non-capturing group"),
                new Track(8, 10, "non-capturing group start"),
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
                new Track(0, 19, "alternation of [alternation of [literal 'a' repeated once or many times,literal 'b' repeated once or many times],alternation of [literal 'c' repeated once or many times,literal 'd' repeated once or many times]]"),
                new Track(0, 9, "alternation of [literal 'a' repeated once or many times,literal 'b' repeated once or many times]"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
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
                new Track(10, 19, "alternation of [literal 'c' repeated once or many times,literal 'd' repeated once or many times]"),
                new Track(10, 13, "non-capturing group"),
                new Track(10, 12, "non-capturing group start"),
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
                new Track(0, 15, "alternation of [literal 'a',literal 'b',literal 'c',literal 'd']"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "literal 'a'"),
                new Track(4, 5, "alternation"),
                new Track(5, 6, "literal 'b'"),
                new Track(6, 7, "capturing group end"),
                new Track(7, 8, "alternation"),
                new Track(8, 15, "alternation of [literal 'c',literal 'd']"),
                new Track(8, 11, "non-capturing group"),
                new Track(8, 10, "non-capturing group start"),
                new Track(10, 11, "mod modifier end"),
                new Track(11, 12, "literal 'c'"),
                new Track(12, 13, "alternation"),
                new Track(13, 14, "literal 'd'"),
                new Track(14, 15, "capturing group end"),
        });

        put("a|.", new Track[]{
                new Track(0, 3, "any characters excluding \"\\n\""),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 2, "alternation"),
                new Track(2, 3, "any characters excluding \"\\n\""),
        });

        put(".|a", new Track[]{
                new Track(0, 3, "any characters excluding \"\\n\""),
                new Track(0, 1, "any characters excluding \"\\n\""),
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

        put("(?:[abc]|A|Z)", new Track[]{
                new Track(0, 13, "alternation of [literal 'a',literal 'b',literal 'c',literal 'A',literal 'Z']"),
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
                new Track(12, 13, "capturing group end"),
        });

        put("\\Q+|*?{[\\E", new Track[]{
                new Track(0, 10, "string \"+|*?{[\""),
                new Track(0, 3, "literal '+'"),
                new Track(0, 2, "escaped string start"),
                new Track(2, 3, "literal '+'"),
                new Track(3, 4, "literal '|'"),
                new Track(4, 5, "literal '*'"),
                new Track(5, 6, "literal '?'"),
                new Track(6, 7, "literal '{'"),
                new Track(7, 8, "literal '['"),
                new Track(8, 10, "escaped string end"),
        });

        put("\\Q+\\E+", new Track[]{
                new Track(0, 6, "literal '+' repeated once or many times"),
                new Track(0, 5, "literal '+'"),
                new Track(0, 3, "literal '+'"),
                new Track(0, 2, "escaped string start"),
                new Track(2, 3, "literal '+'"),
                new Track(3, 5, "escaped string end"),
                new Track(5, 6, "quantifier: repeated once or many times"),
        });

        put("\\Qab\\E+", new Track[]{
                new Track(0, 7, "sequence of [literal 'a',literal 'b' repeated once or many times]"),
                new Track(0, 3, "literal 'a'"),
                new Track(0, 2, "escaped string start"),
                new Track(2, 3, "literal 'a'"),
                new Track(3, 7, "literal 'b' repeated once or many times"),
                new Track(3, 6, "literal 'b'"),
                new Track(3, 4, "literal 'b'"),
                new Track(4, 6, "escaped string end"),
                new Track(6, 7, "quantifier: repeated once or many times"),
        });

        put("\\Q\\\\E", new Track[]{
                new Track(0, 5, "literal '\\'"),
                new Track(0, 3, "literal '\\'"),
                new Track(0, 2, "escaped string start"),
                new Track(2, 3, "literal '\\'"),
                new Track(3, 5, "escaped string end"),
        });

        put("\\Q\\\\\\E", new Track[]{
                new Track(0, 6, "string \"\\\\\""),
                new Track(0, 3, "literal '\\'"),
                new Track(0, 2, "escaped string start"),
                new Track(2, 3, "literal '\\'"),
                new Track(3, 4, "literal '\\'"),
                new Track(4, 6, "escaped string end"),
        });

        put("(?m)^", new Track[]{
                new Track(0, 5, "line start"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "multi-line: '^' and '$' match at the start and end of each line"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "line start"),
        });

        put("(?-m)^", new Track[]{
                new Track(0, 6, "word start"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "negated"),
                new Track(3, 4, "multi-line: '^' and '$' match at the start and end of each line"),
                new Track(4, 5, "capturing group end"),
                new Track(5, 6, "word start"),
        });

        put("(?m)$", new Track[]{
                new Track(0, 5, "line end"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "multi-line: '^' and '$' match at the start and end of each line"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "line end"),
        });

        put("(?-m)$", new Track[]{
                new Track(0, 6, "word end"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "negated"),
                new Track(3, 4, "multi-line: '^' and '$' match at the start and end of each line"),
                new Track(4, 5, "capturing group end"),
                new Track(5, 6, "word end"),
        });

        put("(?m)\\A", new Track[]{
                new Track(0, 6, "word start"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "multi-line: '^' and '$' match at the start and end of each line"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 6, "word start"),
        });

        put("(?-m)\\A", new Track[]{
                new Track(0, 7, "word start"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "negated"),
                new Track(3, 4, "multi-line: '^' and '$' match at the start and end of each line"),
                new Track(4, 5, "capturing group end"),
                new Track(5, 7, "word start"),
        });

        put("(?m)\\z", new Track[]{
                new Track(0, 6, "word end"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "multi-line: '^' and '$' match at the start and end of each line"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 6, "word end"),
        });

        put("(?-m)\\z", new Track[]{
                new Track(0, 7, "word end"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "negated"),
                new Track(3, 4, "multi-line: '^' and '$' match at the start and end of each line"),
                new Track(4, 5, "capturing group end"),
                new Track(5, 7, "word end"),
        });

        put("(?P<name>a)", new Track[]{
                new Track(0, 11, "capturing group \"name\" of [literal 'a']"),
                new Track(0, 9, "non-capturing group"),
                new Track(0, 4, "group name"),
                new Track(4, 8, "group name:\"name\""),
                new Track(8, 9, "group name end"),
                new Track(9, 10, "literal 'a'"),
                new Track(10, 11, "capturing group end"),
        });

        put("[Aa]", new Track[]{
                new Track(0, 4, "case insensitive literal 'A'"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "literal 'A'"),
                new Track(2, 3, "literal 'a'"),
                new Track(3, 4, "character class end"),
        });

        put("[\\x{100}\\x{101}]", new Track[]{
                new Track(0, 16, "case insensitive literal 'Ā'"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "escape"),
                new Track(2, 8, "hexadecimal 256"),
                new Track(8, 9, "escape"),
                new Track(9, 15, "hexadecimal 257"),
                new Track(15, 16, "character class end"),
        });

        put("[Δδ]", new Track[]{
                new Track(0, 4, "case insensitive literal 'Δ'"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "literal 'Δ'"),
                new Track(2, 3, "literal 'δ'"),
                new Track(3, 4, "character class end"),
        });

        put("abcde", new Track[]{
                new Track(0, 5, "string \"abcde\""),
        });

        put("[Aa][Bb]cd", new Track[]{
                new Track(0, 10, "sequence of [case insensitive string \"AB\",string \"cd\"]"),
                new Track(0, 8, "case insensitive string \"AB\""),
                new Track(0, 4, "case insensitive literal 'A'"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "literal 'A'"),
                new Track(2, 3, "literal 'a'"),
                new Track(3, 4, "character class end"),
                new Track(4, 8, "case insensitive literal 'B'"),
                new Track(4, 5, "character class"),
                new Track(5, 6, "literal 'B'"),
                new Track(6, 7, "literal 'b'"),
                new Track(7, 8, "character class end"),
                new Track(8, 10, "string \"cd\""),
        });

        put("abc|abd|aef|bcx|bcy", new Track[]{
                new Track(0, 19, "alternation of [string \"abc\",string \"abd\",string \"aef\",string \"bcx\",string \"bcy\"]"),
                new Track(0, 3, "string \"abc\""),
                new Track(3, 4, "alternation"),
                new Track(4, 7, "string \"abd\""),
                new Track(7, 8, "alternation"),
                new Track(8, 11, "string \"aef\""),
                new Track(11, 12, "alternation"),
                new Track(12, 15, "string \"bcx\""),
                new Track(15, 16, "alternation"),
                new Track(16, 19, "string \"bcy\""),
        });

        //    {
//      "ax+y|ax+z|ay+w",
//      "cat{lit{a}alt{cat{plus{lit{x}}lit{y}}cat{plus{lit{x}}lit{z}}cat{plus{lit{y}}lit{w}}}}"
//    },

        put("ax+y|ax+z|ay+w", new Track[]{
                new Track(0, 14, "sequence of [sequence of [literal 'a',literal 'x' repeated once or many times,literal 'y'],sequence of [literal 'a',literal 'x' repeated once or many times,literal 'z'],sequence of [literal 'a',literal 'y' repeated once or many times,literal 'w']]"),
                new Track(0, 4, "sequence of [literal 'a',literal 'x' repeated once or many times,literal 'y']"),
                // FIXME lack of track of literal 'a'
                new Track(1, 3, "literal 'x' repeated once or many times"),
                new Track(1, 2, "literal 'x'"),
                new Track(2, 3, "quantifier: repeated once or many times"),
                new Track(3, 4, "literal 'y'"),
                new Track(4, 5, "alternation"),
                new Track(5, 9, "sequence of [literal 'a',literal 'x' repeated once or many times,literal 'z']"),

                new Track(6, 8, "literal 'x' repeated once or many times"),
                new Track(6, 7, "literal 'x'"),
                new Track(7, 8, "quantifier: repeated once or many times"),
                new Track(8, 9, "literal 'z'"),

                new Track(9, 10, "alternation"),
                new Track(10, 14, "sequence of [literal 'a',literal 'y' repeated once or many times,literal 'w']"),

                new Track(11, 13, "literal 'y' repeated once or many times"),
                new Track(11, 12, "literal 'y'"),
                new Track(12, 13, "quantifier: repeated once or many times"),
                new Track(13, 14, "literal 'w'"),
        });

        put("(?:.)", new Track[]{
                new Track(0, 5, "any characters excluding \"\\n\""),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "any characters excluding \"\\n\""),
                new Track(4, 5, "capturing group end"),
        });

        //    {"(?:x|(?:xa))", "cat{lit{x}alt{emp{}lit{a}}}"},
        put("(?:x|(?:xa))", new Track[]{
                // FIXME not correct
                new Track(0, 12, "sequence of [literal 'x',string \"xa\"]"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "literal 'x'"),
                new Track(4, 5, "alternation"),
                new Track(5, 11, "string \"xa\""),
                new Track(5, 8, "non-capturing group"),
                new Track(5, 7, "non-capturing group start"),
                new Track(7, 8, "mod modifier end"),
                new Track(8, 10, "string \"xa\""),
                new Track(10, 11, "capturing group end"),
                new Track(11, 12, "capturing group end"),
        });

        //    {"(?:.|(?:.a))", "cat{dot{}alt{emp{}lit{a}}}"},
        put("(?:.|(?:.a))", new Track[]{
                // FIXME not correct
                new Track(0, 5, "sequence of [any characters excluding \"\\n\"]"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "any characters excluding \"\\n\""),
                new Track(4, 5, "alternation"),
                // FIXME lack of the second group
                new Track(9, 10, "literal 'a'"),
                new Track(11, 12, "capturing group end"),
        });

        put("(?:A(?:A|a))", new Track[]{
                new Track(0, 12, "sequence of [literal 'A',case insensitive literal 'A']"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "literal 'A'"),
                new Track(4, 11, "case insensitive literal 'A'"),
                new Track(4, 7, "non-capturing group"),
                new Track(4, 6, "non-capturing group start"),
                new Track(6, 7, "mod modifier end"),
                new Track(7, 8, "literal 'A'"),
                new Track(8, 9, "alternation"),
                new Track(9, 10, "literal 'a'"),
                new Track(10, 11, "capturing group end"),
                new Track(11, 12, "capturing group end"),
        });

        put("(?:A|a)", new Track[]{
                new Track(0, 7, "case insensitive literal 'A'"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 4, "literal 'A'"),
                new Track(4, 5, "alternation"),
                new Track(5, 6, "literal 'a'"),
                new Track(6, 7, "capturing group end"),
        });

        put("A|(?:A|a)", new Track[]{
                new Track(0, 9, "case insensitive literal 'A'"),
                new Track(0, 1, "literal 'A'"),
                new Track(1, 2, "alternation"),
                new Track(2, 9, "case insensitive literal 'A'"),
                new Track(2, 5, "non-capturing group"),
                new Track(2, 4, "non-capturing group start"),
                new Track(4, 5, "mod modifier end"),
                new Track(5, 6, "literal 'A'"),
                new Track(6, 7, "alternation"),
                new Track(7, 8, "literal 'a'"),
                new Track(8, 9, "capturing group end"),
        });

        //    {"(?s).", "dot{}"},
        put("(?s).", new Track[]{
                new Track(0, 5, "any characters excluding \"\\n\""),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "single-line: dot also matches line breaks"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "any characters excluding \"\\n\""),
        });

        //    {"(?-s).", "dnl{}"},
        put("(?-s).", new Track[]{
                // FIXME not correct
                new Track(0, 6, "any characters excluding \"\\n\""),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "negated"),
                new Track(3, 4, "single-line: dot also matches line breaks"),
                new Track(4, 5, "capturing group end"),
                new Track(5, 6, "any characters excluding \"\\n\""),
        });

        put("(?:(?:^).)", new Track[]{
                new Track(0, 10, "sequence of [line start,any characters excluding \"\\n\"]"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 8, "line start"),
                new Track(3, 6, "non-capturing group"),
                new Track(3, 5, "non-capturing group start"),
                new Track(5, 6, "mod modifier end"),
                new Track(6, 7, "line start"),
                new Track(7, 8, "capturing group end"),
                new Track(8, 9, "any characters excluding \"\\n\""),
                new Track(9, 10, "capturing group end"),
        });

        //    {"(?-s)(?:(?:^).)", "cat{bol{}dnl{}}"},
        put("(?-s)(?:(?:^).)", new Track[]{
                // FIXME not correct
                new Track(0, 15, "sequence of [negated,single-line: dot also matches line breaks,line start,any characters excluding \"\\n\"]"),
                new Track(0, 8, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "negated"),
                new Track(3, 4, "single-line: dot also matches line breaks"),
                new Track(4, 5, "capturing group end"),
                new Track(5, 7, "non-capturing group start"),
                new Track(7, 8, "mod modifier end"),
                new Track(8, 13, "line start"),
                new Track(8, 11, "non-capturing group"),
                new Track(8, 10, "non-capturing group start"),
                new Track(10, 11, "mod modifier end"),
                new Track(11, 12, "line start"),
                new Track(12, 13, "capturing group end"),
                new Track(13, 14, "any characters excluding \"\\n\""),
                new Track(14, 15, "capturing group end"),
        });

        //    {"[\\x00-\\x{10FFFF}]", "dot{}"},
        put("[\\x00-\\x{10FFFF}]", new Track[]{
                // FIXME not correct
                new Track(0, 17, "character class of [hexadecimal 0,string \"-\\\",hexadecimal 1114111]"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "escape"),
                new Track(2, 5, "hexadecimal 0"),
                new Track(5, 7, "string \"-\\\""),
                new Track(7, 16, "hexadecimal 1114111"),
                new Track(16, 17, "character class end"),
        });

        //    {"[^\\x00-\\x{10FFFF}]", "cc{}"},
        put("[^\\x00-\\x{10FFFF}]", new Track[]{
                // FIXME not correct
                new Track(0, 18, "negated character class of [hexadecimal 0,string \"-\\\",hexadecimal 1114111]"),
                new Track(0, 2, "negated character class"),
                new Track(2, 3, "escape"),
                new Track(3, 6, "hexadecimal 0"),
                new Track(6, 8, "string \"-\\\""),
                new Track(8, 17, "hexadecimal 1114111"),
                new Track(17, 18, "character class end"),
        });

        put("(?:[a][a-])", new Track[]{
                new Track(0, 11, "sequence of [literal 'a',character class of [literal 'a',literal '-']]"),
                new Track(0, 3, "non-capturing group"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "mod modifier end"),
                new Track(3, 6, "literal 'a'"),
                new Track(3, 4, "character class"),
                new Track(4, 5, "literal 'a'"),
                new Track(5, 6, "character class end"),
                new Track(6, 10, "character class of [literal 'a',literal '-']"),
                new Track(6, 7, "character class"),
                new Track(7, 8, "literal 'a'"),
                new Track(8, 9, "literal '-'"),
                new Track(9, 10, "character class end"),
                new Track(10, 11, "capturing group end"),
        });

        //    {"abc|abd", "cat{str{ab}cc{0x63-0x64}}"},
        put("abc|abd", new Track[]{
                // FIXME not correct
                new Track(0, 7, "sequence of [string \"abc\",string \"abd\"]"),
                new Track(0, 3, "string \"abc\""),
                new Track(3, 4, "alternation"),
                new Track(4, 7, "string \"abd\""),
        });

        //    {"a(?:b)c|abd", "cat{str{ab}cc{0x63-0x64}}"},
        put("a(?:b)c|abd", new Track[]{
                // FIXME not correct
                new Track(0, 11, "sequence of [string \"abc\",string \"abd\"]"),
                new Track(0, 7, "string \"abc\""),
                new Track(0, 1, "literal 'a'"),
                new Track(1, 4, "non-capturing group"),
                new Track(1, 3, "non-capturing group start"),
                new Track(3, 4, "mod modifier end"),
                new Track(4, 5, "literal 'b'"),
                new Track(5, 6, "capturing group end"),
                new Track(6, 7, "literal 'c'"),
                new Track(7, 8, "alternation"),
                new Track(8, 11, "string \"abd\""),
        });

        put("abc|x|abd", new Track[]{
                new Track(0, 9, "alternation of [string \"abc\",literal 'x',string \"abd\"]"),
                new Track(0, 3, "string \"abc\""),
                new Track(3, 4, "alternation"),
                new Track(4, 5, "literal 'x'"),
                new Track(5, 6, "alternation"),
                new Track(6, 9, "string \"abd\""),
        });

        put("(?i)abc|ABD", new Track[]{
                new Track(0, 11, "sequence of [case insensitive string \"ABC\",string \"ABD\"]"),
                new Track(0, 7, "case insensitive string \"ABC\""),
                new Track(0, 5, "case insensitive literal 'A'"),
                new Track(0, 2, "non-capturing group start"),
                new Track(2, 3, "case insensitive"),
                new Track(3, 4, "capturing group end"),
                new Track(4, 5, "literal 'a'"),
                new Track(5, 6, "literal 'b'"),
                new Track(6, 7, "literal 'c'"),
                new Track(7, 8, "alternation"),
                new Track(8, 11, "string \"ABD\""),
        });

        //    {"[ab]c|[ab]d", "cat{cc{0x61-0x62}cc{0x63-0x64}}"},
        put("[ab]c|[ab]d", new Track[]{
                new Track(0, 6, "sequence of [character class of [literal 'a',literal 'b'],literal 'c']"),
                new Track(0, 4, "character class of [literal 'a',literal 'b']"),
                new Track(0, 1, "character class"),
                new Track(1, 2, "literal 'a'"),
                new Track(2, 3, "literal 'b'"),
                new Track(3, 4, "character class end"),
                new Track(4, 5, "literal 'c'"),
                new Track(5, 6, "alternation"),
                // FIXME lack of tracks
                new Track(10, 11, "literal 'd'"),
        });

        //    {".c|.d", "cat{dot{}cc{0x63-0x64}}"},
        put(".c|.d", new Track[]{
                new Track(0, 3, "sequence of [any characters excluding \"\\n\",literal 'c']"),
                new Track(0, 1, "any characters excluding \"\\n\""),
                new Track(1, 2, "literal 'c'"),
                new Track(2, 3, "alternation"),
                // FIXME lack of tracks
                new Track(4, 5, "literal 'd'"),
        });

        //    {"x{2}|x{2}[0-9]", "cat{rep{2,2 lit{x}}alt{emp{}cc{0x30-0x39}}}"},
        put("x{2}|x{2}[0-9]", new Track[]{
                new Track(0, 5, "sequence of [literal 'x' repeated twice]"),
                new Track(0, 4, "literal 'x' repeated twice"),
                new Track(0, 1, "literal 'x'"),
                new Track(1, 4, "quantifier: repeated twice"),
                new Track(4, 5, "alternation"),

                // FIXME lack of tracks
                new Track(9, 14, "character class of [range 0 to 9]"),
                new Track(9, 10, "character class"),
                new Track(10, 13, "range 0 to 9"),
                new Track(13, 14, "character class end"),
        });

        //    {"x{2}y|x{2}[0-9]y", "cat{rep{2,2 lit{x}}alt{lit{y}cat{cc{0x30-0x39}lit{y}}}}"},
        put("x{2}y|x{2}[0-9]y", new Track[]{
                new Track(0, 16, "sequence of [literal 'x' repeated twice,literal 'y',sequence of [literal 'x' repeated twice,character class of [range 0 to 9],literal 'y']]"),
                new Track(0, 4, "literal 'x' repeated twice"),
                new Track(0, 1, "literal 'x'"),
                new Track(1, 4, "quantifier: repeated twice"),
                new Track(4, 5, "literal 'y'"),
                new Track(5, 6, "alternation"),
                new Track(6, 16, "sequence of [literal 'x' repeated twice,character class of [range 0 to 9],literal 'y']"),

                // FIXME lack of tracks
                new Track(10, 15, "character class of [range 0 to 9]"),
                new Track(10, 11, "character class"),
                new Track(11, 14, "range 0 to 9"),
                new Track(14, 15, "character class end"),
                new Track(15, 16, "literal 'y'"),
        });

        put("a.*?c|a.*?b", new Track[]{
                new Track(0, 16, "sequence of [literal 'x' repeated twice,literal 'y',sequence of [literal 'x' repeated twice,character class of [range 0 to 9],literal 'y']]"),
                new Track(0, 4, "literal 'x' repeated twice"),
                new Track(0, 1, "literal 'x'"),
                new Track(1, 4, "quantifier: repeated twice"),
                new Track(4, 5, "literal 'y'"),
                new Track(5, 6, "alternation"),
                new Track(6, 16, "sequence of [literal 'x' repeated twice,character class of [range 0 to 9],literal 'y']"),

                // FIXME lack of tracks
                new Track(10, 15, "character class of [range 0 to 9]"),
                new Track(10, 11, "character class"),
                new Track(11, 14, "range 0 to 9"),
                new Track(14, 15, "character class end"),
                new Track(15, 16, "literal 'y'"),
        });
    }};

//  {
//
//    // RE2 prefix_tests
//    {
//      "abc|abd|aef|bcx|bcy",
//      "alt{cat{lit{a}alt{cat{lit{b}cc{0x63-0x64}}str{ef}}}" + "cat{str{bc}cc{0x78-0x79}}}"
//    },


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
        testRegexpTrack("a.*?c|a.*?b");

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
