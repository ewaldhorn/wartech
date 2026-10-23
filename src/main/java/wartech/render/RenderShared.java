package wartech.render;

// Pixel helpers shared by the renderers

import wartech.building.BuildingType;
import wartech.core.Memory;
import wartech.core.Surface;
import wartech.unit.UnitType;
import static wartech.render.Renderer.*;

public final class RenderShared {
    private RenderShared() {
    }

    public static void ditherFill(Surface screen, int x0, int y0, int w, int h, int color) {
        int sx = Math.max(0, x0);
        int sy = Math.max(0, y0);
        int x1 = Math.min(SCREEN_WIDTH, x0 + w);
        int y1 = Math.min(SCREEN_HEIGHT, y0 + h);
        if (sx >= x1 || sy >= y1) return;
        int baseAddr = screen.base;
        int stride = screen.width;
        for (int y = sy; y < y1; y++) {
            int rowOffset = baseAddr + (y * stride + sx) * 4;
            for (int x = sx; x < x1; x++) {
                if (((x + y) & 1) == 0) Memory.put32(rowOffset, color);
                rowOffset += 4;
            }
        }
    }

    private static int blendOver(int dst, int src, int alpha) {
        int ia = 255 - alpha;
        int r = ((src & 0xFF) * alpha + (dst & 0xFF) * ia) / 255;
        int g = (((src >> 8) & 0xFF) * alpha + ((dst >> 8) & 0xFF) * ia) / 255;
        int b = (((src >> 16) & 0xFF) * alpha + ((dst >> 16) & 0xFF) * ia) / 255;
        return Palette.BLACK | (b << 16) | (g << 8) | r;
    }

    public static void blendRect(Surface screen, int x, int y, int w, int h, int color) {
        int alpha = (color >>> 24) & 0xFF;
        if (w <= 0 || h <= 0) return;
        int x0 = Math.max(0, x);
        int y0 = Math.max(0, y);
        int x1 = Math.min(SCREEN_WIDTH, x + w);
        int y1 = Math.min(SCREEN_HEIGHT, y + h);
        if (x0 >= x1 || y0 >= y1) return;
        if (alpha == 0) {
            screen.fillRect(x0, y0, x1 - x0, y1 - y0, color);
            return;
        }
        int baseAddr = screen.base;
        int stride = screen.width;
        for (int yy = y0; yy < y1; yy++) {
            int rowOffset = baseAddr + (yy * stride + x0) * 4;
            for (int xx = x0; xx < x1; xx++) {
                Memory.put32(rowOffset, blendOver(Memory.get32(rowOffset), color, alpha));
                rowOffset += 4;
            }
        }
    }

    public static void drawShadowRect(Surface screen, int x0, int y0, int w, int h) {
        blendRect(screen, x0, y0, w, h, 0x801A1410);
    }

    public static void drawShadowEllipse(Surface screen, int cx, int cy, int rx, int ry) {
        blendRect(screen, cx - rx, cy - ry, rx * 2, ry * 2, 0x801A1410);
    }

    public static void renderBuildingIcon(Surface s, BuildingType bt, int x, int y) {
        s.fillRect(x, y, 18, 18, 0xFF1C1814);
        s.drawLine(x, y, x + 17, y, Palette.UMBER);
        s.drawLine(x, y, x, y + 17, Palette.UMBER);
        s.drawLine(x + 17, y, x + 17, y + 17, Palette.BLACK_SHADOW);
        s.drawLine(x, y + 17, x + 17, y + 17, Palette.BLACK_SHADOW);

        switch (bt) {
            case WIND_TURBINE -> {
            s.fillRect(x + 7, y + 7, 4, 4, 0xFF00E0FF);
            s.drawLine(x + 3, y + 3, x + 7, y + 7, Palette.YELLOW_PALE);
            s.drawLine(x + 14, y + 3, x + 10, y + 7, Palette.YELLOW_PALE);
            s.drawLine(x + 3, y + 14, x + 7, y + 10, Palette.YELLOW_PALE);
            s.drawLine(x + 14, y + 14, x + 10, y + 10, Palette.YELLOW_PALE);
            }
            case REFINERY -> {
            s.fillRect(x + 3, y + 6, 12, 10, 0xFF184080);
            s.fillRect(x + 2, y + 3, 14, 4, 0xFF3060A0);
            s.fillRect(x + 7, y + 4, 4, 5, 0xFF40FF60);
            }
            case BARRACKS -> {
            s.fillRect(x + 3, y + 4, 12, 11, 0xFFFFA030);
            s.fillRect(x + 6, y + 10, 6, 5, 0xFF101010);
            s.drawLine(x + 14, y + 2, x + 14, y + 6, Palette.GRAY_LIGHT);
            s.setPixel(x + 14, y + 2, 0xFF2020FF);
            }
            case LIGHT_FACTORY -> {
            s.fillRect(x + 2, y + 4, 14, 11, 0xFF2A2E32);
            s.fillRect(x + 5, y + 8, 8, 7, Palette.GOLD_DARK);
            s.fillRect(x + 6, y + 10, 6, 4, Palette.WHITE);
            }
            case HEAVY_FACTORY -> {
            s.fillRect(x + 2, y + 3, 14, 12, 0xFF353535);
            s.fillRect(x + 5, y + 7, 8, 8, 0xFF101010);
            s.drawLine(x + 9, y + 11, x + 9, y + 17, Palette.GRAY_LIGHT);
            s.drawLine(x + 8, y + 11, x + 8, y + 17, Palette.GRAY_LIGHT);
            }
            case REPAIR_FACILITY -> {
            s.fillRect(x + 2, y + 4, 14, 11, 0xFF353028);
            s.fillRect(x + 4, y + 6, 10, 7, 0xFF181512);
            s.drawLine(x + 5, y + 5, x + 12, y + 12, Palette.BLUE_SKY);
            s.drawLine(x + 12, y + 5, x + 10, y + 7, Palette.BLUE_SKY);
            s.drawLine(x + 5, y + 12, x + 7, y + 10, Palette.BLUE_SKY);
            }
            case TURRET -> {
            s.fillRect(x + 4, y + 6, 10, 10, 0xFF4A4A4A);
            s.fillRect(x + 6, y + 4, 6, 6, 0xFF707070);
            s.drawLine(x + 11, y + 3, x + 15, y + 0, 0xFFD0D0D0);
            s.drawLine(x + 9, y + 2, x + 13, y + 0, 0xFFD0D0D0);
            }
            case RADAR -> {
            s.drawLine(x + 4, y + 15, x + 9, y + 8, 0xFF606060);
            s.drawLine(x + 14, y + 15, x + 9, y + 8, 0xFF606060);
            s.fillRect(x + 7, y + 3, 8, 6, Palette.GOLD);
            s.drawLine(x + 4, y + 2, x + 6, y + 4, Palette.GOLD_LIGHT);
            }
            case CON_YARD -> {
            s.fillRect(x + 3, y + 6, 12, 9, 0xFF00C0E0);
            s.fillRect(x + 5, y + 8, 8, 7, 0xFF151515);
            s.drawLine(x + 3, y + 3, x + 15, y + 3, Palette.GRAY_LIGHT);
            s.drawLine(x + 9, y + 3, x + 9, y + 6, Palette.GRAY_LIGHT);
            }
            case null, default -> { }
        }
    }

    public static void renderUnitIcon(Surface s, UnitType ut, int x, int y) {
        s.fillRect(x, y, 18, 18, 0xFF1C1814);
        s.drawLine(x, y, x + 17, y, Palette.UMBER);
        s.drawLine(x, y, x, y + 17, Palette.UMBER);
        s.drawLine(x + 17, y, x + 17, y + 17, Palette.BLACK_SHADOW);
        s.drawLine(x, y + 17, x + 17, y + 17, Palette.BLACK_SHADOW);

        switch (ut) {
            case TROOPER_RIFLE, TROOPER_MISSILE -> {
            s.fillRect(x + 7, y + 3, 4, 4, 0xFFFF9030);
            s.fillRect(x + 6, y + 7, 6, 6, 0xFF204060);
            s.drawLine(x + 12, y + 5, x + 15, y + 3, Palette.GRAY_LIGHT);
            }
            case MECHANIC -> {
            s.fillRect(x + 7, y + 3, 4, 4, Palette.BLUE_SKY);
            s.fillRect(x + 6, y + 7, 6, 6, 0xFF4A4030);
            s.drawLine(x + 11, y + 8, x + 15, y + 4, Palette.GOLD_LIGHT);
            }
            case MEDIC -> {
            s.fillRect(x + 7, y + 3, 4, 4, Palette.GRAY_LIGHT);
            s.fillRect(x + 6, y + 7, 6, 6, 0xFF2A4A3A);
            s.fillRect(x + 13, y + 5, 1, 5, Palette.GREEN);
            s.fillRect(x + 11, y + 7, 5, 1, Palette.GREEN);
            }
            case SCOUT_TRIKE -> {
            s.fillRect(x + 5, y + 5, 8, 7, 0xFFFF9030);
            s.fillRect(x + 8, y + 3, 2, 3, Palette.GRAY_DARK);
            s.fillRect(x + 3, y + 11, 3, 3, Palette.GRAY_DARK);
            s.fillRect(x + 12, y + 11, 3, 3, Palette.GRAY_DARK);
            }
            case COMBAT_TANK -> {
            s.fillRect(x + 3, y + 5, 12, 8, 0xFFFF8020);
            s.fillRect(x + 6, y + 4, 6, 5, 0xFF606060);
            s.drawLine(x + 9, y + 4, x + 15, y + 1, Palette.GRAY_LIGHT);
            }
            case SIEGE_TANK -> {
            s.fillRect(x + 2, y + 4, 14, 10, 0xFFFF8020);
            s.fillRect(x + 5, y + 5, 8, 6, 0xFF404040);
            s.drawLine(x + 7, y + 5, x + 14, y + 1, Palette.GRAY_LIGHT);
            s.drawLine(x + 9, y + 7, x + 16, y + 3, Palette.GRAY_LIGHT);
            }
            case COLLECTOR -> {
            s.fillRect(x + 3, y + 4, 12, 10, 0xFFFFA020);
            s.fillRect(x + 6, y + 7, 6, 5, 0xFF30D050);
            s.fillRect(x + 4, y + 13, 10, 3, 0xFF505050);
            }
            case RAIDER, VANDAL -> {
            s.fillRect(x + 7, y + 3, 4, 4, 0xFF94282A);
            s.fillRect(x + 6, y + 7, 6, 6, 0xFF3A2A2E);
            s.drawLine(x + 12, y + 5, x + 15, y + 3, 0xFFE0A0A0);
            }
            case REAVER_BUGGY -> {
            s.fillRect(x + 5, y + 5, 8, 7, 0xFF94282A);
            s.fillRect(x + 8, y + 3, 2, 3, 0xFFD0D0D0);
            s.fillRect(x + 3, y + 11, 3, 3, Palette.GRAY_DARK);
            s.fillRect(x + 12, y + 11, 3, 3, Palette.GRAY_DARK);
            }
            case RAVAGER_TANK -> {
            s.fillRect(x + 3, y + 5, 12, 8, 0xFF94282A);
            s.fillRect(x + 6, y + 4, 6, 5, 0xFF404040);
            s.drawLine(x + 9, y + 4, x + 15, y + 1, 0xFFFFB0A0);
            }
            case DREADNOUGHT -> {
            s.fillRect(x + 2, y + 4, 14, 10, 0xFF94282A);
            s.fillRect(x + 5, y + 5, 8, 6, 0xFF282828);
            s.drawLine(x + 7, y + 5, x + 14, y + 1, 0xFFFFB0A0);
            s.drawLine(x + 9, y + 7, x + 16, y + 3, 0xFFFFB0A0);
            }
            case null, default -> { }
        }
    }
}
