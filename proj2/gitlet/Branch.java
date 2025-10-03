package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import static gitlet.Utils.*;

public class Branch implements Serializable {

    public String name;
    private List<String> commitList;

    public Branch(String name) {
        this.name = name;
        commitList = new LinkedList<>();
        addCurrBranchName(name);

        File newBranch = join(Repository.branch, name);
        try {
            newBranch.createNewFile();
        } catch (IOException e) {
            System.err.println("创建文件时发生IO错误: " + e.getMessage());
        }

        writeObject(newBranch, this);
    }

    public static void addCommit(String name, String commitID) {
        File thisBranch = join(Repository.branch, name);

        Branch b = readObject(thisBranch, Branch.class);

        b.commitList.add(commitID);
        writeObject(thisBranch, b);
    }

    public static void addCurrBranchName(String name) {
        File currBranchName = join(Repository.branch, "current_Branch_Name");

        if (!currBranchName.exists()) {
            try {
                currBranchName.createNewFile();
            } catch (IOException e) {
                System.err.println("创建文件时发生IO错误: " + e.getMessage());
            }
        }

        writeContents(currBranchName, name);
    }

    public static String readCurrBranchName() {
        File currBranchName = join(Repository.branch, "current_Branch_Name");

        return readContentsAsString(currBranchName);
    }
}
