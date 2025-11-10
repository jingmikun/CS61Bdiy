package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *
 *  @author Jingmikun
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    public HashMap<String, String> blobs = new HashMap<>();
    public Date timestamp;
    public List<String> parent = new ArrayList<>();
    public String message;

    public Commit (String message, String parent1, String parent2) {
        this.message = message;
        this.parent.add(parent1);
        this.parent.add(parent2);

        if (parent1 == null && parent2 == null) {
            timestamp = new Date(0);
        } else {
            timestamp = new Date();
            // Use parent1 as the primary parent (parent1 should not be null in normal commits)
            String primaryParent = parent1 != null ? parent1 : parent2;
            if (primaryParent != null) {
                Commit parentcommit = readCommit(primaryParent);
                if (parentcommit != null) {
                    blobs.putAll(parentcommit.blobs);
                }
            }
        }
    }

    public void createCommitFile() {
        String commitId = sha1(serialize(this));
        File commitPath = join(Repository.commit, commitId);

        try {
            commitPath.createNewFile();
        } catch (IOException e) {
            System.err.println("创建文件时发生IO错误: " + e.getMessage());
        }

        writeObject(commitPath, this);
    }

    public boolean checkBlob(Blob b) {
        if (blobs == null) {
            return false;
        }

        String c = blobs.get(b.filename);
        if (c == null) {
            return false;
        }

        return c.equals(sha1(serialize(b)));
    }

    /**
     * Read a commit
     * @param ID
     * @return the corresponding commit object
     */
    public static Commit readCommit(String ID) {
        if (ID == null) {
            return null;
        }

        return readObject(join(Repository.commit, ID), Commit.class);
    }
}
