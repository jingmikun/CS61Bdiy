package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static gitlet.Utils.*;

public class Branch implements Serializable {

    public String name;
    private String headCommit;

    public Branch(String name) {
        this.name = name;
    }

    public void createBranchFile() {
        File newBranch = join(Repository.branch, this.name);
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

        b.headCommit = commitID;
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

    public String getHeadCommit() {
        return headCommit;
    }

    public void changeHeadCommit(String ID) {
        File thisBranch = join(Repository.branch, this.name);

        this.headCommit = ID;
        writeObject(thisBranch, this);
    }

    public void changeHeadCommitRemote(String ID) {
        this.headCommit = ID;
    }

    public static Branch readBranch(String name) {
        File thisBranch = join(Repository.branch, name);

        return readObject(thisBranch, Branch.class);
    }

    public static String readCurrHeadCommit() {
        return readBranch(Branch.readCurrBranchName()).getHeadCommit();
    }

}
