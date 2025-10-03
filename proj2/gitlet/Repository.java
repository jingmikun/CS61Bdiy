package gitlet;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author jingmikun
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static File commit = join(GITLET_DIR, "commit");
    public static File blob = join(GITLET_DIR, "blob");
    public static File stage = join(GITLET_DIR, "stage");
    public static File branch = join(GITLET_DIR, "branch");
    public static File addition = join(stage, "addition");
    public static File removal = join(stage, "removal");
    public static File head = join(GITLET_DIR, "head");

    /** Init Command: this function initialize a repository of gitlet,
     * -- create a file system - create the initial commit - integrate the initial commit into curr Branch and Head
     * @Para void
     */
    public static void init() {
        GITLET_DIR.mkdir();
        commit.mkdir();
        blob.mkdir();
        stage.mkdir();
        branch.mkdir();
        addition.mkdir();
        removal.mkdir();

        try {
            head.createNewFile();
        } catch (IOException e) {
            System.err.println("创建文件时发生IO错误: " + e.getMessage());
        }

        Commit init_commit = new Commit("initial commit", null);
        init_commit.createCommitFile();
        Head.writeInHead(init_commit);

        Branch master = new Branch("master");
        Branch.addCommit("master", sha1(serialize(init_commit)));
    }

    public static void add(File newFile) {
        if (!newFile.exists()) {
            throw new GitletException("File does not exist.");
        }

        Blob newblob = new Blob(newFile);
        //To see whether the blob has been added in currCommit;
        Commit currentCommit = Head.returnCurrCommit();
        List<String> allStageBlobId = plainFilenamesIn(addition);

        for (String id : allStageBlobId) {
            Blob tmp = readObject(join(blob, id), Blob.class);
            if (tmp.filename.equals(newblob.filename)) {
                join(addition, id).delete();
            }
        }

        if (!currentCommit.checkBlob(newblob)) {
            writeObject(join(blob, sha1(serialize(newblob))), newblob);

            try {
                join(addition, sha1(serialize(newblob))).createNewFile();
            } catch (IOException e) {
                System.err.println("创建文件时发生IO错误: " + e.getMessage());
            }
        }
    }

    public static void commit(String message) {
        List<String> allStageBlobId = plainFilenamesIn(addition);

        if (allStageBlobId == null || allStageBlobId.isEmpty()) {
            throw new GitletException("No changes added to the commit.");
        }

        String currentCommitID = sha1(serialize(Head.returnCurrCommit()));
        Commit newCommit = new Commit(message, currentCommitID);

        /**
         * In this function, we are going to check every blob in parent commit (i.e. the blobs default in
         * new commit) , if there is a new version of the blob, change it
         * the time complexity is O(mlogn) m is the file number in the staging area, n is the file number
         * in the parent commit
         */

        if (newCommit.blobs == null) {
            for (String b: allStageBlobId) {
                String filename = readObject(join(blob, b), Blob.class).filename;
                newCommit.blobs.put(filename, b);
            }
        } else {
            for (String b: allStageBlobId) {
                String filename = readObject(join(blob, b), Blob.class).filename;
                if (newCommit.blobs.containsKey(filename)) {
                    newCommit.blobs.remove(filename);
                    newCommit.blobs.put(filename, b);
                } else {
                    newCommit.blobs.put(filename, b);
                }
            }
        }


         //save the commit and clear the staging area
        newCommit.createCommitFile();
        for (String b: allStageBlobId) {
            File d = join(addition,b);
            d.delete();
        }

        //add thisCommit into commit tree (curr branch) and change the head pointer
        Branch.addCommit(Branch.readCurrBranchName(), sha1(serialize(newCommit)));
        Head.writeInHead(newCommit);
    }

    /** The log function reads from the current commit and return a log of all its parents until inital
     * commit.
     */

    public static void log() {
        Commit p = Head.returnCurrCommit();

        // Use a stable, locale-fixed date format with explicit timezone.
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        while (p != null) {
            System.out.println("===");
            System.out.println("commit " + sha1(serialize(p)));
            System.out.println("Date: " + sdf.format(p.timestamp));
            System.out.println(p.message);
            System.out.println();
            p = Commit.readCommit(p.parent);
        }
    }

    /** The case I checkout：checkout with merely the file name,
     * Takes the version of the file as it exists in the head commit and puts it in the working directory,
     * overwriting the version of the file that’s already there if there is one.
     * @param filename
     * */

    public static void checkout(String filename) {
        Commit curr = Head.returnCurrCommit();

        if (!curr.blobs.containsKey(filename)) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            String ID = curr.blobs.get(filename);
            Blob b = readObject(join(blob, ID), Blob.class);
            File f = join(CWD, filename);

            writeContents(f, b.content);
        }
    }

    /** The case II checkout：checkout with merely the file name,
     * Takes the version of the file as it exists in the head commit and puts it in the working directory,
     * overwriting the version of the file that’s already there if there is one.
     * @param ID, filename
     ** */

    public static void checkout(String ID, String filename) {
        String resolved = resolveCommitId(ID);
        File commitPath = join(commit, resolved);
        if (!commitPath.exists()) {
            throw new GitletException("No commit with that id exists.");
        }

        Commit c = readObject(commitPath, Commit.class);

        if (!c.blobs.containsKey(filename)) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            String id = c.blobs.get(filename);
            Blob b = readObject(join(blob, id), Blob.class);
            File f = join(CWD, filename);

            writeContents(f, b.content);
        }
    }

    /** Resolve possibly-short commit id to full 40-char id. */
    private static String resolveCommitId(String shortId) {
        if (shortId == null) {
            throw new GitletException("No commit with that id exists.");
        }
        // Full id case
        if (shortId.length() == Utils.UID_LENGTH) {
            return shortId;
        }
        List<String> ids = plainFilenamesIn(commit);
        if (ids == null || ids.isEmpty()) {
            throw new GitletException("No commit with that id exists.");
        }
        String match = null;
        for (String id : ids) {
            if (id.startsWith(shortId)) {
                if (match == null) {
                    match = id;
                } else {
                    // Ambiguous short id; treat as not found per simple semantics
                    throw new GitletException("No commit with that id exists.");
                }
            }
        }
        if (match == null) {
            throw new GitletException("No commit with that id exists.");
        }
        return match;
    }
}
