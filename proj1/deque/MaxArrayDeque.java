package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {
    private Comparator<T> defaultCompare;

    public MaxArrayDeque(Comparator<T> c) {
        defaultCompare = c;
    }

    public T max() {
        if (this.isEmpty()) {
            return null;
        }
        T maxItem = get(0);

        for (int i = 1; i < size(); i++) {
            int compareResult = defaultCompare.compare(maxItem, get(i));
            if (compareResult < 0) {
                maxItem = get(i); 
            }
        }

        return maxItem;
    }

    public T max(Comparator<T> c) {
        if (this.isEmpty()) {
            return null;
        }
        T maxItem = get(0);

        for (int i = 0; i < size(); i++) {
            int compareResult = c.compare(maxItem, get(i));
            if (compareResult < 0) {
                maxItem = get(i); 
            }
        }

        return maxItem;
    }
}
