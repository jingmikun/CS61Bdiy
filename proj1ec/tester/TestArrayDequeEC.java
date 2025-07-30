package tester;

import static org.junit.Assert.*;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import student.StudentArrayDeque;

public class TestArrayDequeEC {

    @Test
    public void randomizedTest() {
        ArrayDequeSolution<Integer> L = new ArrayDequeSolution<>();
        StudentArrayDeque<Integer> A = new StudentArrayDeque<>();

        int N = 5000;
        String[] actionDict = new String[3];

        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 5);

            actionDict[0] = actionDict[1];
            actionDict[1] = actionDict[2];

            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                A.addLast(randVal);

                actionDict[2] = "addLast(" + randVal + ")";
            } else if (operationNumber == 1) {
                // addFirst
                int randVal = StdRandom.uniform(0, 100);
                L.addFirst(randVal);
                A.addFirst(randVal);

                actionDict[2] = "addFirst(" + randVal + ")";
            } else if (operationNumber == 2) {
                // size

                actionDict[2] = "removeFirst()";

                if (!L.isEmpty() && !A.isEmpty()) {
                    assertEquals("\n"+actionDict[0]+"\n"+actionDict[1]+"\n"+actionDict[2],L.removeFirst(), A.removeFirst());
                }
            } else if (operationNumber == 3) {
                // removeLast

                actionDict[2] = "removeLast()";

                if (!L.isEmpty() && !A.isEmpty()) {
                    assertEquals("\n"+actionDict[0]+"\n"+actionDict[1]+"\n"+actionDict[2],L.removeLast(), A.removeLast());
                }
            }
        }
    }
}
