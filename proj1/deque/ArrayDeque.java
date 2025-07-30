package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Iterable<T>, Deque<T> {

    private int nextLast;
    private int nextFirst;
    private int size;
    private T[] item;

    public ArrayDeque() {
        size = 0;
        item = (T[]) new Object[8];
        nextFirst = 0;
        nextLast = 1;
    }

    public void addFirst(T x) {
        this.item[nextFirst] = x;
        nextFirst = precede(nextFirst);
        size++;

        if (size == item.length) { resize(size * 2) ; }
    }

    public void addLast(T x) {
        this.item[nextLast] = x;
        nextLast = goBackward(nextLast);
        size++;

        if (size == item.length) { resize(size * 2); }
    }

    public T removeFirst() {
        if (size == 0) {
            return null;
        }

        T resultItem = this.item[goBackward(nextFirst)];
        item[goBackward(nextFirst)] = null;
        nextFirst = goBackward(nextFirst);
        size--;

        if (size > 8 && size < item.length / 4) {
            resize(item.length / 4);
        }
        return resultItem;
    }

    public T removeLast() {
        if (size == 0) {
            return null;
        }

        T resultItem = this.item[precede(nextLast)];
        item[precede(nextLast)] = null;
        nextLast = precede(nextLast);
        size--;

        if (size > 8 && size < item.length / 4) {
            resize(item.length / 4);
        }
        return  resultItem;
    }

    private void resize(int capacity) {
        T [] newItems = (T[]) new Object[capacity];
        int reader = goBackward(nextFirst);

        for (int i = 0; i < size; i++) {
            newItems[i] = item[reader];
            reader = goBackward(reader);
        }

        item = newItems;
        nextFirst = capacity - 1;
        nextLast = size;
    }

    private int goBackward(int index) {
        if (index == item.length - 1) {
            return 0;
        } else {
            return index + 1;
        }
    }

    private int precede(int index) {
        if (index == 0) {
            return item.length-1;
        }
        return index - 1;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        int reader = goBackward(nextFirst);
        while (reader != nextLast) {
            System.out.print(item[reader]);
            System.out.print(' ');
            reader = goBackward(reader);
        }

        System.out.println();
    }


    public T get(int index) {
        if (index > size - 1 || index < 0) {
            return null;
        }
        int start = goBackward(nextFirst);
        return item[(start+index) % (item.length)];

    }

    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {

        int reader = goBackward(nextFirst);

        @Override
        public boolean hasNext() {
            return reader != nextLast;
        }

        @Override
        public T next() {
            T toBeReturn = item[reader];
            reader = goBackward(reader);
            return toBeReturn;
        }
    }

    @Override
    public boolean equals(Object o) {
        // 1. 检查对象是否为自身
        if (this == o) {
            return true;
        }

        if (!(o instanceof Deque)) {
            return false;
        }

        Deque<T> other = (Deque<T>) o;
        if (this.size() != other.size()) {
            return false;
        }

        // 4. 逐个比较元素，依赖公开的 get() 方法
        for (int i = 0; i < this.size(); i++) {
            if (!java.util.Objects.equals(this.get(i), other.get(i))) {
                return false;
            }
        }

        return true;
    }
}
