package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import timingtest.AList;

import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
    @Test
    public void testThreeAddThreeRemove(){
        AListNoResizing<Integer> noResizingTest = new AListNoResizing<>();
        BuggyAList<Integer> buggyAListTest = new BuggyAList<>();

        noResizingTest.addLast(4);
        buggyAListTest.addLast(4);
        noResizingTest.addLast(5);
        buggyAListTest.addLast(5);
        noResizingTest.addLast(6);
        buggyAListTest.addLast(6);

        assertEquals(noResizingTest.size(),buggyAListTest.size());

        assertEquals(noResizingTest.removeLast(),buggyAListTest.removeLast());
        assertEquals(noResizingTest.removeLast(),buggyAListTest.removeLast());
        assertEquals(noResizingTest.removeLast(),buggyAListTest.removeLast());
    }

    @Test
    public void randomizedTest(){
        AListNoResizing<Integer> L = new AListNoResizing<>();
        BuggyAList<Integer> B = new BuggyAList<>();

        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                B.addLast(randVal);
            } else if (operationNumber == 1) {
                // size
                int sizeL = L.size();
                int sizeB = B.size();
                assertEquals(sizeL,sizeB);
            } else if (operationNumber == 2) {
                // getLast
                if (L.size() > 0 && B.size() > 0){
                    assertEquals(L.getLast(),B.getLast());
                }
            } else if (operationNumber == 3) {
                // removeLast
                if (L.size() > 0 && B.size() > 0){
                    assertEquals(L.removeLast(),B.removeLast());
                }
            }
        }
    }
}
