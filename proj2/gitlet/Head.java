package gitlet;

import java.io.File;
import static gitlet.Utils.*;

/**
 * The head class create the head file and memeorize the sha1 code of the commit;
 */
public class Head {
    private static File HEAD_PATH = Repository.head;

    public static void writeInHead(Commit commit){
        writeContents(HEAD_PATH, sha1(serialize(commit)));
    }

    public static Commit returnCurrCommit() {
        File current = join(Repository.commit, readContentsAsString(HEAD_PATH));

        return readObject(current, Commit.class);
    }

}
