package wartech.core;

import java.util.Arrays;

public final class Memory {
    private static final int BUFFER_SIZE = Display.WIDTH * Display.HEIGHT;
    public static final int[] HEAP = new int[BUFFER_SIZE];

    public static int base() {
        return 0;
    }

    public static int get32(int addr) {
        int idx = addr >> 2;
        if (idx >= 0 && idx < HEAP.length) {
            return HEAP[idx];
        }
        return 0;
    }

    public static void put32(int addr, int val) {
        int idx = addr >> 2;
        if (idx >= 0 && idx < HEAP.length) {
            HEAP[idx] = val;
        }
    }

    public static void fill32(int addr, int count, int val) {
        int start = addr >> 2;
        int end = Math.min(HEAP.length, start + count);
        if (start < end && start >= 0) {
            Arrays.fill(HEAP, start, end, val);
        }
    }
}
