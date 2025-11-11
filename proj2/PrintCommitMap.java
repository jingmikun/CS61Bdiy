import gitlet.Commit;
public class PrintCommitMap {
  public static void main(String[] args) {
    Commit c = Commit.readCommit(args[0]);
    System.out.println(c.getBlobs());
  }
}
