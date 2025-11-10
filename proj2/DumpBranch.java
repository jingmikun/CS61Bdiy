import gitlet.Branch;
import java.io.File;
import static gitlet.Utils.*;
public class DumpBranch {
  public static void main(String[] args) {
    Branch b = readObject(new File(args[0]), Branch.class);
    System.out.println(b.getName()+" -> " + b.getHeadCommit());
  }
}
