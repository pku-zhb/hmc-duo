package com.zhuhuibin.hypermusiccover.duo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

/** Bounded mixed-orientation vertical typography; measurements come from the actual font. */
final class VerticalPillText {
    enum Orientation { UPRIGHT, SIDEWAYS, COMBINED }
    static final class Run {
        final String text;
        final Orientation orientation;
        final float advance;
        Run(String text, Orientation orientation, float advance) {
            this.text = text; this.orientation = orientation; this.advance = advance;
        }
    }
    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    private static final Pattern SPACE = Pattern.compile("[\\s\\p{Z}]+");
    private static final int MAX_GRAPHEMES = 512;
    private VerticalPillText() {}

    static List<Run> runs(CharSequence source) {
        String value = source == null ? "" : SPACE.matcher(source).replaceAll(" ").trim();
        ArrayList<Run> out = new ArrayList<>();
        var matcher = GRAPHEME.matcher(value);
        StringBuilder horizontal = new StringBuilder();
        int count = 0;
        while (matcher.find()) {
            if (++count > MAX_GRAPHEMES) { flush(horizontal, out); out.add(new Run("︙", Orientation.UPRIGHT, 0)); break; }
            String glyph = matcher.group();
            int cp = glyph.codePointAt(0);
            String vertical = verticalPunctuation(glyph);
            if (upright(cp) || glyph.indexOf(0x20E3) >= 0 || !vertical.equals(glyph)) {
                flush(horizontal, out);
                out.add(new Run(vertical, Orientation.UPRIGHT, 0));
            } else horizontal.append(glyph);
        }
        flush(horizontal, out);
        return out;
    }

    private static void flush(StringBuilder horizontal, List<Run> out) {
        String text = horizontal.toString().trim();
        horizontal.setLength(0);
        if (text.isEmpty()) return;
        // Tate-chu-yoko: short numbers stay readable horizontally in one vertical cell.
        Orientation mode = text.matches("[0-9]{1,4}") ? Orientation.COMBINED : Orientation.SIDEWAYS;
        out.add(new Run(text, mode, 0));
    }

    static List<Run> layout(CharSequence text, float cell, float height, ToDoubleFunction<String> measure) {
        if (!Float.isFinite(cell) || !Float.isFinite(height) || cell <= 0 || height < cell) return List.of();
        List<Run> source = runs(text);
        ArrayList<Run> measured = new ArrayList<>();
        float total = 0;
        for (Run run : source) {
            float advance = run.orientation == Orientation.SIDEWAYS ? Math.max(cell, (float) measure.applyAsDouble(run.text)) : cell;
            if (!Float.isFinite(advance)) return List.of();
            measured.add(new Run(run.text, run.orientation, advance)); total += advance;
        }
        if (total <= height) return measured;
        ArrayList<Run> result = new ArrayList<>();
        float remaining = height - cell;
        for (Run run : measured) {
            if (run.advance <= remaining) {
                result.add(run); remaining -= run.advance; continue;
            }
            if (run.orientation == Orientation.SIDEWAYS && remaining >= cell) {
                var glyphs = GRAPHEME.matcher(run.text);
                String prefix = "";
                while (glyphs.find()) {
                    String candidate = run.text.substring(0, glyphs.end());
                    if (Math.max(cell, measure.applyAsDouble(candidate)) > remaining) break;
                    prefix = candidate;
                }
                // Prefer a complete word when truncation reached a later word.
                int space = prefix.lastIndexOf(' ');
                if (space > 0 && !prefix.endsWith(" ")) prefix = prefix.substring(0, space);
                prefix = prefix.stripTrailing();
                if (!prefix.isEmpty()) result.add(new Run(prefix, run.orientation,
                        Math.max(cell, (float) measure.applyAsDouble(prefix))));
            }
            break;
        }
        // Remove an opening bracket that would otherwise introduce only the truncation mark.
        if (!result.isEmpty() && "︵︷︹︻︽︿﹁﹃".contains(result.get(result.size()-1).text))
            result.remove(result.size()-1);
        result.add(new Run("︙", Orientation.UPRIGHT, cell));
        return result;
    }

    private static boolean upright(int cp) {
        Character.UnicodeScript script = Character.UnicodeScript.of(cp);
        return script == Character.UnicodeScript.HAN || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA || script == Character.UnicodeScript.HANGUL
                || cp >= 0xFF01 && cp <= 0xFF60 || cp >= 0x1F000
                || Character.getType(cp) == Character.OTHER_SYMBOL || cp >= 0xFE10 && cp <= 0xFE48;
    }

    private static String verticalPunctuation(String glyph) {
        return switch (glyph) {
            case "、" -> "︑"; case "。" -> "︒"; case "，" -> "︐";
            case "：" -> "︓"; case "；" -> "︔"; case "！" -> "︕"; case "？" -> "︖";
            case "（" -> "︵"; case "）" -> "︶"; case "【" -> "︻"; case "】" -> "︼";
            case "《" -> "︽"; case "》" -> "︾"; case "〈" -> "︿"; case "〉" -> "﹀";
            case "「", "“" -> "﹁"; case "」", "”" -> "﹂";
            case "『", "‘" -> "﹃"; case "』", "’" -> "﹄";
            case "…" -> "︙"; case "—" -> "︱";
            default -> glyph;
        };
    }
}
