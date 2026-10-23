package wartech.unit;

import wartech.core.WarTech;

public final class UnitSpatialGrid {
    public static final int CELL_SIZE = 128; // 2 tiles (64 * 2)
    public static final int CELL_SHIFT = 7;  // 1 << 7 == 128
    public static final int MAX_CELLS = 8192; 
    public static final int MAX_UNITS = WarTech.MAX_UNITS;

    public final int[] head = new int[MAX_CELLS];
    public final int[] next = new int[MAX_UNITS];
    public final int[] touchedCells = new int[MAX_UNITS];
    public int touchedCount = 0;

    public int cols = 64;
    public int rows = 64;

    public final int[] queryResults = new int[MAX_UNITS];
    public int queryCount = 0;

    public UnitSpatialGrid() {
        for (int i = 0; i < MAX_CELLS; i++) {
            head[i] = -1;
        }
    }

    public void init(int mapWidthPx, int mapHeightPx) {
        this.cols = (mapWidthPx + CELL_SIZE - 1) >> CELL_SHIFT;
        this.rows = (mapHeightPx + CELL_SIZE - 1) >> CELL_SHIFT;
        if (cols < 1) cols = 1;
        if (rows < 1) rows = 1;
        for (int i = 0; i < MAX_CELLS; i++) {
            head[i] = -1;
        }
        touchedCount = 0;
    }

    public void clear() {
        for (int i = 0; i < touchedCount; i++) {
            head[touchedCells[i]] = -1;
        }
        touchedCount = 0;
    }

    public int getCellX(double x) {
        int cx = (int) x >> CELL_SHIFT;
        if (cx < 0) return 0;
        if (cx >= cols) return cols - 1;
        return cx;
    }

    public int getCellY(double y) {
        int cy = (int) y >> CELL_SHIFT;
        if (cy < 0) return 0;
        if (cy >= rows) return rows - 1;
        return cy;
    }

    public void insert(int unitIndex, double x, double y) {
        if (unitIndex < 0 || unitIndex >= MAX_UNITS) return;
        int cx = getCellX(x);
        int cy = getCellY(y);
        int cell = cy * cols + cx;
        if (cell >= 0 && cell < MAX_CELLS) {
            if (head[cell] == -1) {
                touchedCells[touchedCount++] = cell;
            }
            next[unitIndex] = head[cell];
            head[cell] = unitIndex;
        }
    }

    public void rebuild(Unit[] units, int unitCount) {
        clear();
        for (int i = 0; i < unitCount; i++) {
            Unit u = units[i];
            if (u != null && !u.isDestroyed()) {
                insert(i, u.x, u.y);
            }
        }
    }

    public int queryRange(double x, double y, double radius) {
        queryCount = 0;
        int minCx = getCellX(x - radius);
        int maxCx = getCellX(x + radius);
        int minCy = getCellY(y - radius);
        int maxCy = getCellY(y + radius);

        for (int cy = minCy; cy <= maxCy; cy++) {
            int rowOffset = cy * cols;
            for (int cx = minCx; cx <= maxCx; cx++) {
                int uIdx = head[rowOffset + cx];
                while (uIdx != -1) {
                    if (queryCount < MAX_UNITS) {
                        queryResults[queryCount++] = uIdx;
                    }
                    uIdx = next[uIdx];
                }
            }
        }
        return queryCount;
    }
}
