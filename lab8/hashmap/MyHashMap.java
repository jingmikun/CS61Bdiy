package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] buckets;
    private int size;
    private double maxLoad = 0.75;
    private int tableSize = 16;
    // You should probably define some more!

    /** Constructors */
    public MyHashMap() {
        buckets = createTable(tableSize);

    }

    public MyHashMap(int initialSize) {
        this.tableSize = initialSize;
        buckets = createTable(tableSize);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        this.tableSize = initialSize;
        this.maxLoad = maxLoad;

        buckets = createTable(tableSize);
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        Collection<Node>[] table = new Collection[tableSize];

        for (int k = 0; k < tableSize ; k++) {
            table[k] = createBucket();
        }

        return table;
    }

    // Your code won't compile until you do so!

    @Override
    public void clear() {
        buckets = createTable(tableSize);
        size = 0;
    }

    @Override
    public boolean containsKey(K key) {
        return this.get(key) != null;
    }

    @Override
    public V get(K key) {
        for (Node n : buckets[Math.floorMod(key.hashCode(), tableSize)]) {
            if (n.key.equals(key)) {
                return n.value;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public void put(K key, V value) {

        Node newnode = new Node(key, value);
        int pos = Math.floorMod(key.hashCode(), tableSize);
        boolean flag = false;

        for (Node n : buckets[pos]) {
            if (n.key.equals(key)) {
                n.value = value;
                flag = true;
            }
        }

        if (!flag) {
            buckets[pos].add(newnode);
            size ++;
        }

        if ((double) this.size / this.tableSize > this.maxLoad) {
            resize(tableSize * 2);
        }
    }

    private void resize(int newSize) {
        // 1. 创建一个更大容量的新表
        Collection<Node>[] newBuckets = createTable(newSize);

        for (Collection<Node> bucket : this.buckets) {
            for (Node node : bucket) {
                int newPos = Math.floorMod(node.key.hashCode(), newSize);
                newBuckets[newPos].add(node);
            }
        }

        this.buckets = newBuckets;
        this.tableSize = newSize;
    }

    @Override
    public Set<K> keySet() {
        Set<K> result = new HashSet<>();

        for (K key : this) {
            result.add(key);
        }
        return result;
    }

    @Override
    public V remove(K key) {
        for (Collection<Node> bucket : this.buckets) {
            for (Node node : bucket) {
                if (node.key.equals(key)) {
                    V val = node.value;
                    bucket.remove(node);
                    return val;
                }
            }
        }
        return null;
    }

    @Override
    public V remove(K key, V value) {
        return remove(key);
    }

    @Override
    public Iterator<K> iterator() {
        return new hashIterator();
    }

    private class hashIterator implements Iterator<K> {

        private int currBucket = 0;
        private Iterator<Node> currIterator = null;
        @Override
        public boolean hasNext() {
            if (currIterator != null && currIterator.hasNext()) {
                return true;
            }

            while (currBucket < tableSize) {
                if (buckets[currBucket] != null && !buckets[currBucket].isEmpty()) {
                    currIterator = buckets[currBucket].iterator();
                    currBucket ++;
                    if (currIterator.hasNext()) {
                        return true;
                    }
                } else {
                    currBucket ++;
                }
            }

            return false;
        }

        @Override
        public K next() {
            if (!hasNext()) {
                return null;
            }
            else {
                return currIterator.next().key;
            }
        }
    }
}
