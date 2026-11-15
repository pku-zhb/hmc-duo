package com.zhuhuibin.hypermusiccover.duo;

import java.util.List;
import java.util.function.ToDoubleFunction;
import org.junit.Test;
import static org.junit.Assert.*;

public class VerticalPillTextTest {
    private final ToDoubleFunction<String> width = s -> s.codePointCount(0, s.length()) * 5.0;
    private String joined(List<VerticalPillText.Run> runs) {
        return String.join("|", runs.stream().map(r -> r.text).toList());
    }
    @Test public void chineseIsUprightAndEnglishWordsStayTogether() {
        var runs = VerticalPillText.runs("夜曲 Love Story 周杰伦");
        assertEquals("夜|曲|Love Story|周|杰|伦", joined(runs));
        assertEquals(VerticalPillText.Orientation.UPRIGHT, runs.get(0).orientation);
        assertEquals(VerticalPillText.Orientation.SIDEWAYS, runs.get(2).orientation);
    }
    @Test public void shortNumbersUseHorizontalInVerticalComposition() {
        var runs = VerticalPillText.runs("还有12分钟，2026年");
        assertEquals("还|有|12|分|钟|︐|2026|年", joined(runs));
        assertEquals(VerticalPillText.Orientation.COMBINED, runs.get(2).orientation);
        assertEquals(VerticalPillText.Orientation.COMBINED, runs.get(6).orientation);
        assertEquals(VerticalPillText.Orientation.SIDEWAYS, VerticalPillText.runs("12345").get(0).orientation);
    }
    @Test public void punctuationUsesVerticalForms() {
        assertEquals("︽|夜|曲|︾|︵|现|场|︶|︕|︙", joined(VerticalPillText.runs("《夜曲》（现场）！…")));
    }
    @Test public void extendedEmojiAndAccentClustersSurviveTruncation() {
        assertEquals("👨‍👩‍👧|🇨🇳|👍🏽|1️⃣", joined(VerticalPillText.runs("👨‍👩‍👧🇨🇳👍🏽1️⃣")));
        assertEquals("👨‍👩‍👧|︙", joined(VerticalPillText.layout("👨‍👩‍👧🇨🇳好", 10, 20, width)));
        assertEquals("e\u0301", VerticalPillText.runs("e\u0301").get(0).text);
    }
    @Test public void measuredEnglishRunsUseRealAvailableHeight() {
        assertEquals("Love|︙", joined(VerticalPillText.layout("Love Story tonight", 10, 40, width)));
        assertEquals("Love Story", joined(VerticalPillText.layout("Love Story", 10, 50, width)));
    }
    @Test public void chineseTruncationReservesAVerticalEllipsisCell() {
        var runs = VerticalPillText.layout("这是很长的消息", 10, 35, width);
        assertEquals("这|是|︙", joined(runs));
        assertTrue(runs.stream().mapToDouble(r -> r.advance).sum() <= 35);
        assertEquals("︙", joined(VerticalPillText.layout("很长", 10, 10, width)));
    }
    @Test public void truncatedOpeningBracketDoesNotDangle() {
        assertEquals("︙", joined(VerticalPillText.layout("《长歌标题》", 10, 20, width)));
    }
    @Test public void arabicIsKeptAsAShapedRunAndWhitespaceIsBounded() {
        assertEquals("مرحبا بالعالم", joined(VerticalPillText.runs("مرحبا بالعالم")));
        assertEquals("Hello world", joined(VerticalPillText.runs("  Hello\n\tworld  ")));
        assertTrue(VerticalPillText.runs(null).isEmpty());
        assertTrue(VerticalPillText.layout("文字", 10, 9, width).isEmpty());
        assertTrue(VerticalPillText.layout("文字", Float.NaN, 50, width).isEmpty());
    }
    @Test public void uprightCjkUsesAnEmWithoutRepeatingHorizontalLineRoom() {
        float advance = VerticalPillText.uprightAdvance(20, -20, 6, -17, 1);
        assertEquals(20f, advance, 0f);
        var compact = VerticalPillText.layout("通知内容", 20, 80, width, ignored -> advance);
        assertEquals("通|知|内|容", joined(compact));
        assertEquals("通|知|︙", joined(VerticalPillText.layout("通知内容", 26, 80, width)));
    }
    @Test public void tallFallbackInkKeepsItsOwnRoomWithoutExpandingAdjacentCjk() {
        float cjk = VerticalPillText.uprightAdvance(20, -20, 6, -17, 1);
        float emoji = VerticalPillText.uprightAdvance(20, -20, 6, -25, 4);
        var runs = VerticalPillText.layout("中👨‍👩‍👧文", 20, 100, width,
                glyph -> glyph.equals("👨‍👩‍👧") ? emoji : cjk);
        assertEquals("中|👨‍👩‍👧|文", joined(runs));
        assertEquals(20f, runs.get(0).advance, 0f);
        assertEquals(36f, runs.get(1).advance, 0f);
        assertEquals(20f, runs.get(2).advance, 0f);
        // Drawing still centers the font's baseline, so both ends of fallback ink must fit.
        float baseline = emoji / 2f - (-20 + 6) / 2f;
        assertTrue(baseline - 25 >= 0);
        assertTrue(baseline + 4 <= emoji);
    }
    @Test public void compactPitchRetainsWholeShapedRunsAndCombinedNumbers() {
        var runs = VerticalPillText.layout("中12مرحبا بالعالم文", 20, 200,
                text -> text.equals("12مرحبا بالعالم") ? 95 : 20, ignored -> 20);
        assertEquals("中|12مرحبا بالعالم|文", joined(runs));
        assertEquals(VerticalPillText.Orientation.SIDEWAYS, runs.get(1).orientation);
        assertEquals(95f, runs.get(1).advance, 0f);
        runs = VerticalPillText.layout("中12文", 20, 60, width, ignored -> 20);
        assertEquals("中|12|文", joined(runs));
        assertEquals(VerticalPillText.Orientation.COMBINED, runs.get(1).orientation);
    }
    @Test public void measuredVerticalEllipsisFitsItsInkAtTheTruncatedEnd() {
        var runs = VerticalPillText.layout("通知内容", 20, 50, width,
                glyph -> glyph.equals("︙") ? 28 : 20);
        assertEquals("通|︙", joined(runs));
        assertEquals(28f, runs.get(1).advance, 0f);
        assertTrue(runs.stream().mapToDouble(run -> run.advance).sum() <= 50);
    }
    @Test public void combiningMarksStayInTheirUprightGraphemeAndKeepInkRoom() {
        var runs = VerticalPillText.layout("中\u0301文", 20, 60, width,
                glyph -> glyph.equals("中\u0301") ? 34 : 20);
        assertEquals("中\u0301|文", joined(runs));
        assertEquals(34f, runs.get(0).advance, 0f);
        assertTrue(Float.isNaN(VerticalPillText.uprightAdvance(20, -20, 6, 1, -17)));
        assertTrue(VerticalPillText.layout("中文", 20, 50, width, ignored -> Double.NaN).isEmpty());
    }
}
