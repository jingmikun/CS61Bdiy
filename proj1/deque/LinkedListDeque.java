package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Iterable<T>,Deque<T> {
    private IntNode sentinel;
    private int size;

    public class IntNode {
        public T first;
        public IntNode next;
        public IntNode prev;

        private IntNode(T item, IntNode n,IntNode p) {
            first = item;
            next = n;
            prev = p;
        }
    }

    public LinkedListDeque() {
        sentinel = new IntNode(null,null,null);
        sentinel.next = sentinel;
        sentinel.prev = sentinel;
        size = 0;
    }

    public void addFirst(T item) {
        IntNode remain = sentinel.next;
        IntNode newNode = new IntNode(item,remain,sentinel);

        size++;
        sentinel.next = newNode;
        remain.prev = newNode;
        if (size == 1){sentinel.prev = newNode;}
    }

    public void addLast(T item) {
        IntNode remain = sentinel.prev;
        IntNode newNode = new IntNode(item,sentinel,remain);

        size++;
        sentinel.prev = newNode;
        remain.next = newNode;
        if (size == 1) { sentinel.next = newNode; }
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        IntNode p = sentinel.next;

        while (p != sentinel) {
            System.out.print(p.first + " ");
            p = p.next;
        }
        System.out.println();
    }

    public T removeFirst() {
        if (size == 0) {
            return null;
        }
        T returnValue = sentinel.next.first;
        sentinel.next = sentinel.next.next;
        sentinel.next.prev = sentinel;
        size--;
        return returnValue;
    }

    public T removeLast() {
        if (size == 0) {
            return null;
        }
        T returnValue = sentinel.prev.first;
        sentinel.prev = sentinel.prev.prev;
        sentinel.prev.next = sentinel;
        size--;
        return returnValue;
    }

    public T get(int index) {
        IntNode p = sentinel;
        int counter = index;

        if (index > size - 1 ) {
            return null;
        }
        while (counter != -1) {
            p = p.next;
            counter--;
        }

        return p.first;
    }

    public T getRecursive(int index){
        if (index > size - 1) {
            return null;
        }
        return getRecursive(index,sentinel.next);
    }

    private T getRecursive(int index,IntNode start){
        if (index == 0) {
            return start.first;
        }
        else{
            return getRecursive(index - 1,start.next);
        }
    }

    public Iterator<T> iterator() {
        return new LinkedListIterator();
    }

    private class LinkedListIterator implements Iterator<T> {

        IntNode p = sentinel.next;
        @Override
        public boolean hasNext() {
            return p != sentinel;
        }

        @Override
        public T next() {
            T toBeReturn = p.first;
            p = p.next;
            return toBeReturn;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Deque)) {
            return false;
        }

        Deque<T> other = (Deque<T>) o;

        // 3. 比较大小
        if (this.size() != other.size()) {
            return false;
        }

        // 4. 逐个比较元素
        for (int i = 0; i < this.size(); i++) {
            T thisItem = this.get(i);
            T otherItem = other.get(i);
            if (!java.util.Objects.equals(thisItem, otherItem)) {
                return false;
            }
        }

        return true;
    }
}
