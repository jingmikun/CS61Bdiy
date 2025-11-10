package gitlet;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    static File commit = join(GITLET_DIR, "commit");
    static File blob = join(GITLET_DIR, "blob");
    static File stage = join(GITLET_DIR, "stage");
    static File branch = join(GITLET_DIR, "branch");
    static File addition = join(stage, "addition");
    static File additionByName = join(stage, "addition_by_name");
    static File removal = join(stage, "removal");
    static File head = join(GITLET_DIR, "head");
    static File remote = join(GITLET_DIR, "remote");

    /** Init Command: this function initialize a repository of gitlet,
     * -- create a file system - create the initial commit - integrate
     * the initial commit into curr Branch and Head
     * @Para void
     */
    public static void init() {
        GITLET_DIR.mkdir();
        commit.mkdir();
        blob.mkdir();
        stage.mkdir();
        branch.mkdir();
        addition.mkdir();
        additionByName.mkdir();
        removal.mkdir();
        remote.mkdir();

        try {
            head.createNewFile();
        } catch (IOException e) {
            System.err.println("???????????O???: " + e.getMessage());
        }

        Commit initCommit = new Commit("initial commit", null, null);
        String initId = initCommit.createCommitFile();
        Head.writeHeadId(initId);

        Branch master = new Branch("master");
        master.createBranchFile();
        Branch.addCommit("master", initId);
        // Explicitly set current branch name to master at init
        Branch.addCurrBranchName("master");
    }

    /** Clear the entire staging area: additions (by id), name index, and removals. */
    private static void clearStaging() {
        List<String> addIds = plainFilenamesIn(addition);
        if (addIds != null) {
            for (String id : addIds) {
                join(addition, id).delete();
            }
        }
        List<String> addNames = plainFilenamesIn(additionByName);
        if (addNames != null) {
            for (String name : addNames) {
                join(additionByName, name).delete();
            }
        }
        List<String> rmIds = plainFilenamesIn(removal);
        if (rmIds != null) {
            for (String id : rmIds) {
                join(removal, id).delete();
            }
        }
    }

    public static void add(File newFile) {
        if (!newFile.exists()) {
            System.out.println("File does not exist.");
            return;
        }

        Blob newblob = new Blob(newFile);
        //To see whether the blob has been added in currCommit;
        Commit currentCommit = Head.returnCurrCommit();
        // If this file is already staged for addition, unstage it via index
        File idxFile = join(additionByName, newblob.getFilename());
        if (idxFile.exists()) {
            String oldId = readContentsAsString(idxFile);
            join(addition, oldId).delete();
            idxFile.delete();
        }

        // Cancel pending removal for this file based on HEAD mapping
        String trackedId = currentCommit.getBlobs().get(newblob.getFilename());
        if (trackedId != null) {
            join(removal, trackedId).delete();
        }

        String newId = sha1(serialize(newblob));
        if (!currentCommit.checkBlob(newblob)) {
            writeObject(join(blob, newId), newblob);

            try {
                join(addition, newId).createNewFile();
            } catch (IOException e) {
                System.err.println("???????????O???: " + e.getMessage());
            }
            // Update name -> id index for staged addition (constant time)
            writeContents(join(additionByName, newblob.getFilename()), newId);
        }
    }

    public static void commit(String message, String parent2) {
        List<String> allStageBlobId = plainFilenamesIn(addition);
        List<String> allRemovalBlobId = plainFilenamesIn(removal);

        boolean noAdds = (allStageBlobId == null || allStageBlobId.isEmpty());
        boolean noRemoves = (allRemovalBlobId == null || allRemovalBlobId.isEmpty());
        if (noAdds && noRemoves) {
            System.out.println("No changes added to the commit.");
            return;
        }

        // Use the actual commit ID from HEAD file, not a recalculated one
        // This ensures we use the correct parent commit ID
        String currentCommitID = readContentsAsString(Repository.head);
        Commit newCommit = new Commit(message, currentCommitID, parent2);

        /**
         * In this function, we are going to check every blob in parent commit (i.e. the blobs default in
         * new commit) , if there is a new version of the blob, change it
         * the time complexity is O(mlogn) m is the file number in the staging area, n is the file number
         * in the parent commit
         */

        // Apply staged additions (if any)
        if (allStageBlobId != null) {
            for (String b: allStageBlobId) {
                String filename = readObject(join(blob, b), Blob.class)
                        .getFilename();
                if (newCommit.getBlobs().containsKey(filename)) {
                    newCommit.getBlobs().remove(filename);
                }
                newCommit.getBlobs().put(filename, b);
            }
        }

        // Apply staged removals (if any)
        if (allRemovalBlobId != null) {
            for (String b: allRemovalBlobId) {
                String filename = readObject(join(blob, b), Blob.class)
                        .getFilename();
                newCommit.getBlobs().remove(filename);
            }
        }


         //save the commit and clear the staging area
        String newCommitId = newCommit.createCommitFile();
        clearStaging();

        //add thisCommit into commit tree (curr branch) and change the head pointer
        Branch.addCommit(Branch.readCurrBranchName(), newCommitId);
        Head.writeHeadId(newCommitId);
    }

    /** The log function reads from the current commit and return a log
     * of all its parents until initial commit.
     */

    public static void log() {
        // Start from the current HEAD commit id so we always print the true ids
        String commitId = readContentsAsString(head);
        // Use a stable, locale-fixed date format with explicit timezone.
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        while (commitId != null) {
            Commit p = readObject(join(commit, commitId), Commit.class);
            System.out.println("===");
            System.out.println("commit " + commitId);
            // Print Merge line if this is a merge commit (has two parents)
            if (p.getParent().size() >= 2 && p.getParent().get(0) != null
                    && p.getParent().get(1) != null) {
                String parent1Short = p.getParent().get(0).substring(0, 7);
                String parent2Short = p.getParent().get(1).substring(0, 7);
                System.out.println("Merge: " + parent1Short + " "
                        + parent2Short);
            }
            System.out.println("Date: " + sdf.format(p.getTimestamp()));
            System.out.println(p.getMessage());
            System.out.println();
            // Move to first parent; stop when there is no parent.
            if (p.getParent().isEmpty() || p.getParent().get(0) == null) {
                break;
            }
            commitId = p.getParent().get(0);
        }
    }

    /** The case I checkout??heckout with merely the file name,
     * Takes the version of the file as it exists in the head commit
     * and puts it in the working directory, overwriting the version
     * of the file that's already there if there is one.
     * @param filename
     * */

    public static void checkout(String filename) {
        Commit curr = Head.returnCurrCommit();

        if (!curr.getBlobs().containsKey(filename)) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            String id = curr.getBlobs().get(filename);
            Blob b = readObject(join(blob, id), Blob.class);
            File f = join(CWD, filename);

            writeContents(f, b.getContent());
        }
    }

    /** The case II checkout??heckout with merely the file name,
     * Takes the version of the file as it exists in the head commit
     * and puts it in the working directory, overwriting the version
     * of the file that's already there if there is one.
     * @param id, filename
     ** */

    public static void checkout(String id, String filename) {
        String resolved = resolveCommitId(id);
        File commitPath = join(commit, resolved);
        if (!commitPath.exists()) {
            throw new GitletException("No commit with that id exists.");
        }

        Commit c = readObject(commitPath, Commit.class);

        if (!c.getBlobs().containsKey(filename)) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            String blobId = c.getBlobs().get(filename);
            Blob b = readObject(join(blob, blobId), Blob.class);
            File f = join(CWD, filename);

            writeContents(f, b.getContent());
        }
    }

    /**Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory,overwriting the versions
     * of the files that are already there if they exist.Also, at the
     * end of this command, the given branch will now be considered
     * the current branch (HEAD). Any files that are tracked in the
     * current branch but are not present in the checked-out branch
     * are deleted. The staging area is cleared, unless the checked-out
     * branch is the current branch
     * @param branchName
     */

    public static void checkoutWithBranch(String branchName) {
        //Find the branch
        List<String> allBranchName = plainFilenamesIn(Repository.branch);
        List<String> filesInCWD = plainFilenamesIn(CWD);
        Commit curr = Head.returnCurrCommit();

        Branch b = null;

        if (Objects.equals(branchName, Branch.readCurrBranchName())) {
            throw new GitletException("No need to checkout the current branch.");
        }

        if (allBranchName != null) {
            for (String n: allBranchName) {
                if (Objects.equals(n, branchName)) {
                    b = readObject(join(branch, n), Branch.class);
                }
            }
        }

        if (b == null) {
            throw new GitletException("No such branch exists.");
        }

        Commit target = readObject(join(commit, b.getHeadCommit()), Commit.class);

        // Untracked file check: any file untracked by current,
        // but tracked by target
        if (filesInCWD != null) {
            for (String n : filesInCWD) {
                if (!curr.getBlobs().containsKey(n)
                        && target.getBlobs().containsKey(n)) {
                    throw new GitletException("There is an untracked file in "
                            + "the way; delete it, or add and commit it first.");
                }
            }
        }

        // Move HEAD to target commit first so file checkout reads from target
        Head.writeHeadId(b.getHeadCommit());

        // Remove files that are tracked in current but not in target
        for (String n : curr.getBlobs().keySet()) {
            if (!target.getBlobs().containsKey(n)) {
                restrictedDelete(join(CWD, n));
            }
        }

        // Write all files tracked in target to the working directory
        for (String n : target.getBlobs().keySet()) {
            checkout(n); // uses HEAD (now target) to write contents
        }

        // Clear staging area
        clearStaging();

        // Update current branch name pointer
        Branch.addCurrBranchName(branchName);
    }

    /** Resolve possibly-short commit id to full 40-char id. */
    private static String resolveCommitId(String shortId) {
        if (shortId == null) {
            throw new GitletException("No commit with that id exists.");
        }
        // Full id case
        if (shortId.length() == Utils.UID_LENGTH) {
            if (!join(commit, shortId).exists()) {
                throw new GitletException("No commit with that id exists.");
            }
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

    /** The rm function Unstage the file if it is currently staged
     * for addition. If the file is tracked in the current commit,
     * stage it for removal and remove the file from the working
     * directory if the user has not already done (do not remove it
     * unless it is tracked in the current commit).
     * @param filename
     */
    public static void rm(String filename) {
        //If the file is at stage
        List<String> allStageBlobId = plainFilenamesIn(addition);
        boolean flag1 = false; // this flag to detect whether there is a file in stage
        boolean flag2 = false; // this flag to detect whether there is a file in commit

        //check current commit and see whether there is this file
        Commit curr = Head.returnCurrCommit();

        // If staged for addition, unstage in constant time via index
        File idx = join(additionByName, filename);
        if (idx.exists()) {
            String stagedId = readContentsAsString(idx);
            join(addition, stagedId).delete();
            idx.delete();
            flag1 = true;
            // If file is not in current commit, also remove it
            // from working directory
            if (!curr.getBlobs().containsKey(filename)) {
                File fileInCWD = join(CWD, filename);
                if (fileInCWD.exists()) {
                    restrictedDelete(fileInCWD);
                }
            }
        }

        if (curr.getBlobs().containsKey(filename)) {
            String thisBlobId = curr.getBlobs().get(filename);

            try {
                join(removal, thisBlobId).createNewFile();
            } catch (IOException e) {
                System.err.println("???????????O???: " + e.getMessage());
            }

            restrictedDelete(join(CWD, filename));
            flag2 = true;
        }

        if (!flag1 && !flag2) {
            System.out.println("No reason to remove the file.");
            return;
        }
    }

    /**Like log, except displays information
     * about all commits ever made. The order of the commits does not matter.
     */

    public static void globalLog() {
        List<String> allCommit = plainFilenamesIn(commit);

        if (allCommit != null) {
            Map<String, Commit> commitCache = new HashMap<>();
            for (String id : allCommit) {
                commitCache.put(id, readObject(join(commit, id), Commit.class));
            }
            allCommit.sort((id1, id2) -> {
                Commit c1 = commitCache.get(id1);
                Commit c2 = commitCache.get(id2);
                int cmp = c2.getTimestamp().compareTo(c1.getTimestamp());
                if (cmp != 0) {
                    return cmp;
                }
                return id2.compareTo(id1);
            });

            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            for (String c : allCommit) {
                Commit p = commitCache.get(c);
                System.out.println("===");
                System.out.println("commit " + c);
                // Print Merge line if this is a merge commit (has two parents)
                if (p.getParent().size() >= 2 && p.getParent().get(0) != null
                        && p.getParent().get(1) != null) {
                    String parent1Short = p.getParent().get(0).substring(0, 7);
                    String parent2Short = p.getParent().get(1).substring(0, 7);
                    System.out.println("Merge: " + parent1Short + " "
                            + parent2Short);
                }
                System.out.println("Date: " + sdf.format(p.getTimestamp()));
                System.out.println(p.getMessage());
                System.out.println();
            }
        }
    }

    /**Prints out the ids of all commits that have the given commit message, one per line.
     * If there are multiple such commits, it prints the ids out on separate lines.
     * The commit message is a single operand; to indicate a multiword message,
     * put the operand in quotation marks, as for the commit command below.
     * @param message
     */

    public static void find(String message) {
        List<String> ids = plainFilenamesIn(commit);
        if (ids == null || ids.isEmpty()) {
            System.out.println("Found no commit with that message.");
            return;
        }

        Map<String, Commit> cache = new HashMap<>();
        List<String> matches = new ArrayList<>();

        for (String id : ids) {
            Commit c = cache.computeIfAbsent(id, k -> readObject(join(commit, k), Commit.class));
            if (Objects.equals(c.getMessage(), message)) {
                matches.add(id);
            }
        }

        if (matches.isEmpty()) {
            System.out.println("Found no commit with that message.");
            return;
        }

        matches.sort((a, b) -> {
            Commit ca = cache.get(a);
            Commit cb = cache.get(b);
            int cmp = cb.getTimestamp().compareTo(ca.getTimestamp());
            if (cmp != 0) {
                return cmp;
            }
            return b.compareTo(a);
        });

        for (String id : matches) {
            System.out.println(id);
        }
    }

    /** Displays what branches currently exist, and marks the current
     * branch with a *. Also displays what files have been staged for
     * addition or removal and modifications not staged and untracked files
     */

    public static void status() {
        //Print the Branches
        List<String> allBranchName = plainFilenamesIn(branch);
        String currBranch = Branch.readCurrBranchName();

        System.out.println("=== Branches ===");
        System.out.println("*" + currBranch);

        if (allBranchName != null) {
            Collections.sort(allBranchName);
            for (String n : allBranchName) {
                if (!Objects.equals(n, currBranch)
                        && !n.equals("current_Branch_Name")) {
                    System.out.println(n);
                }
            }
        }
        System.out.println();

        //Print the Staged Files;
        List<String> allStageId = plainFilenamesIn(addition);
        List<String> allAdditionStage = plainFilenamesIn(additionByName);
        System.out.println("=== Staged Files ===");

        if (allAdditionStage != null && !allAdditionStage.isEmpty()) {
            Collections.sort(allAdditionStage);
            for (String n : allAdditionStage) {
                System.out.println(n);
            }
        }
        System.out.println();
        //Print the Removed Files;
        List<String> allRemovalStage = plainFilenamesIn(removal);
        List<String> allRemovalFileName = new ArrayList<>();

        if (allRemovalStage != null) {
            for (String i : allRemovalStage) {
                allRemovalFileName.add(readObject(join(blob, i), Blob.class)
                        .getFilename());
            }
        }

        System.out.println("=== Removed Files ===");
        Collections.sort(allRemovalFileName);
        for (String n : allRemovalFileName) {
            System.out.println(n);
        }
        System.out.println();
        //Print the modified but not stage files;
        // A file in the working directory is modified but not staged if it is
        //1.Tracked in the current commit, changed in the working directory, but not staged; or
        //2.Staged for addition, but with different contents than in the working directory; or
        //3.Staged for addition, but deleted in the working directory; or
        //4.Not staged for removal, but tracked in the current commit and deleted from the working directory.

        List<String> modifiedNotStaged = new ArrayList<>();
        List<String> fileInCWD = plainFilenamesIn(CWD);
        //Read all the files in the curr commit
        Commit curr = Head.returnCurrCommit();

        if (fileInCWD != null) {
            for (String n : fileInCWD) {
                //To further compare, we create a temporary blob
                Blob tmp = new Blob(join(CWD, n));
                String contentID = sha1(serialize(tmp));
                //First case:
                // Case 1: Tracked in current commit
                if (curr.getBlobs().containsKey(n)) {
                    String trackedId = curr.getBlobs().get(n);
                    if (!contentID.equals(trackedId)) {
                        if (allAdditionStage == null
                                || !allAdditionStage.contains(n)) {
                            modifiedNotStaged.add(n + " (modified)");
                        } else {
                            String stagedId = readContentsAsString(
                                    join(additionByName, n));
                            if (!contentID.equals(stagedId)) {
                                modifiedNotStaged.add(n + " (modified)");
                            }
                        }
                    }
                } else if (allAdditionStage != null
                        && allAdditionStage.contains(n)) {
                    // Case 2: Only staged (not tracked) - use else if
                    // to avoid duplication
                    String stagedId = readContentsAsString(
                            join(additionByName, n));
                    if (!contentID.equals(stagedId)) {
                        modifiedNotStaged.add(n + " (modified)");
                    }
                }
            }
        }
        //Third Case:
        if (allAdditionStage != null) {
            for (String n : allAdditionStage) {
                if (fileInCWD != null) {
                    if (!fileInCWD.contains(n)) {
                        modifiedNotStaged.add(n + " (deleted)");
                    }
                }
            }
        }

        //Fourth Case:
        for (String n : curr.getBlobs().keySet()) {
            if (fileInCWD != null) {
                if (!fileInCWD.contains(n)
                        && !allRemovalFileName.contains(n)) {
                    modifiedNotStaged.add(n + " (deleted)");
                }
            }
        }

        Collections.sort(modifiedNotStaged);
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String s : modifiedNotStaged) {
            System.out.println(s);
        }
        if (modifiedNotStaged.isEmpty()) {
            System.out.println();
        }

        //Print the untracked files;
        //The files in the CWD that aren't tracked by current commit nor staged
        // Files present in the working directory but neither staged for addition nor tracked.
        // This includes files that have been staged for removal, but then re-created without Gitlet?? knowledge.
        // Ignore any subdirectories that may have been introduced, since Gitlet does not deal with them.

        List<String> untracked = new ArrayList<>();
        if (fileInCWD != null) {
            for (String s : fileInCWD) {
                // Exclude files that are staged for addition or removal,
                // or tracked in current commit
                boolean isStagedForAddition = allAdditionStage != null
                        && allAdditionStage.contains(s);
                boolean isStagedForRemoval = allRemovalFileName.contains(s);
                boolean isTracked = curr.getBlobs().containsKey(s);
                
                if (!isStagedForAddition && !isStagedForRemoval && !isTracked) {
                    untracked.add(s);
                }
            }
        }

        Collections.sort(untracked);
        System.out.println("=== Untracked Files ===");
        for (String s : untracked) {
            System.out.println(s);
        }
        System.out.println();
    }

    /** Create a new branch given the branch name, the head commit of this branch is
     * initialized by the current head commit
     * @param branchName
     */
    public static void branch(String branchName) {
        File newBranch = join(branch, branchName);

        if (newBranch.exists()) {
            throw new GitletException("A branch with that name already exists.");
        }

        Branch b = new Branch(branchName);
        b.createBranchFile();
        Branch.addCommit(branchName, Head.currentHeadId());
    }

    /** Normalize line endings so CRLF/LF comparisons are consistent across OSes. */
    private static String normalizeLineEndings(String content) {
        if (content == null) {
            return null;
        }
        return content.replace("\r\n", "\n");
    }

    /** Removes a branch pointer without changing any commits
     * @param branchName
     */

    public static void rmBranch(String branchName) {
        File b = join(branch, branchName);

        if (Objects.equals(branchName, Branch.readCurrBranchName())) {
            throw new GitletException("Cannot remove the current branch.");
        }
        if (!b.exists()) {
            throw new GitletException("A branch with that name does not exist.");
        }

        b.delete();
    }

    /** Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node.
     * @param commitId
     */

    public static void reset(String commitId) {
        String resolved = resolveCommitId(commitId);
        Commit target = Commit.readCommit(resolved);
        Commit curr = Head.returnCurrCommit();

        // Check for untracked files that would be overwritten
        List<String> filesInCWD = plainFilenamesIn(CWD);
        if (filesInCWD != null) {
            for (String n : filesInCWD) {
                if (!curr.getBlobs().containsKey(n)
                        && target.getBlobs().containsKey(n)) {
                    throw new GitletException("There is an untracked file in "
                            + "the way; delete it, or add and commit it first.");
                }
            }
        }

        // Remove files tracked in current but not in target
        for (String n : curr.getBlobs().keySet()) {
            if (!target.getBlobs().containsKey(n)) {
                restrictedDelete(join(CWD, n));
            }
        }

        // Write all files tracked in target commit
        for (String n : target.getBlobs().keySet()) {
            checkout(resolved, n);
        }

        // Clear staging area fully (additions, index, removals)
        clearStaging();

        // Update HEAD and move current branch's head to target
        Head.writeHeadId(resolved);
        readObject(join(branch, Branch.readCurrBranchName()), Branch.class)
                .changeHeadCommit(resolved);
    }

    public static void merge(String branchName) {
        //Print some errors:
        List<String> adds = plainFilenamesIn(addition);
        List<String> rms = plainFilenamesIn(removal);
        if ((adds != null && !adds.isEmpty()) || (rms != null && !rms.isEmpty())) {
            throw new GitletException("You have uncommitted changes.");
        }
        File b = join(branch, branchName);

        if (!b.exists()) {
            throw new GitletException("A branch with that name does not exist.");
        }

        if (Branch.readCurrBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        //Find the split point
        String givenHead = Branch.readBranch(branchName).getHeadCommit();
        String currHead = Branch.readCurrHeadCommit();
        
        // Check if given branch is an ancestor of current branch
        // This means given branch's head should be reachable from current branch's head
        if (isAncestor(givenHead, currHead)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }
        
        String sp = findSplitPoint(Branch.readBranch(branchName),
                Branch.readBranch(Branch.readCurrBranchName()));

        //Split point exceptions - this check is redundant since
        // isAncestor above already handles it. But keeping it as a
        // defensive check in case split point equals given head for
        // other reasons
        if (sp != null && sp.equals(givenHead)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        // Fast-forward: when split point equals current head
        // According to spec: "If the split point is the current
        // branch, then the effect is to check out the given branch,
        // and the operation ends after printing the message"
        if (sp != null && sp.equals(Branch.readCurrHeadCommit())) {
            checkoutWithBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        //Normal case
        //Check the untracked files in curr commit, which exists
        // in CWD and given commit
        Commit givenHeadCommit = Commit.readCommit(
                Branch.readBranch(branchName).getHeadCommit());
        Commit currHeadCommit = Commit.readCommit(
                Branch.readCurrHeadCommit());
        Commit spCommit = Commit.readCommit(sp);

        // Track if merge encountered conflicts
        boolean hasConflict = false;

        // Check untracked files that would be overwritten
        // First, collect all files that exist in CWD
        List<String> filesInCWD = plainFilenamesIn(CWD);
        List<String> stagedAdditionNames = plainFilenamesIn(additionByName);
        if (filesInCWD != null) {
            for (String n : filesInCWD) {
                // Skip if file is deleted in both branches (will be
                // handled in both delete case)
                boolean bothDeleted = spCommit.getBlobs()
                        .containsKey(n)
                        && !currHeadCommit.getBlobs().containsKey(n)
                        && !givenHeadCommit.getBlobs().containsKey(n);
                if (bothDeleted) {
                    continue; // Skip this file, it will be handled later
                }
                // Check if file would be overwritten by given branch
                boolean stagedForAddition = stagedAdditionNames != null
                        && stagedAdditionNames.contains(n);
                if (!currHeadCommit.getBlobs().containsKey(n)
                        && givenHeadCommit.getBlobs().containsKey(n)) {
                    if (stagedForAddition) {
                        continue; // considered tracked for purposes of conflict check
                    }
                    File cwdFile = join(CWD, n);
                    if (!cwdFile.exists()) {
                        continue; // nothing to overwrite
                    }

                    String givenBlobId = givenHeadCommit.getBlobs().get(n);
                    String givenContent = normalizeLineEndings(
                            Blob.readBlobContent(givenBlobId));
                    String cwdContent = normalizeLineEndings(
                            readContentsAsString(cwdFile));
                    if (Objects.equals(cwdContent, givenContent)) {
                        continue; // safe: file matches given branch version
                    }
                    throw new GitletException("There is an untracked file in "
                            + "the way; delete it, or add and commit it first.");
                }
            }
        }
        /* Any files that have been modified in the given branch since
          the split point, but not modified in the current branch since
          the split point should be changed to their versions in the
          given branch (checked out from the commit at the front of the
          given branch).
         */
        /*Any files that have been modified in the current branch but
          not in the given branch since the split point should stay as
          they are.
         */
        for (String n : givenHeadCommit.getBlobs().keySet()) {
            //Check is modified since sp
            if (spCommit.getBlobs().containsKey(n)
                    && !Objects.equals(spCommit.getBlobs().get(n),
                            givenHeadCommit.getBlobs().get(n))) {
                if (currHeadCommit.getBlobs().containsKey(n)
                        && Objects.equals(spCommit.getBlobs().get(n),
                                currHeadCommit.getBlobs().get(n))) {
                    //modified in given branch but not modified in curr branch
                    //change the version
                    checkout(Branch.readBranch(branchName)
                            .getHeadCommit(), n);
                    add(join(CWD, n));
                }
            }
            //Any files that were not present at the split point
            //and are present only in the given branch should be checked
            //out and staged. But if it's also in current branch with
            //different content, it's a conflict (handled later)
            if (!spCommit.getBlobs().containsKey(n)
                    && !currHeadCommit.getBlobs().containsKey(n)) {
                checkout(Branch.readBranch(branchName)
                        .getHeadCommit(), n);
                add(join(CWD, n));
            }
            // Conflict: new file in both branches with different content
            if (!spCommit.getBlobs().containsKey(n)
                    && currHeadCommit.getBlobs().containsKey(n)
                    && !Objects.equals(currHeadCommit.getBlobs().get(n),
                            givenHeadCommit.getBlobs().get(n))) {
                String givenContent = Blob.readBlobContent(
                        givenHeadCommit.getBlobs().get(n));
                String currContent = Blob.readBlobContent(
                        currHeadCommit.getBlobs().get(n));
                // Handle null content - treat deleted files as empty
                // strings
                if (givenContent == null) {
                    givenContent = "";
                }
                if (currContent == null) {
                    currContent = "";
                }
                // Format: <<<<<<< HEAD\n<curr>=======\n<given>>>>>>>\n
                // Use straight concatenation - if file has no newline
                // at end, separators will be on same line
                String conflictContent = "<<<<<<< HEAD\n" + currContent
                        + "=======\n" + givenContent + ">>>>>>>\n";
                writeContents(join(CWD, n), conflictContent);
                add(join(CWD, n)); // Stage the conflict file
                hasConflict = true; // Mark that we encountered a conflict
                // Continue to create merge commit (don't throw exception)
            }
            //Merge Conflicts
            // Conflict: file existed at split point, deleted in
            // current branch, but modified in given branch
            // Note: If file is also deleted in given branch, no
            // conflict (handled in both delete case)
            if (spCommit.getBlobs().containsKey(n)
                    && !currHeadCommit.getBlobs().containsKey(n)
                    && givenHeadCommit.getBlobs().containsKey(n)
                    && !Objects.equals(givenHeadCommit.getBlobs().get(n),
                            spCommit.getBlobs().get(n))) {
                String givenContent = Blob.readBlobContent(
                        givenHeadCommit.getBlobs().get(n));
                // Handle null content - treat deleted file as empty
                // string
                if (givenContent == null) {
                    givenContent = "";
                }
                // Format: <<<<<<< HEAD\n=======\n<given>>>>>>>\n
                // Use straight concatenation - deleted file in current
                // branch is empty
                String conflictContent = "<<<<<<< HEAD\n=======\n"
                        + givenContent + ">>>>>>>\n";

                writeContents(join(CWD, n), conflictContent);
                add(join(CWD, n)); // Stage the conflict file
                hasConflict = true; // Mark that we encountered a conflict
                // Continue to create merge commit (don't throw exception)
            }
        }

        // Check files from split point to handle special cases
        for (String splitFile : spCommit.getBlobs().keySet()) {
            // Handle both delete case: file existed at split point,
            // deleted in both branches
            // According to spec: "If a file was removed from both the
            // current and given branch, but a file of the same name is
            // present in the working directory, it is left alone and
            // continues to be absent (not tracked nor staged) in the merge."
            if (!currHeadCommit.getBlobs().containsKey(splitFile)
                    && !givenHeadCommit.getBlobs().containsKey(splitFile)) {
                // File deleted in both branches - leave working directory file alone (do nothing)
                // The file will remain untracked and unstaged
                continue; // Skip this file
            }
            // Handle: file existed at split point, unmodified in given
            // branch, absent in current branch
            // According to spec: "Any files present at the split point,
            // unmodified in the given branch, and absent in the current
            // branch should remain absent."
            // This case is already handled implicitly - if file is not
            // in currHeadCommit, it won't be processed in the
            // currHeadCommit loop, so it remains absent. No action needed.
        }
        
        for (String s : currHeadCommit.getBlobs().keySet()) {
            //Any files present at the split point, unmodified in the
            //current branch, and absent in the given branch should be
            //removed (and untracked).
            if (spCommit.getBlobs().containsKey(s)
                    && Objects.equals(currHeadCommit.getBlobs().get(s),
                            spCommit.getBlobs().get(s))
                    && !givenHeadCommit.getBlobs().containsKey(s)) {
                rm(s);
                continue; // Skip conflict checking for this file
            }

            //Merge conflicts
            // Conflict occurs when:
            // 1. File exists in both branches but with different content
            // 2. File was modified in both branches since split point
            //    (if split point exists)
            //    OR file is new in both branches (if split point doesn't
            //    exist) with different content
            if (givenHeadCommit.getBlobs().containsKey(s)
                    && !givenHeadCommit.getBlobs().get(s)
                            .equals(currHeadCommit.getBlobs().get(s))) {
                boolean isConflict = false;
                
                if (spCommit.getBlobs().containsKey(s)) {
                    // File existed at split point
                    boolean currModified = !Objects.equals(
                            spCommit.getBlobs().get(s),
                            currHeadCommit.getBlobs().get(s));
                    boolean givenModified = !Objects.equals(
                            spCommit.getBlobs().get(s),
                            givenHeadCommit.getBlobs().get(s));
                    
                    if (currModified && givenModified) {
                        // Both modified - check if results are the same
                        // If results are the same, auto-merge (no conflict)
                        // If results are different, conflict
                        if (!Objects.equals(currHeadCommit.getBlobs()
                                .get(s), givenHeadCommit.getBlobs()
                                .get(s))) {
                            isConflict = true;
                        }
                        // If results are the same, isConflict stays false,
                        // file stays as is (auto-merged)
                    } else if (currModified && !givenModified) {
                        // Current modified, given unmodified - keep current
                        // (already in working directory)
                        // No action needed, file stays as is
                        continue;
                    } else if (!currModified && givenModified) {
                        // Current unmodified, given modified - should be
                        // handled in givenHeadCommit loop
                        // But if we reach here, it means it wasn't handled
                        // there (maybe conflict case)
                        // Actually this case should be handled in lines
                        // 695-701, so if we reach here it might be an
                        // error, but to be safe, we'll skip it
                        continue;
                    }
                    // If neither modified but contents are different,
                    // that shouldn't happen
                } else {
                    // File didn't exist at split point - new in both
                    // branches
                    // If both have it but different, it's a conflict
                    isConflict = true;
                }
                
                if (isConflict) {
                    String givenContent = Blob.readBlobContent(
                            givenHeadCommit.getBlobs().get(s));
                    String currContent = Blob.readBlobContent(
                            currHeadCommit.getBlobs().get(s));
                    // Handle null content - treat deleted files as empty
                    // strings
                    if (givenContent == null) {
                        givenContent = "";
                    }
                    if (currContent == null) {
                        currContent = "";
                    }
                    // Format: <<<<<<< HEAD\n<curr>=======\n<given>>>>>>>\n
                    // Use straight concatenation - if file has no newline
                    // at end, separators will be on same line
                    String conflictContent = "<<<<<<< HEAD\n" + currContent
                            + "=======\n" + givenContent + ">>>>>>>\n";

                    writeContents(join(CWD, s), conflictContent);
                    add(join(CWD, s)); // Stage the conflict file
                    hasConflict = true; // Mark that we encountered a conflict
                    // Continue to create merge commit (don't throw exception)
                }
            }

            //Merge Conflicts
            // Conflict: file existed at split point, deleted in given
            // branch, but modified in current branch
            // This must be checked BEFORE the "both branches have
            // different content" check
            if (spCommit.getBlobs().containsKey(s)
                    && !givenHeadCommit.getBlobs().containsKey(s)
                    && !Objects.equals(currHeadCommit.getBlobs().get(s),
                            spCommit.getBlobs().get(s))) {
                String currContent = Blob.readBlobContent(
                        currHeadCommit.getBlobs().get(s));
                // Handle null content - treat deleted file as empty string
                if (currContent == null) {
                    currContent = "";
                }
                // Format: <<<<<<< HEAD\n<curr>=======\n>>>>>>>\n
                // Use straight concatenation - deleted file in given
                // branch is empty
                String conflictContent = "<<<<<<< HEAD\n" + currContent
                        + "=======\n>>>>>>>\n";

                writeContents(join(CWD, s), conflictContent);
                add(join(CWD, s)); // Stage the conflict file
                hasConflict = true; // Mark that we encountered a conflict
                continue; // Skip further processing for this file
            }
        }

        // For merge commits, even if no files are staged, we still need to create the commit
        // Check if staging area is empty
        List<String> stagedAdds = plainFilenamesIn(addition);
        List<String> stagedRms = plainFilenamesIn(removal);
        boolean noStagedAdds = (stagedAdds == null || stagedAdds.isEmpty());
        boolean noStagedRemoves = (stagedRms == null || stagedRms.isEmpty());
        
        if (noStagedAdds && noStagedRemoves) {
            // No changes staged, but this is a merge - create commit
            // anyway
            // This can happen when both branches deleted the same file,
            // or other cases
            String currentCommitId = Branch.readCurrHeadCommit();
            Commit newCommit = new Commit("Merged " + branchName + " into "
                    + Branch.readCurrBranchName() + ".",
                    currentCommitId,
                    Branch.readBranch(branchName).getHeadCommit());
            String mergeCommitId = newCommit.createCommitFile();
            clearStaging();
            Branch.addCommit(Branch.readCurrBranchName(),
                    mergeCommitId);
            Head.writeHeadId(mergeCommitId);
        } else {
            // Use the actual current head commit ID, not the calculated one
            // Temporarily set HEAD to use the correct commit ID for
            // commit() method. But actually, commit() uses
            // sha1(serialize(Head.returnCurrCommit())). So we need to
            // ensure HEAD points to the correct commit. Actually,
            // commit() should work correctly as HEAD should already
            // point to the right commit
            commit("Merged " + branchName + " into "
                    + Branch.readCurrBranchName() + ".",
                    Branch.readBranch(branchName).getHeadCommit());
        }
        
        // After committing, check if we encountered conflicts and print message
        if (hasConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /**
     * The private function helps to find the split point, given the two
     * branches and returns the commitID of the split point commit.If
     * they don't have shared points, returns null.
     * The logic of which is the crossing linkedlist problem, the time
     * complexity is O(N+M) and space complexity is O(1)
     * @param b1
     * @param b2
     * @return CommitId string
     */
    private static String findSplitPoint(Branch b1, Branch b2) {
        String p1 = b1.getHeadCommit();
        String p2 = b2.getHeadCommit();

        while (!Objects.equals(p1, p2)) {
            if (p1 == null) {
                p1 = b2.getHeadCommit();
            } else {
                Commit c1 = Commit.readCommit(p1);
                if (c1.getParent().isEmpty()
                        || c1.getParent().get(0) == null) {
                    p1 = null;
                } else {
                    p1 = c1.getParent().get(0);
                }
            }
            
            if (p2 == null) {
                p2 = b1.getHeadCommit();
            } else {
                Commit c2 = Commit.readCommit(p2);
                if (c2.getParent().isEmpty()
                        || c2.getParent().get(0) == null) {
                    p2 = null;
                } else {
                    p2 = c2.getParent().get(0);
                }
            }
        }

        return p1;
    }

    /**
     * Check if commit1 is an ancestor of commit2 (commit1 is reachable from commit2)
     * @param commit1 The potential ancestor commit ID
     * @param commit2 The commit to check from
     * @return true if commit1 is an ancestor of commit2
     */
    private static boolean isAncestor(String commit1, String commit2) {
        if (commit1 == null || commit2 == null) {
            return false;
        }
        if (commit1.equals(commit2)) {
            return true;
        }
        
        Commit c2 = Commit.readCommit(commit2);
        if (c2 == null) {
            return false;
        }
        
        // Check all parents (including merge commits)
        for (String parent : c2.getParent()) {
            if (parent != null && isAncestor(commit1, parent)) {
                return true;
            }
        }
        
        return false;
    }

    public static void addRemote(String remoteName, String remotePath) {
        List<String> allRemote = plainFilenamesIn(remote);

        if (allRemote != null && !allRemote.isEmpty()) {
            if (allRemote.contains(remoteName)) {
                System.out.println("A remote with that name already exists.");
                return;
            }
        }

        File newRemote = join(remote, remoteName);

        try {
            newRemote.createNewFile();
        } catch (IOException e) {
            System.err.println("???????????O???: " + e.getMessage());
        }

        writeContents(newRemote, remotePath);
    }

    public static void rmRemote(String remoteName) {
        List<String> allRemote = plainFilenamesIn(remote);

        if (allRemote == null || !allRemote.contains(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }

        File remoteTobeDeleted = join(remote, remoteName);
        remoteTobeDeleted.delete();
    }

    /** Attempts to append the current branch?? commits to the end of the given branch
     * at the given remote.This command only works if the remote branch?? head is in the history
     * of the current local head. which means that the local branch contains some commits in the future
     * of the remote branch. In this case, append the future commits to the remote branch.
     * Then, the remote should reset to the front of the appended commits
     * @param remoteName
     * @param remoteBranch
     */
    public static void push(String remoteName, String remoteBranch) {
        File remotePath = new File(readContentsAsString(join(remote, remoteName)));

        if (!remotePath.exists()) {
            throw new GitletException("Remote directory not found.");
        }

        //if the branch does not exist, create first;
        File rb = join(remotePath, "branch", remoteBranch);
        File rmCommit = join(remotePath, "commit");

        if (!rb.exists()) {
            //create a branch at remote
            try {
                rb.createNewFile();
            } catch (IOException e) {
                System.err.println("???????????O???: " + e.getMessage());
            }
            Branch remBranch = new Branch(remoteBranch);


            //Copy all the commits in local branch
            Commit p = Commit.readCommit(Branch.readCurrHeadCommit());

            while (!p.getParent().isEmpty()
                    && p.getParent().get(0) != null) {
                copyCommit(GITLET_DIR, sha1(serialize(p)), remotePath);

                p = Commit.readCommit(p.getParent().get(0));
            }

            //Set the Branch Head Commit;
            remBranch.changeHeadCommitRemote(Branch.readCurrHeadCommit());
            writeObject(rb, remBranch);
        }

        //Check whether it's a historical commit
        Branch rbranch = readObject(rb, Branch.class);
        Boolean isHistorical = false;
        Commit p = Commit.readCommit(Branch.readCurrHeadCommit());

        while (p != null && !p.getParent().isEmpty()
                && p.getParent().get(0) != null) {
            String remoteHead = rbranch.getHeadCommit();
            String currHead = sha1(serialize(Commit.readCommit(
                    Branch.readCurrHeadCommit())));
            isHistorical = Objects.equals(remoteHead, currHead)
                    || isAncestor(remoteHead, currHead);

            p = Commit.readCommit(p.getParent().get(0));
        }

        if (!isHistorical) {
            throw new GitletException("Please pull down remote changes before pushing.");
        } else {
            //Copy the commit from local to remote, from the split point
            Commit q = Commit.readCommit(Branch.readCurrHeadCommit());
            String remoteHead = rbranch.getHeadCommit();
            while (q != null && !sha1(serialize(q)).equals(remoteHead)) {
                copyCommit(GITLET_DIR, sha1(serialize(q)), remotePath);
                if (q.getParent().isEmpty()
                        || q.getParent().get(0) == null) {
                    break;
                }
                q = Commit.readCommit(q.getParent().get(0));
            }

            //Set new headCommit - fast-forwarded
            rbranch.changeHeadCommitRemote(Branch.readCurrHeadCommit());
            writeObject(rb, rbranch);
        }
    }

    /** Brings down commits from the remote Gitlet repository into the
     * local Gitlet repository. Basically, this copies all commits and
     * blobs from the given branch in the remote repository (that are
     * not already in the current repository) into a branch named
     * [remote name]/[remote branch name] in the local .gitlet (just as
     * in real Git), changing [remote name]/[remote branch name] to
     * point to the head commit (thus copying the contents of the branch
     * from the remote repository to the current one). This branch is
     * created in the local repository if it did not previously exist.
     * @param remoteName
     * @param remoteBranch
     */
    public static void fetch(String remoteName, String remoteBranch) {
        File remotePath = new File(readContentsAsString(join(remote, remoteName)));

        if (!remotePath.exists()) {
            throw new GitletException("Remote directory not found.");
        }

        File rmBranch = join(remotePath, "branch", remoteBranch);

        if (!rmBranch.exists()) {
            throw new GitletException("That remote does not have that branch.");
        }

        if (!join(branch, remoteName + "/" + remoteBranch).exists()) {
            File newBranch = join(branch, remoteName + "/" + remoteBranch);

            Branch b = new Branch(remoteName + "/" + remoteBranch);
            b.createBranchFile();
        }

        Branch targetBranch = readObject(join(branch, remoteName + "/"
                + remoteBranch), Branch.class);
        String rmHeadId = readObject(rmBranch, Branch.class)
                .getHeadCommit();
        Branch.addCommit(remoteName + "/" + remoteBranch, rmHeadId);
        Commit p = readObject(join(remotePath, "commit", rmHeadId),
                Commit.class);

        while (!p.getParent().isEmpty()
                && p.getParent().get(0) != null) {
            if (!join(commit, rmHeadId).exists()) {
                copyCommit(remotePath, rmHeadId, GITLET_DIR);
            }

            rmHeadId = p.getParent().get(0);
            p = readObject(join(remotePath, "commit", rmHeadId),
                    Commit.class);
        }
    }

    public static void pull(String remoteName, String remoteBranch) {
        fetch(remoteName, remoteBranch);
        merge(remoteName + "/" + remoteBranch);
    }

    /** this helper functions copies commit and its corresponding blobs
     * from local Gitlet repo to remote gitlet repo
     * @param localPath
     * @param commitIdA
     * @param remotePath
     */
    private static void copyCommit(File localPath, String commitIdA,
            File remotePath) {
        File localCommit = join(localPath, "commit");
        File localBlobs = join(localPath, "blob");
        File remoteCommit = join(remotePath, "commit");
        File remoteBlobs = join(remotePath, "blob");

        //We assume the commit exist
        //firstly copy the commit
        File c = join(localCommit, commitIdA);
        try {
            join(remoteCommit, commitIdA).createNewFile();
        } catch (IOException e) {
            System.err.println("???????????O???: " + e.getMessage());
        }
        writeContents(join(remoteCommit, commitIdA), readContents(c));

        Commit cm = readObject(join(localCommit, commitIdA), Commit.class);
        //Access to the blobs and copy them:
        for (String k : cm.getBlobs().keySet()) {
            File b = join(localBlobs, k);
            try {
                join(remoteBlobs, k).createNewFile();
            } catch (IOException e) {
                System.err.println("???????????O???: " + e.getMessage());
            }
            writeObject(join(remoteBlobs, k), readObject(b, Blob.class));
        }
    }
}
