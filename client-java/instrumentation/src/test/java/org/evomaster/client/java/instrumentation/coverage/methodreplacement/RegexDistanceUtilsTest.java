package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;
/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gordon Fraser
 *
 */
public class RegexDistanceUtilsTest {
    @Test
    public void testLongRegex() {
        final String example = "-@0.AA";
        final String REGEX = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        assertTrue(example.matches(REGEX));

        assertEquals(0.0, RegexDistanceUtils.getStandardDistance(example, REGEX), 0.0);
    }

    @Test
    public void testEmptyRegex() {
        assertEquals(0.0, RegexDistanceUtils.getStandardDistance("", ""), 0.0);
        assertEquals(1.0, RegexDistanceUtils.getStandardDistance("a", ""), 0.0);
        assertEquals(2.0, RegexDistanceUtils.getStandardDistance("ab", ""), 0.0);
        assertEquals(3.0, RegexDistanceUtils.getStandardDistance("abc", ""), 0.0);
    }

    @Test
    public void testIdenticalChar() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("a", "a"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("aa", "aa"), 0.0);
    }

    @Test
    public void testReplaceChar() {
        assertEquals(1, RegexDistanceUtils.getStandardDistance("b", "a"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("ab", "aa"), 0.0);
    }

    @Test
    public void testDeleteChar() {
        assertEquals(1, RegexDistanceUtils.getStandardDistance("aa", "a"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("aaa", "a"), 0.0);
        assertEquals(3, RegexDistanceUtils.getStandardDistance("aaaa", "a"), 0.0);
        assertEquals(4, RegexDistanceUtils.getStandardDistance("aaaaa", "a"), 0.0);
    }

    @Test
    public void testInsertCharInEmptyString() {
        assertEquals(1, RegexDistanceUtils.getStandardDistance("", "a"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("", "aa"), 0.0);
        assertEquals(3, RegexDistanceUtils.getStandardDistance("", "aaa"), 0.0);
    }

    @Test
    public void testInsertChar() {
        assertEquals(1, RegexDistanceUtils.getStandardDistance("a", "aa"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("a", "aaa"), 0.0);
        assertEquals(3, RegexDistanceUtils.getStandardDistance("a", "aaaa"), 0.0);
    }

    @Test
    public void testTwoChar() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("ab", "ab"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("ab", "ba"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("ab", "bc"), 0.0);

        assertEquals(2, RegexDistanceUtils.getStandardDistance("bb", "aa"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("bb", "cc"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("bb", "ac"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("bb", "ca"), 0.0);

        assertEquals(2, RegexDistanceUtils.getStandardDistance("", "ab"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("ab", ""), 0.0);

        assertEquals(1, RegexDistanceUtils.getStandardDistance("a", "ab"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("aaa", "ab"), 0.0);
        assertEquals(3, RegexDistanceUtils.getStandardDistance("aaaa", "ab"), 0.0);

        assertEquals(1, RegexDistanceUtils.getStandardDistance("bb", "bab"), 0.0);

        assertEquals(3, RegexDistanceUtils.getStandardDistance("b", "bcab"), 0.0);
        assertEquals(4, RegexDistanceUtils.getStandardDistance("b", "bcaab"), 0.0);

        assertEquals(1.0, RegexDistanceUtils.getStandardDistance("xb", "xcb"), 0.0);
        assertEquals(1.0, RegexDistanceUtils.getStandardDistance("b", "cb"), 0.0);
        assertEquals(2.0, RegexDistanceUtils.getStandardDistance("b", "cab"), 0.0);
        assertEquals(3.0, RegexDistanceUtils.getStandardDistance("b", "caab"), 0.0);

        assertEquals(1.0, RegexDistanceUtils.getStandardDistance("b", "ab"), 0.0);
        assertEquals(2.0, RegexDistanceUtils.getStandardDistance("b", "aab"), 0.0);

    }

    @Test
    public void testThreeChar() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("abc", "abc"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("abc", "bab"), 0.0);

        assertEquals(3, RegexDistanceUtils.getStandardDistance("", "abc"), 0.0);
        assertEquals(3, RegexDistanceUtils.getStandardDistance("abc", ""), 0.0);

        assertEquals(2, RegexDistanceUtils.getStandardDistance("a", "abc"), 0.0);
        assertEquals(2.0, RegexDistanceUtils.getStandardDistance("aa", "abc"), 0.0);
        assertEquals(3.0, RegexDistanceUtils.getStandardDistance("aaaa", "abb"), 0.0);

    }

    @Test
    public void testOr() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("ac", "(a|b)a*(c|d)"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("bc", "(a|b)a*(c|d)"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("ad", "(a|b)a*(c|d)"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("bd", "(a|b)a*(c|d)"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("aac", "(a|b)a*(c|d)"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("aad", "(a|b)a*(c|d)"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("bad", "(a|b)a*(c|d)"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("baac", "(a|b)a*(c|d)"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("aaaaad", "(a|b)a*(c|d)"), 0.0);

        assertEquals(2, RegexDistanceUtils.getStandardDistance("", "(a|b)a*(c|d)"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("a", "(a|b)a*(c|d)"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("b", "(a|b)a*(c|d)"), 0.0);
        assertEquals(1.0, RegexDistanceUtils.getStandardDistance("aaa", "(a|b)a*(c|d)"), 0.0);
    }

    @Test
    public void testThreeOrFour() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("AAA", "A{3,4}"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("AAAA", "A{3,4}"), 0.0);

        assertEquals(3, RegexDistanceUtils.getStandardDistance("", "A{3,4}"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("A", "A{3,4}"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("AA", "A{3,4}"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("AAAAA", "A{3,4}"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("AAAAAA", "A{3,4}"), 0.0);
    }

    @Test
    public void testOptional() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("ac", "a.?c"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("abc", "a.?c"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("a.c", "a.?c"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("acc", "a.?c"), 0.0);

        assertEquals(2, RegexDistanceUtils.getStandardDistance("", "a.?c"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("acd", "a.?c"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("a", "a.?c"), 0.0);
        assertEquals(1.0, RegexDistanceUtils.getStandardDistance("cc", "a.?c"), 0.0);
    }

    @Test
    public void testDeletionFollowedByInsertion() {
        /*
         * this does not work, as expected.
         * Cannot delete last 'd' and _then_ replace second 'd' with a 'c'
         * in the distance calculation. Even if distance is not precise,
         *  AVM should still be able to solve the constraint
         */

        double addd = RegexDistanceUtils.getStandardDistance("addd", "a.?c");
        double add = RegexDistanceUtils.getStandardDistance("add", "a.?c");

        assertTrue(addd != 1.5d);
        assertTrue(add < addd);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("add", "a.?c"), 0.0);
    }

    @Test
    public void testRange() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("A", "[A-Z-0-9]+"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("1", "[A-Z-0-9]+"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("A1", "[A-Z-0-9]+"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("A1B2", "[A-Z-0-9]+"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("3H8J2", "[A-Z-0-9]+"), 0.0);

        assertEquals(1, RegexDistanceUtils.getStandardDistance("", "[A-Z-0-9]+"), 0.0);
        assertEquals(1.0, RegexDistanceUtils.getStandardDistance("a", "[A-Z-0-9]+"), 0.0);
        assertEquals(1.0, RegexDistanceUtils.getStandardDistance("1a", "[A-Z-0-9]+"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("A1By", "[A-Z-0-9]+"), 0.1);
        assertEquals(2.0, RegexDistanceUtils.getStandardDistance("1aa", "[A-Z-0-9]+"), 0.0);
    }

    @Test
    public void testEmail() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("ZhiX@Hhhh",
                "[A-Za-z]{4,10}\\@[A-Za-z]{4,10}"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("ZhiX@Hhh",
                "[A-Za-z]{4,10}\\@[A-Za-z]{4,10}"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("ZhiX@Hh",
                "[A-Za-z]{4,10}\\@[A-Za-z]{4,10}"), 0.0);
        assertEquals(3, RegexDistanceUtils.getStandardDistance("ZhiX@H",
                "[A-Za-z]{4,10}\\@[A-Za-z]{4,10}"), 0.0);

        //2 replacements and 4 insertions
        assertEquals(5,
                RegexDistanceUtils.getStandardDistance("hiX@H", "[A-Za-z]{4,10}\\@[A-Za-z]{4,10}"),
                1.0);

        assertEquals(4,
                RegexDistanceUtils.getStandardDistance("ZhiXH", "[A-Za-z]{4,10}\\@[A-Za-z]{4,10}"),
                0.3);

    }

    @Test
    public void testClosure() {
        assertEquals(0, RegexDistanceUtils.getStandardDistance("", "[a0]*"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("a", "[a0]"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("0", "[a0]"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("a", "[a0]+"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("0", "[a0]+"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("a", "[a0]*"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("0", "[a0]*"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("test", "[a0]*test"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("atest", "[a0]*test"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("0test", "[a0]*test"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("a0a0test", "[a0]*test"), 0.0);
        assertEquals(0, RegexDistanceUtils.getStandardDistance("aaaaa0a0test", "[a0]*test"), 0.0);

        assertEquals(4, RegexDistanceUtils.getStandardDistance("", "[a0]*test"), 0.0);
        assertEquals(3, RegexDistanceUtils.getStandardDistance("t", "[a0]*test"), 0.0);
        assertEquals(2, RegexDistanceUtils.getStandardDistance("te", "[a0]*test"), 0.0);
        assertEquals(1, RegexDistanceUtils.getStandardDistance("tes", "[a0]*test"), 0.0);
    }

    @Test
    public void testGroups() {
        assertEquals(0,
                RegexDistanceUtils.getStandardDistance("tue",
                        "((mon)|(tue)|(wed)|(thur)|(fri)|(sat)|(sun))"),
                0.0);

    }

    @Test
    public void testWordBoundaries() {
        String regex = ".*\\bhallo\\b.*";
        String str = "hallo test";
        Pattern p = Pattern.compile(regex);
        if (p.matcher(str).matches()) {
            assertEquals(0, RegexDistanceUtils.getStandardDistance(str, regex));
        } else {
            assertTrue(0 < RegexDistanceUtils.getStandardDistance(str, regex));
        }
    }
}