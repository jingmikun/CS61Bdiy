package deque;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArrayDequeTest {

    @Test
    public void comparisonTest(){
        LinkedListDeque<Integer> lld = new LinkedListDeque<>();
        ArrayDeque<Integer> ad = new ArrayDeque<>();

        lld.addLast(4);
        lld.addLast(5);
        lld.addLast(6);
        lld.addFirst(3);
        lld.addFirst(2);
        lld.addFirst(1);

        ad.addLast(4);
        ad.addLast(5);
        ad.addLast(6);
        ad.addFirst(3);
        ad.addFirst(2);
        ad.addFirst(1);

        assertEquals(lld.size(),ad.size());
        assertEquals(lld.removeFirst(),ad.removeFirst());
        assertEquals(lld.removeFirst(),ad.removeFirst());
        assertEquals(lld.removeFirst(),ad.removeFirst());
        assertEquals(lld.removeLast(),ad.removeLast());
        assertEquals(lld.removeLast(),ad.removeLast());
        assertEquals(lld.removeLast(),ad.removeLast());
    }

    @Test
    public void randomizedTest(){
        LinkedListDeque<Integer> L = new LinkedListDeque<>();
        ArrayDeque<Integer> A = new ArrayDeque<>();

        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 5);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                A.addLast(randVal);
            } else if (operationNumber == 1) {
                // addFirst
                int randVal = StdRandom.uniform(0, 100);
                L.addFirst(randVal);
                A.addFirst(randVal);
            } else if (operationNumber == 2) {
                // size
                int sizeL = L.size();
                int sizeB = A.size();
                assertEquals(sizeL,sizeB);
            } else if (operationNumber == 3) {
                // removeLast
                if (!L.isEmpty() && !A.isEmpty()){
                    assertEquals(L.removeLast(), A.removeLast());
                }
            } else if (operationNumber == 4) {
                // get
                if (!L.isEmpty() && !A.isEmpty()) {
                    int randVal = StdRandom.uniform(0, A.size());
                    assertEquals(L.get(randVal), A.get(randVal));
                }
            }
        }
    }

    @Test
    public void testGetLast(){
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        for (int i = 0; i < 10; i++) {
            ad.addLast(i);
        }

        for (int i = 0; i < 10; i++) {
            assertEquals("Should have the same value", i, ad.get(i), 0.0);
        }
    }

    @Test
    public void testPrint(){
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        for (int i = 0; i < 10; i++) {
            ad.addLast(i);
        }

        ad.printDeque();
    }

    @Test
    public void testIterator(){
        ArrayDeque<Integer> ad = new ArrayDeque<>();
        for (int i = 0; i < 10; i++) {
            ad.addLast(i);
        }

        int check = 0;
        for (int i:ad){
            assertEquals(i,check);
            check++;
        }
    }

    @Test
    public void checkEqual(){
        ArrayDeque<Integer> ad1 = new ArrayDeque<>();
        ArrayDeque<Integer> ad2 = new ArrayDeque<>();
        ArrayDeque<Integer> ad3 = new ArrayDeque<>();
        ArrayDeque<Integer> ad4 = new ArrayDeque<>();

        for (int i = 0; i < 10; i++) {
            ad1.addLast(i);
            ad4.addLast(i);
        }

        for (int i = 0; i < 8; i++) {
            ad2.addLast(i);
        }

        for (int i = 0; i < 8; i++){
            ad3.addLast(i);
        }
        ad3.addLast(10);

        assertFalse(ad1.equals(ad2));
        assertFalse(ad1.equals(ad3));
        assertTrue(ad1.equals(ad4));
    }
}
