package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeSLList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeGetLast();
    }

    public static void timeGetLast() {
        AList<Integer> testNumber = new AList<>();
        AList<Double> timeNumber = new AList<>();
        AList<Integer> operation = new AList<>();

        int n = 1000;
        int m = 10000;
        int maxTestNumber = 128000;
        while (n <= maxTestNumber){
            testNumber.addLast(n);
            operation.addLast(m);
            SLList<Integer> Execute = new SLList<>();

            for(int k = 0;k <= n; k++){
                Execute.addLast(1);
            }

            Stopwatch sw = new Stopwatch();
            for(int i =0;i <= m; i++){
                Execute.getLast();
            }
            timeNumber.addLast(sw.elapsedTime());
            n *= 2;
        }

        printTimingTable(testNumber,timeNumber,operation);
    }

}
