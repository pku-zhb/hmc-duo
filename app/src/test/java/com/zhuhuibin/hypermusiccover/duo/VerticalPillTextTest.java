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
}
