package org.example.model.helper;

import java.util.ArrayDeque;

public class SessionPathDeque<T> extends ArrayDeque<T> {
    @Override
    public void push(T item) {
        if (item == null) {
            return;
        }
        if (this.contains(item)) {
            while (!item.equals(this.peek())) {
                this.pop();
            }
        } else {
            super.push(item);
        }
    }

    public T peekSecond() {
        return this.size() > 1 ? this.stream().skip(1).findFirst().orElse(null) : null;
    }
}
