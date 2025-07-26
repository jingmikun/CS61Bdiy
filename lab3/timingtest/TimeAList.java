package timingtest;

import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeAList {
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
        timeAListConstruction();
    }

    public static void timeAListConstruction() {
        AList<Integer> Test = new AList<>();
        AList<Double> Time = new AList<>();

        //Initiating the test number
        int n = 1000;
        int maxTestNumber = 128000;
        while (n <= maxTestNumber){
            Test.addLast(n);
            AList<Integer> Execute = new AList<>();
            Stopwatch sw = new Stopwatch();

            for(int k = 0;k <= n; k++){
                Execute.addLast(1);
            }
            Time.addLast(sw.elapsedTime());
            n *= 2;
        }

        printTimingTable(Test,Time,Test);
    }
}
