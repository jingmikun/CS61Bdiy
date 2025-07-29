package deque;

import java.util.Iterator;

public class LinkedListDeque<Item>
implements Iterable<Item> {
    private IntNode sentinel;
    private int size;

    public class IntNode{
        public Item first;
        public IntNode next;
        public IntNode prev;

        public IntNode(Item item, IntNode n,IntNode p){
            first = item;
            next = n;
            prev = p;
        }
    }

    public LinkedListDeque(){
        sentinel = new IntNode(null,null,null);
        sentinel.next = sentinel;
        sentinel.prev = sentinel;
        size = 0;
    }

    public void addFirst(Item item){
        IntNode remain = sentinel.next;
        IntNode newNode = new IntNode(item,remain,sentinel);

        size++;
        sentinel.next = newNode;
        remain.prev = newNode;
        if (size == 1){sentinel.prev = newNode;}
    }

    public void addLast(Item item){
        IntNode remain = sentinel.prev;
        IntNode newNode = new IntNode(item,sentinel,remain);

        size ++;
        sentinel.prev = newNode;
        remain.next = newNode;
        if (size == 1){sentinel.next = newNode;}
    }

    public boolean isEmpty(){
        return size == 0;
    }

    public int size(){
        return size;
    }

    public void printDeque(){
        IntNode p = sentinel.next;

        while (p != sentinel){
            System.out.print(p.first + " ");
            p = p.next;
        }
        System.out.println();
    }

    public Item removeFirst(){
        if (size == 0){return null;}
        Item returnValue = sentinel.next.first;
        sentinel.next = sentinel.next.next;
        sentinel.next.prev = sentinel;
        size --;
        return returnValue;
    }

    public Item removeLast(){
        if (size == 0){return null;}
        Item returnValue = sentinel.prev.first;
        sentinel.prev = sentinel.prev.prev;
        sentinel.prev.next = sentinel;
        size --;
        return returnValue;
    }

    public Item get(int index){
        IntNode p = sentinel;
        int counter = index;

        if (index > size-1 ) {return null;}
        while (counter != -1){
            p = p.next;
            counter --;
        }

        return p.first;
    }

    public Item getRecursive(int index){
        if (index > size-1) {return null;}
        return getRecursive(index,sentinel.next);
    }

    private Item getRecursive(int index,IntNode start){
        if (index == 0){
            return start.first;
        }
        else{
            return getRecursive(index-1,start.next);
        }
    }

    public Iterator<Item> iterator(){
        return new LinkedListIterator();
    }

    private class LinkedListIterator implements Iterator<Item>{

        IntNode p = sentinel.next;
        @Override
        public boolean hasNext() {
            return p != sentinel;
        }

        @Override
        public Item next() {
            Item toBeReturn = p.first;
            p = p.next;
            return toBeReturn;
        }
    }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof LinkedListDeque)){return false;}
        LinkedListDeque<Item> other = (LinkedListDeque<Item>) o;

        if (this.size()!=other.size()){return false;}
        for (int i = 0; i < this.size();i++){
            if(this.get(i) != other.get(i)) {return false;}
        }
        return true;
    }
}
