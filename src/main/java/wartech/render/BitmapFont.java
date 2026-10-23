package wartech.render;

import wartech.core.Surface;

public final class BitmapFont {
    public static final int CHAR_WIDTH = 5;
    public static final int CHAR_HEIGHT = 7;
    public static final int SPACING = 1;

    private static long pack(int r0, int r1, int r2, int r3, int r4, int r5, int r6) {
        return (((long) r0 & 0x1FL) << 30) |
               (((long) r1 & 0x1FL) << 25) |
               (((long) r2 & 0x1FL) << 20) |
               (((long) r3 & 0x1FL) << 15) |
               (((long) r4 & 0x1FL) << 10) |
               (((long) r5 & 0x1FL) << 5)  |
               ((long) r6 & 0x1FL);
    }

    public static long getGlyph(char c) {
        if (c >= 'a' && c <= 'z') c = (char) (c - 32);
        switch (c) {
            case '0': return pack(14, 17, 19, 21, 25, 17, 14);
            case '1': return pack(4, 12, 4, 4, 4, 4, 14);
            case '2': return pack(14, 17, 1, 6, 8, 16, 31);
            case '3': return pack(14, 17, 1, 6, 1, 17, 14);
            case '4': return pack(17, 17, 17, 31, 1, 1, 1);
            case '5': return pack(31, 16, 30, 1, 1, 17, 14);
            case '6': return pack(14, 16, 16, 30, 17, 17, 14);
            case '7': return pack(31, 1, 2, 4, 8, 8, 8);
            case '8': return pack(14, 17, 17, 14, 17, 17, 14);
            case '9': return pack(14, 17, 17, 15, 1, 1, 14);

            case 'A': return pack(14, 17, 17, 31, 17, 17, 17);
            case 'B': return pack(30, 17, 17, 30, 17, 17, 30);
            case 'C': return pack(15, 16, 16, 16, 16, 16, 15);
            case 'D': return pack(30, 17, 17, 17, 17, 17, 30);
            case 'E': return pack(31, 16, 16, 30, 16, 16, 31);
            case 'F': return pack(31, 16, 16, 30, 16, 16, 16);
            case 'G': return pack(15, 16, 16, 23, 17, 17, 15);
            case 'H': return pack(17, 17, 17, 31, 17, 17, 17);
            case 'I': return pack(14, 4, 4, 4, 4, 4, 14);
            case 'J': return pack(1, 1, 1, 1, 17, 17, 14);
            case 'K': return pack(17, 18, 20, 24, 20, 18, 17);
            case 'L': return pack(16, 16, 16, 16, 16, 16, 31);
            case 'M': return pack(17, 27, 21, 21, 17, 17, 17);
            case 'N': return pack(17, 25, 25, 21, 19, 19, 17);
            case 'O': return pack(14, 17, 17, 17, 17, 17, 14);
            case 'P': return pack(30, 17, 17, 30, 16, 16, 16);
            case 'Q': return pack(14, 17, 17, 17, 21, 18, 13);
            case 'R': return pack(30, 17, 17, 30, 20, 18, 17);
            case 'S': return pack(15, 16, 16, 14, 1, 1, 30);
            case 'T': return pack(31, 4, 4, 4, 4, 4, 4);
            case 'U': return pack(17, 17, 17, 17, 17, 17, 14);
            case 'V': return pack(17, 17, 17, 17, 10, 10, 4);
            case 'W': return pack(17, 17, 17, 21, 21, 27, 17);
            case 'X': return pack(17, 10, 4, 4, 4, 10, 17);
            case 'Y': return pack(17, 17, 10, 4, 4, 4, 4);
            case 'Z': return pack(31, 1, 2, 4, 8, 16, 31);

            case ' ': return 0L;
            case '$': return pack(4, 15, 20, 14, 5, 30, 4);
            case '+': return pack(0, 4, 4, 31, 4, 4, 0);
            case '-': return pack(0, 0, 0, 31, 0, 0, 0);
            case '*': return pack(0, 17, 10, 31, 10, 17, 0);
            case '/': return pack(1, 2, 2, 4, 8, 8, 16);
            case '=': return pack(0, 0, 31, 0, 31, 0, 0);
            case ':': return pack(0, 12, 12, 0, 12, 12, 0);
            case '.': return pack(0, 0, 0, 0, 0, 12, 12);
            case ',': return pack(0, 0, 0, 0, 0, 12, 8);
            case '!': return pack(4, 4, 4, 4, 4, 0, 4);
            case '?': return pack(14, 17, 1, 2, 4, 0, 4);
            case '(': return pack(2, 4, 8, 8, 8, 4, 2);
            case ')': return pack(8, 4, 2, 2, 2, 4, 8);
            case '[': return pack(14, 8, 8, 8, 8, 8, 14);
            case ']': return pack(14, 2, 2, 2, 2, 2, 14);
            case '<': return pack(2, 4, 8, 16, 8, 4, 2);
            case '>': return pack(8, 4, 2, 1, 2, 4, 8);
            case '%': return pack(17, 18, 4, 4, 4, 9, 17);
            case '_': return pack(0, 0, 0, 0, 0, 0, 31);
            case '#': return pack(10, 10, 31, 10, 31, 10, 10);
            case '@': return pack(14, 17, 21, 21, 22, 16, 15);
            case '&': return pack(12, 18, 20, 8, 21, 18, 13);
            case '\'': return pack(4, 4, 8, 0, 0, 0, 0);
            case '"': return pack(10, 10, 10, 0, 0, 0, 0);
            default:  return 0L;
        }
    }

    public static final int CHAR_WIDTH_LG = 6;
    public static final int CHAR_HEIGHT_LG = 9;
    public static final int SPACING_LG = 1;

    private static final int[] ROW_MAP_LG = {0, 1, 2, 2, 3, 4, 4, 5, 6};
    private static final int[] COL_MAP_LG = {0, 1, 2, 2, 3, 4};

    public static final int CHAR_WIDTH_XL = 8;
    public static final int CHAR_HEIGHT_XL = 11;
    public static final int SPACING_XL = 1;

    private static final int[] ROW_MAP_XL = {0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6};
    private static final int[] COL_MAP_XL = {0, 1, 1, 2, 2, 3, 3, 4};

    public static int getTextWidth(String text) {
        if (text == null) return 0;
        return text.length() * (CHAR_WIDTH + SPACING);
    }

    public static int getTextLargeWidth(String text) {
        if (text == null) return 0;
        return text.length() * (CHAR_WIDTH_LG + SPACING_LG);
    }

    public static int getTextXLWidth(String text) {
        if (text == null) return 0;
        return text.length() * (CHAR_WIDTH_XL + SPACING_XL);
    }

    private static void blitGlyph(Surface s, long glyph, int x, int y, int color, int scale,
                                  int rows, int cols, int[] rowMap, int[] colMap) {
        for (int row = 0; row < rows; row++) {
            int srcRow = rowMap == null ? row : rowMap[row];
            for (int col = 0; col < cols; col++) {
                int srcCol = colMap == null ? col : colMap[col];
                int shift = 34 - (srcRow * 5 + srcCol);
                if (((glyph >> shift) & 1L) == 1L) {
                    if (scale <= 1) {
                        s.setPixel(x + col, y + row, color);
                    } else {
                        s.fillRect(x + col * scale, y + row * scale, scale, scale, color);
                    }
                }
            }
        }
    }

    private static void blitGlyphLG(Surface s, char c, int x, int y, int color, int scale) {
        long glyph = getGlyph(c);
        if (glyph == 0L) return;
        blitGlyph(s, glyph, x, y, color, scale, CHAR_HEIGHT_LG, CHAR_WIDTH_LG, ROW_MAP_LG, COL_MAP_LG);
    }

    private static void blitGlyphXL(Surface s, char c, int x, int y, int color, int scale) {
        long glyph = getGlyph(c);
        if (glyph == 0L) return;
        blitGlyph(s, glyph, x, y, color, scale, CHAR_HEIGHT_XL, CHAR_WIDTH_XL, ROW_MAP_XL, COL_MAP_XL);
    }

    public static void drawGlyph(Surface s, char c, int x, int y, int color, int scale) {
        long glyph = getGlyph(c);
        if (glyph == 0L) return;
        blitGlyph(s, glyph, x, y, color, scale, CHAR_HEIGHT, CHAR_WIDTH, null, null);
    }

    public static int intWidth(int value, int scale) {
        int digits = 1;
        if (value < 0) {
            digits++;
            value = -value;
        }
        while (value >= 10) {
            value /= 10;
            digits++;
        }
        return digits * (CHAR_WIDTH + SPACING) * scale;
    }

    public static void drawInt(Surface s, int value, int startX, int startY, int color, int scale) {
        int advance = (CHAR_WIDTH + SPACING) * scale;
        int x = startX;
        if (value < 0) {
            drawGlyph(s, '-', x, startY, color, scale);
            x += advance;
            value = -value;
        }
        int divisor = 1;
        while (value / divisor >= 10) divisor *= 10;
        for (; divisor > 0; divisor /= 10) {
            drawGlyph(s, (char) ('0' + (value / divisor) % 10), x, startY, color, scale);
            x += advance;
        }
    }

    public static void drawText(Surface s, String text, int startX, int startY, int color) {
        drawText(s, text, startX, startY, color, 1);
    }

    public static int drawLine(Surface s, String prefix, int value, String suffix,
                               int startX, int startY, int color) {
        int x = startX;
        if (prefix != null && !prefix.isEmpty()) {
            drawText(s, prefix, x, startY, color, 1);
            x += getTextWidth(prefix);
        }
        if (value != NO_VALUE) {
            drawInt(s, value, x, startY, color, 1);
            x += intWidth(value, 1);
        }
        if (suffix != null && !suffix.isEmpty()) {
            drawText(s, suffix, x, startY, color, 1);
            x += getTextWidth(suffix);
        }
        return x;
    }

    public static void drawTextWithShadow(Surface s, String text, int startX, int startY, int color, int scale) {
        drawText(s, text, startX + scale, startY + scale, Palette.BLACK, scale);
        drawText(s, text, startX, startY, color, scale);
    }

    public static void drawText(Surface s, String text, int startX, int startY, int color, int scale) {
        if (text == null || text.isEmpty()) return;
        int advance = (CHAR_WIDTH + SPACING) * scale;
        int lineAdvance = (CHAR_HEIGHT + 3) * scale;
        int x = startX;
        int y = startY;
        for (int i = 0, len = text.length(); i < len; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                x = startX;
                y += lineAdvance;
                continue;
            }
            drawGlyph(s, c, x, y, color, scale);
            x += advance;
        }
    }

    public static void drawTextLarge(Surface s, String text, int startX, int startY, int color) {
        drawTextLarge(s, text, startX, startY, color, 1);
    }

    public static void drawTextLarge(Surface s, String text, int startX, int startY, int color, int scale) {
        if (text == null || text.isEmpty()) return;
        int advance = (CHAR_WIDTH_LG + SPACING_LG) * scale;
        int lineAdvance = (CHAR_HEIGHT_LG + 3) * scale;
        int x = startX;
        int y = startY;
        for (int i = 0, len = text.length(); i < len; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                x = startX;
                y += lineAdvance;
                continue;
            }
            blitGlyphLG(s, c, x, y, color, scale);
            x += advance;
        }
    }

    public static final int NO_VALUE = Integer.MIN_VALUE;

    private static void drawGlyphLarge(Surface s, char c, int x, int y, int color) {
        long glyph = getGlyph(c);
        if (glyph == 0L) return;
        for (int row = 0; row < CHAR_HEIGHT_LG; row++) {
            int srcRow = ROW_MAP_LG[row];
            for (int col = 0; col < CHAR_WIDTH_LG; col++) {
                int shift = 34 - (srcRow * 5 + COL_MAP_LG[col]);
                if (((glyph >> shift) & 1L) == 1L) s.setPixel(x + col, y + row, color);
            }
        }
    }

    public static void drawLargeLine(Surface s, String prefix, int value, String suffix, int startX, int startY, int color) {
        int advance = CHAR_WIDTH_LG + SPACING_LG;
        int x = startX;
        if (prefix != null) {
            drawTextLarge(s, prefix, x, startY, color, 1);
            x += prefix.length() * advance;
        }
        if (value != NO_VALUE) {
            if (value < 0) {
                drawGlyphLarge(s, '-', x, startY, color);
                x += advance;
                value = -value;
            }
            int divisor = 1;
            while (value / divisor >= 10) divisor *= 10;
            for (; divisor > 0; divisor /= 10) {
                drawGlyphLarge(s, (char) ('0' + (value / divisor) % 10), x, startY, color);
                x += advance;
            }
        }
        if (suffix != null) drawTextLarge(s, suffix, x, startY, color, 1);
    }

    public static void drawTextXL(Surface s, String text, int startX, int startY, int color) {
        drawTextXL(s, text, startX, startY, color, 1);
    }

    public static void drawTextXL(Surface s, String text, int startX, int startY, int color, int scale) {
        if (text == null || text.isEmpty()) return;
        int advance = (CHAR_WIDTH_XL + SPACING_XL) * scale;
        int lineAdvance = (CHAR_HEIGHT_XL + 3) * scale;
        int x = startX;
        int y = startY;
        for (int i = 0, len = text.length(); i < len; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                x = startX;
                y += lineAdvance;
                continue;
            }
            blitGlyphXL(s, c, x, y, color, scale);
            x += advance;
        }
    }

    public static int xlIntWidth(int value) {
        int digits = 1;
        if (value < 0) {
            digits++;
            value = -value;
        }
        while (value >= 10) {
            value /= 10;
            digits++;
        }
        return digits * (CHAR_WIDTH_XL + SPACING_XL);
    }

    public static void drawIntXL(Surface s, int value, int startX, int startY, int color) {
        int advance = CHAR_WIDTH_XL + SPACING_XL;
        int x = startX;
        if (value < 0) {
            blitGlyphXL(s, '-', x, startY, color, 1);
            x += advance;
            value = -value;
        }
        int divisor = 1;
        while (value / divisor >= 10) divisor *= 10;
        for (; divisor > 0; divisor /= 10) {
            blitGlyphXL(s, (char) ('0' + (value / divisor) % 10), x, startY, color, 1);
            x += advance;
        }
    }

    public static int drawLineXL(Surface s, String prefix, int value, String suffix,
                                 int startX, int startY, int color) {
        int x = startX;
        if (prefix != null && !prefix.isEmpty()) {
            drawTextXL(s, prefix, x, startY, color);
            x += getTextXLWidth(prefix);
        }
        if (value != NO_VALUE) {
            drawIntXL(s, value, x, startY, color);
            x += xlIntWidth(value);
        }
        if (suffix != null && !suffix.isEmpty()) {
            drawTextXL(s, suffix, x, startY, color);
            x += getTextXLWidth(suffix);
        }
        return x;
    }

    public static int getWrappedLineCountXL(String text, int maxWidth) {
        if (text == null || text.isEmpty()) return 1;
        int charAdvance = CHAR_WIDTH_XL + SPACING_XL; // 9px
        int maxChars = Math.max(1, maxWidth / charAdvance);
        int lines = 1;
        int col = 0;
        int i = 0;
        int len = text.length();
        while (i < len) {
            int wordEnd = i;
            while (wordEnd < len && text.charAt(wordEnd) != ' ' && text.charAt(wordEnd) != '\n') wordEnd++;
            int wordLen = wordEnd - i;
            if (col + wordLen > maxChars && col > 0) {
                lines++;
                col = 0;
            }
            col += wordLen;
            if (wordEnd < len) {
                char sep = text.charAt(wordEnd);
                if (sep == '\n') {
                    lines++;
                    col = 0;
                } else if (col + 1 <= maxChars) {
                    col++;
                } else {
                    lines++;
                    col = 0;
                }
                i = wordEnd + 1;
            } else {
                i = wordEnd;
            }
        }
        return lines;
    }

    public static int drawTextXLWrapped(Surface s, String text, int startX, int startY, int maxWidth, int color) {
        return drawTextXLWrapped(s, text, startX, startY, maxWidth, color, Integer.MAX_VALUE);
    }

    public static int drawTextXLWrapped(Surface s, String text, int startX, int startY, int maxWidth, int color, int maxChars) {
        if (text == null) return startY;
        int curX = startX;
        int curY = startY;
        int charAdvance = CHAR_WIDTH_XL + SPACING_XL; // 9px
        int lineAdvance = CHAR_HEIGHT_XL + 4; // 15px
        int len = Math.min(text.length(), Math.max(0, maxChars));
        int wordStart = 0;
        while (wordStart < len) {
            int wordEnd = wordStart;
            while (wordEnd < len && text.charAt(wordEnd) != ' ' && text.charAt(wordEnd) != '\n') {
                wordEnd++;
            }
            int wordLen = wordEnd - wordStart;
            int wordPx = wordLen * charAdvance;
            if (curX + wordPx > startX + maxWidth && curX > startX) {
                curY += lineAdvance;
                curX = startX;
            }
            for (int i = wordStart; i < wordEnd; i++) {
                blitGlyphXL(s, text.charAt(i), curX, curY, color, 1);
                curX += charAdvance;
            }
            if (wordEnd < len) {
                char sep = text.charAt(wordEnd);
                if (sep == '\n') {
                    curY += lineAdvance;
                    curX = startX;
                } else if (sep == ' ') {
                    if (curX + charAdvance <= startX + maxWidth) {
                        curX += charAdvance;
                    } else {
                        curY += lineAdvance;
                        curX = startX;
                    }
                }
                wordStart = wordEnd + 1;
            } else {
                wordStart = wordEnd;
            }
        }
        return curY + lineAdvance;
    }
}
