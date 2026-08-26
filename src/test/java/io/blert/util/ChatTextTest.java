/*
 * Copyright (c) 2026 Alexei Frolov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.blert.util;

import junit.framework.TestCase;

public class ChatTextTest extends TestCase {
    public void testStripsHtmlStyleTags() {
        assertEquals("Wave: 1", ChatText.stripFormatting("<col=ff0000>Wave: 1</col>"));
        assertEquals(
                "Wave 'The Maiden of Sugadinti' (Normal Mode) complete!Duration: 1:23.40",
                ChatText.stripFormatting(
                        "<col=ef1020>Wave 'The Maiden of Sugadinti' (Normal Mode) complete!<br>Duration: 1:23.40</col>"));
    }

    public void testStripsLeadingMacro() {
        assertEquals("Delve level: 1", ChatText.stripFormatting("@mes_hl_red@Delve level: 1"));
        assertEquals(
                "You resurrect a greater ghostly thrall.",
                ChatText.stripFormatting("@mes_hl_mag@You resurrect a greater ghostly thrall."));
    }

    public void testStripsInlineMacros() {
        assertEquals(
                "Delve level: 1 duration: 0:41.40. Personal best: 0:21.00",
                ChatText.stripFormatting(
                        "Delve level: 1 duration: @mes_hl_red@0:41.40. Personal best: @mes_hl_red@0:21.00"));
        assertEquals("Total duration: 0:41.40", ChatText.stripFormatting("Total duration: @mes_hl_red@0:41.40"));
        assertEquals(
                "You still have unclaimed loot! Are you sure you want to teleport away?",
                ChatText.stripFormatting(
                        "You still have @mes_hl_red@unclaimed loot! Are you sure you want to teleport away?"));
    }

    public void testStripsSemanticMacros() {
        assertEquals(
                "You have failed Darkness Is Your Ally?: You equipped a demonbane weapon.",
                ChatText.stripFormatting(
                        "You have failed @ach_comp@Darkness Is Your Ally?: You equipped a demonbane weapon."));
    }

    public void testLeavesUnformattedTextUnchanged() {
        assertEquals("Jagex breaks plugins", ChatText.stripFormatting("Jagex breaks plugins"));
    }

    public void testLeavesNonMacroAtUnchanged() {
        assertEquals("meet @ bank", ChatText.stripFormatting("meet @ bank"));
        assertEquals("support@jagex.com", ChatText.stripFormatting("support" + "@jagex.com"));
    }
}
