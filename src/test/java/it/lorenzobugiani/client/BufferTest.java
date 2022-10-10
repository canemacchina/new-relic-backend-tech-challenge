package it.lorenzobugiani.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BufferTest {

    @Test
    @DisplayName("A new buffer is empty")
    public void isEmpty() {
        var buffer = new Buffer(10);

        assertThat(buffer.isEmpty()).isTrue();
        assertThat(buffer.getElements()).isEmpty();
    }

    @Test
    @DisplayName("Check not empty buffer")
    public void isNotEmpty() {
        var buffer = new Buffer(10);
        buffer.put(1);

        assertThat(buffer.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("Add elements")
    public void addElements() {
        var buffer = new Buffer(10);
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);

        assertThat(buffer.getElements()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Flush")
    public void flush() {
        var buffer = new Buffer(10);
        buffer.put(1);
        buffer.put(2);
        buffer.put(3);

        var elements = buffer.flush();

        assertThat(elements).containsExactly(1, 2, 3);
        assertThat(buffer.isEmpty()).isTrue();
        assertThat(buffer.isFull()).isFalse();
    }

    @Test
    @DisplayName("Check if buffer is full")
    public void isFull() {
        var buffer = new Buffer(3);
        buffer.put(1);
        buffer.put(2);

        assertThat(buffer.isFull()).isFalse();

        buffer.put(3);

        assertThat(buffer.isFull()).isTrue();
    }

    @Test
    @DisplayName("Throw an exception if add more elements than the capacity")
    public void error() {
        var buffer = new Buffer(1);
        buffer.put(1);

        assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
                .isThrownBy(() -> buffer.put(2));
    }
}