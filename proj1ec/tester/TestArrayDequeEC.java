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
        String operation = "";

        for (int i = 0; i < N; i += 1) {
            int operationNumber = StdRandom.uniform(0, 4);

            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                A.addLast(randVal);

                operation = operation + "addLast(" + randVal + ")"+ "\n";
            } else if (operationNumber == 1) {
                // addFirst
                int randVal = StdRandom.uniform(0, 100);
                L.addFirst(randVal);
                A.addFirst(randVal);

                operation = operation + "addFirst(" + randVal + ")"+ "\n";
            } else if (operationNumber == 2) {
                // removeFirst

                operation = operation + "removeFirst()"+ "\n";

                if (!L.isEmpty() && !A.isEmpty()) {
                    assertEquals(operation,L.removeFirst(), A.removeFirst());
                }
            } else if (operationNumber == 3) {
                // removeLast

                operation = operation + "removeLast()"+ "\n";

                if (!L.isEmpty() && !A.isEmpty()) {
                    assertEquals(operation,L.removeLast(), A.removeLast());
                }
            }
        }
    }
}
