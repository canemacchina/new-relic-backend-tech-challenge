package it.lorenzobugiani.client;

import java.util.Arrays;

public class Buffer {

    private final int[] buffer;
    private int idx;

    public Buffer(int size) {
        this.buffer = new int[size];
        this.idx = 0;
    }

    public void put(int item) {
        this.buffer[idx] = item;
        idx++;
    }

    public int[] getElements() {
        return Arrays.copyOf(buffer, idx);
    }

    public int[] flush() {
        var ret = getElements();
        this.idx = 0;
        return ret;
    }

    public boolean isFull() {
        return idx == buffer.length;
    }

    public boolean isEmpty() {
        return idx == 0;
    }
}
