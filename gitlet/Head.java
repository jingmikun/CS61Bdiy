package gitlet;

import java.io.File;
import static gitlet.Utils.*;

/**
 * The head class create the head file and memeorize the sha1 code of the commit;
 */
public class Head {
    private static File HEAD_PATH = Repository.head;

    // Safer variant: write a known commit id directly.
    public static void writeHeadId(String commitId) {
        writeContents(HEAD_PATH, commitId);
    }

    public static String currentHeadId() {
        return readContentsAsString(HEAD_PATH);
    }

    public static Commit returnCurrCommit() {
        File current = join(Repository.commit, readContentsAsString(HEAD_PATH));

        return readObject(current, Commit.class);
    }

}
