package deque;

import org.junit.Test;

import java.util.Comparator;
import java.util.Optional;

import static org.junit.Assert.*;


public class MaxArrayDequeTest {

    private static class numberComperator implements Comparator<Integer> {

        @Override
        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }
    }

    @Test
    public void maxTest(){
        numberComperator c = new numberComperator();
        MaxArrayDeque<Integer> mad = new MaxArrayDeque<>(c);

        for (int i = 0;i < 10 ; i++){
            mad.addLast(i);
        }

        int result = mad.max();

        assertEquals(9,result);
    }
}
