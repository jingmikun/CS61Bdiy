package IntList;

import static org.junit.Assert.*;
import org.junit.Test;

public class SquarePrimesTest {

    /**
     * Here is a test for isPrime method. Try running it.
     * It passes, but the starter code implementation of isPrime
     * is broken. Write your own JUnit Test to try to uncover the bug!
     */
    @Test
    public void testSquarePrimesSimple() {
        IntList lst = IntList.of(14, 15, 16, 17, 18);
        boolean changed = IntListExercises.squarePrimes(lst);
        assertEquals("14 -> 15 -> 16 -> 289 -> 18", lst.toString());
        assertTrue(changed);
    }

    @Test
    public void testSquarePrimesTwoPrime() {
        IntList lst = IntList.of(14, 7, 16, 17, 18);
        boolean changed = IntListExercises.squarePrimes(lst);
        assertEquals("14 -> 49 -> 16 -> 289 -> 18", lst.toString());
        assertTrue(changed);
    }

    @Test
    public void testSquarePrimesNoPrime() {
        IntList lst = IntList.of(8, 15, 16, 20, 18);
        boolean changed = IntListExercises.squarePrimes(lst);
        assertEquals("8 -> 15 -> 16 -> 20 -> 18", lst.toString());
        assertTrue(!changed);
    }

    @Test
    public void testSquarePrimesThreePrime() {
        IntList lst = IntList.of(14, 7, 16, 17, 19);
        boolean changed = IntListExercises.squarePrimes(lst);
        assertEquals("14 -> 49 -> 16 -> 289 -> 361", lst.toString());
        assertTrue(changed);
    }
}
