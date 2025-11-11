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
         * In this function, we check each blob in the parent commit (the default
         * contents for the new commit). If there is a staged blob with new
         * content, we swap it in. The time complexity is O(m log n): m is the
         * number of staged files and n is the number of files in the parent
         * commit.
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

        // Try direct lookup first to support remote-tracking branches (names containing '/')
        Branch b = null;
        File branchFile = join(branch, branchName);
        if (branchFile.exists()) {
            b = readObject(branchFile, Branch.class);
        }

        if (Objects.equals(branchName, Branch.readCurrBranchName())) {
            throw new GitletException("No need to checkout the current branch.");
        }

        if (b == null && allBranchName != null) {
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
        List<String> branchNames = plainFilenamesIn(branch);
        String currentBranch = Branch.readCurrBranchName();
        List<String> stagedNames = plainFilenamesIn(additionByName);
        List<String> removalIds = plainFilenamesIn(removal);
        List<String> filesInCwd = plainFilenamesIn(CWD);
        Commit headCommit = Head.returnCurrCommit();

        printBranchSection(branchNames, currentBranch);
        printSection("Staged Files", sortedCopy(stagedNames));

        List<String> removedFiles = collectRemovedFiles(removalIds);
        printSection("Removed Files", removedFiles);

        List<String> modified = collectModifiedNotStaged(
                filesInCwd, stagedNames, removedFiles, headCommit);
        printSection("Modifications Not Staged For Commit", modified);

        List<String> untracked = collectUntrackedFiles(
                filesInCwd, stagedNames, removedFiles, headCommit);
        printSection("Untracked Files", untracked);
    }

    private static void printBranchSection(List<String> branchNames,
            String currentBranch) {
        System.out.println("=== Branches ===");
        System.out.println("*" + currentBranch);

        for (String name : sortedCopy(branchNames)) {
            if (!Objects.equals(name, currentBranch)
                    && !"current_Branch_Name".equals(name)) {
                System.out.println(name);
            }
        }
        System.out.println();
    }

    private static void printSection(String title, List<String> entries) {
        System.out.println("=== " + title + " ===");
        for (String entry : entries) {
            System.out.println(entry);
        }
        System.out.println();
    }

    private static List<String> sortedCopy(List<String> source) {
        List<String> copy = new ArrayList<>();
        if (source != null) {
            copy.addAll(source);
        }
        Collections.sort(copy);
        return copy;
    }

    private static List<String> collectRemovedFiles(List<String> removalIds) {
        List<String> removed = new ArrayList<>();
        if (removalIds == null) {
            return removed;
        }

        for (String id : removalIds) {
            Blob stagedBlob = readObject(join(blob, id), Blob.class);
            removed.add(stagedBlob.getFilename());
        }
        Collections.sort(removed);
        return removed;
    }

    private static List<String> collectModifiedNotStaged(List<String> filesInCwd,
            List<String> stagedNames, List<String> removedFiles,
            Commit headCommit) {
        List<String> modified = new ArrayList<>();
        addWorkingDirectoryDiffs(filesInCwd, stagedNames, headCommit, modified);
        addMissingStagedFiles(filesInCwd, stagedNames, modified);
        addMissingTrackedFiles(filesInCwd, removedFiles, headCommit, modified);
        Collections.sort(modified);
        return modified;
    }

    private static void addWorkingDirectoryDiffs(List<String> filesInCwd,
            List<String> stagedNames, Commit headCommit,
            List<String> modified) {
        if (filesInCwd == null) {
            return;
        }

        for (String name : filesInCwd) {
            String workingId = workingBlobId(name);
            if (headCommit.getBlobs().containsKey(name)) {
                String trackedId = headCommit.getBlobs().get(name);
                if (!Objects.equals(workingId, trackedId)
                        && differsFromStagedVersion(name, workingId,
                        stagedNames)) {
                    modified.add(name + " (modified)");
                }
            } else if (isStagedForAddition(name, stagedNames)
                    && differsFromStagedVersion(name, workingId, stagedNames)) {
                modified.add(name + " (modified)");
            }
        }
    }

    private static boolean differsFromStagedVersion(String filename,
            String workingId, List<String> stagedNames) {
        if (!isStagedForAddition(filename, stagedNames)) {
            return true;
        }
        String stagedId = stagedBlobId(filename);
        return !Objects.equals(workingId, stagedId);
    }

    private static boolean isStagedForAddition(String filename,
            List<String> stagedNames) {
        return stagedNames != null && stagedNames.contains(filename);
    }

    private static String stagedBlobId(String filename) {
        File staged = join(additionByName, filename);
        if (!staged.exists()) {
            return null;
        }
        return readContentsAsString(staged);
    }

    private static void addMissingStagedFiles(List<String> filesInCwd,
            List<String> stagedNames, List<String> modified) {
        if (stagedNames == null) {
            return;
        }
        for (String name : stagedNames) {
            if (filesInCwd == null || !filesInCwd.contains(name)) {
                modified.add(name + " (deleted)");
            }
        }
    }

    private static void addMissingTrackedFiles(List<String> filesInCwd,
            List<String> removedFiles, Commit headCommit,
            List<String> modified) {
        Set<String> cwd = filesInCwd == null
                ? Collections.emptySet() : new HashSet<>(filesInCwd);
        for (String tracked : headCommit.getBlobs().keySet()) {
            if (!cwd.contains(tracked) && !removedFiles.contains(tracked)) {
                modified.add(tracked + " (deleted)");
            }
        }
    }

    private static String workingBlobId(String filename) {
        Blob tmp = new Blob(join(CWD, filename));
        return sha1(serialize(tmp));
    }

    private static List<String> collectUntrackedFiles(List<String> filesInCwd,
            List<String> stagedNames, List<String> removedFiles,
            Commit headCommit) {
        List<String> untracked = new ArrayList<>();
        if (filesInCwd == null) {
            return untracked;
        }
        for (String name : filesInCwd) {
            boolean stagedAdd = isStagedForAddition(name, stagedNames);
            boolean stagedRemoval = removedFiles.contains(name);
            boolean tracked = headCommit.getBlobs().containsKey(name);
            if (!stagedAdd && !stagedRemoval && !tracked) {
                untracked.add(name);
            }
        }
        Collections.sort(untracked);
        return untracked;
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
        ensureMergeStagingClean();
        File targetBranch = join(branch, branchName);
        if (!targetBranch.exists()) {
            throw new GitletException("A branch with that name does not exist.");
        }
        if (Branch.readCurrBranchName().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }

        MergeContext ctx = buildMergeContext(branchName);
        if (handleTrivialMergeEndings(ctx)) {
            return;
        }

        ensureNoUntrackedConflicts(ctx);
        boolean hasConflict = applyMergeChanges(ctx);
        finalizeMerge(branchName, ctx.givenHeadId, hasConflict);
    }

    private static void ensureMergeStagingClean() {
        List<String> adds = plainFilenamesIn(addition);
        List<String> rms = plainFilenamesIn(removal);
        if ((adds != null && !adds.isEmpty()) || (rms != null && !rms.isEmpty())) {
            throw new GitletException("You have uncommitted changes.");
        }
    }

    private static MergeContext buildMergeContext(String branchName) {
        Branch givenBranch = Branch.readBranch(branchName);
        Branch currentBranch = Branch.readBranch(Branch.readCurrBranchName());
        String givenHeadId = givenBranch.getHeadCommit();
        String currentHeadId = currentBranch.getHeadCommit();
        String splitPointId = findSplitPoint(givenBranch, currentBranch);
        Commit splitCommit = splitPointId == null ? null
                : Commit.readCommit(splitPointId);
        return new MergeContext(
                branchName,
                givenHeadId,
                currentHeadId,
                Commit.readCommit(givenHeadId),
                Commit.readCommit(currentHeadId),
                splitCommit,
                splitPointId);
    }

    private static boolean handleTrivialMergeEndings(MergeContext ctx) {
        if (isAncestor(ctx.givenHeadId, ctx.currentHeadId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return true;
        }
        if (ctx.splitPointId != null && ctx.splitPointId.equals(ctx.givenHeadId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return true;
        }
        if (ctx.splitPointId != null && ctx.splitPointId.equals(ctx.currentHeadId)) {
            checkoutWithBranch(ctx.branchName);
            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        return false;
    }

    private static void ensureNoUntrackedConflicts(MergeContext ctx) {
        if (ctx.filesInCwd == null) {
            return;
        }
        Map<String, String> curr = ctx.currentCommit.getBlobs();
        Map<String, String> given = ctx.givenCommit.getBlobs();
        Map<String, String> split = ctx.splitCommit == null
                ? Collections.emptyMap() : ctx.splitCommit.getBlobs();

        for (String name : ctx.filesInCwd) {
            if (curr.containsKey(name)) {
                continue;
            }
            if (!given.containsKey(name)) {
                continue;
            }
            if (isBothDeleted(name, curr, given, split)) {
                continue;
            }
            if (ctx.stagedAdditionNames != null
                    && ctx.stagedAdditionNames.contains(name)) {
                continue;
            }
            File cwdFile = join(CWD, name);
            if (!cwdFile.exists()) {
                continue;
            }
            String givenContent = normalizeLineEndings(
                    Blob.readBlobContent(given.get(name)));
            String cwdContent = normalizeLineEndings(
                    readContentsAsString(cwdFile));
            if (Objects.equals(cwdContent, givenContent)) {
                continue;
            }
            throw new GitletException("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
        }
    }

    private static boolean isBothDeleted(String name, Map<String, String> curr,
            Map<String, String> given, Map<String, String> split) {
        return split.containsKey(name)
                && !curr.containsKey(name)
                && !given.containsKey(name);
    }

    private static boolean applyMergeChanges(MergeContext ctx) {
        Map<String, String> split = ctx.splitCommit == null
                ? Collections.emptyMap() : ctx.splitCommit.getBlobs();
        Map<String, String> curr = ctx.currentCommit.getBlobs();
        Map<String, String> given = ctx.givenCommit.getBlobs();
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(split.keySet());
        allFiles.addAll(curr.keySet());
        allFiles.addAll(given.keySet());

        boolean hasConflict = false;
        for (String file : allFiles) {
            String splitId = split.get(file);
            String currId = curr.get(file);
            String givenId = given.get(file);

            if (splitId == null) {
                hasConflict |= handleFileWithoutSplit(ctx, file, currId, givenId);
                continue;
            }

            if (currId != null && currId.equals(splitId) && givenId == null) {
                rm(file);
                continue;
            }

            if (currId == null && givenId == null) {
                continue;
            }

            if (!Objects.equals(currId, splitId)
                    && Objects.equals(givenId, splitId)) {
                continue;
            }

            if (Objects.equals(currId, splitId)
                    && !Objects.equals(givenId, splitId)
                    && givenId != null) {
                stageGivenVersion(ctx, file);
                continue;
            }

            if (!Objects.equals(currId, splitId)
                    && !Objects.equals(givenId, splitId)) {
                if (!Objects.equals(currId, givenId)) {
                    createConflictFile(file, currId, givenId);
                    hasConflict = true;
                }
                continue;
            }
        }
        return hasConflict;
    }

    private static boolean handleFileWithoutSplit(MergeContext ctx, String file,
            String currId, String givenId) {
        if (currId == null && givenId == null) {
            return false;
        }
        if (currId == null && givenId != null) {
            stageGivenVersion(ctx, file);
            return false;
        }
        if (currId != null && givenId == null) {
            return false;
        }
        if (Objects.equals(currId, givenId)) {
            return false;
        }
        createConflictFile(file, currId, givenId);
        return true;
    }

    private static void stageGivenVersion(MergeContext ctx, String file) {
        checkout(ctx.givenHeadId, file);
        add(join(CWD, file));
    }

    private static void createConflictFile(String file, String currBlobId,
            String givenBlobId) {
        String currContent = currBlobId == null
                ? "" : Blob.readBlobContent(currBlobId);
        String givenContent = givenBlobId == null
                ? "" : Blob.readBlobContent(givenBlobId);
        if (currContent == null) {
            currContent = "";
        }
        if (givenContent == null) {
            givenContent = "";
        }
        String conflictContent = "<<<<<<< HEAD\n" + currContent
                + "=======\n" + givenContent + ">>>>>>>\n";
        writeContents(join(CWD, file), conflictContent);
        add(join(CWD, file));
    }

    private static void finalizeMerge(String branchName, String givenHeadId,
            boolean hasConflict) {
        List<String> stagedAdds = plainFilenamesIn(addition);
        List<String> stagedRemovals = plainFilenamesIn(removal);
        boolean hasAdds = stagedAdds != null && !stagedAdds.isEmpty();
        boolean hasRemoves = stagedRemovals != null && !stagedRemovals.isEmpty();
        String message = "Merged " + branchName + " into "
                + Branch.readCurrBranchName() + ".";

        if (!hasAdds && !hasRemoves) {
            createMergeCommitWithoutStage(message, givenHeadId);
        } else {
            commit(message, givenHeadId);
        }

        if (hasConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private static void createMergeCommitWithoutStage(String message,
            String parent2) {
        String currentCommitId = Branch.readCurrHeadCommit();
        Commit newCommit = new Commit(message, currentCommitId, parent2);
        String mergeCommitId = newCommit.createCommitFile();
        clearStaging();
        Branch.addCommit(Branch.readCurrBranchName(), mergeCommitId);
        Head.writeHeadId(mergeCommitId);
    }

    private static final class MergeContext {
        private final String branchName;
        private final String givenHeadId;
        private final String currentHeadId;
        private final Commit givenCommit;
        private final Commit currentCommit;
        private final Commit splitCommit;
        private final String splitPointId;
        private final List<String> filesInCwd;
        private final List<String> stagedAdditionNames;

        private MergeContext(String branchName, String givenHeadId,
                String currentHeadId, Commit givenCommit, Commit currentCommit,
                Commit splitCommit, String splitPointId) {
            this.branchName = branchName;
            this.givenHeadId = givenHeadId;
            this.currentHeadId = currentHeadId;
            this.givenCommit = givenCommit;
            this.currentCommit = currentCommit;
            this.splitCommit = splitCommit;
            this.splitPointId = splitPointId;
            this.filesInCwd = plainFilenamesIn(CWD);
            this.stagedAdditionNames = plainFilenamesIn(additionByName);
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
    private static String findSplitPoint(Branch givenBranch, Branch currentBranch) {
        String currentHead = currentBranch.getHeadCommit();
        String givenHead = givenBranch.getHeadCommit();
        if (currentHead == null || givenHead == null) {
            return null;
        }

        Map<String, Integer> currentDistances = new HashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(currentHead);
        currentDistances.put(currentHead, 0);

        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            Commit c = Commit.readCommit(id);
            if (c == null) {
                continue;
            }
            for (String parent : c.getParent()) {
                if (parent != null && !currentDistances.containsKey(parent)) {
                    currentDistances.put(parent, currentDistances.get(id) + 1);
                    queue.addLast(parent);
                }
            }
        }

        Set<String> visited = new HashSet<>();
        queue.clear();
        queue.add(givenHead);

        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            if (id == null) {
                continue;
            }
            if (currentDistances.containsKey(id)) {
                return id;
            }
            if (visited.add(id)) {
                Commit c = Commit.readCommit(id);
                if (c == null) {
                    continue;
                }
                for (String parent : c.getParent()) {
                    if (parent != null) {
                        queue.addLast(parent);
                    }
                }
            }
        }

        return null;
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

        Commit c2;
        try {
            c2 = Commit.readCommit(commit2);
        } catch (IllegalArgumentException e) {
            return false;
        }
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

    /**
     * Attempts to append the current branch commits to the end of the given
     * branch at the specified remote. This only works if the remote branch head
     * is in the history of the current local head (meaning the local branch has
     * commits that extend the remote branch). In that case, copy the future
     * commits to the remote branch and reset the remote pointer to the newest
     * commit.
     * @param remoteName
     * @param remoteBranch
     */
    public static void push(String remoteName, String remoteBranch) {
        File remotePath = new File(readContentsAsString(join(remote, remoteName)));

        if (!remotePath.exists()) {
            throw new GitletException("Remote directory not found.");
        }

        // Ensure the remote has all blobs referenced by commits we might copy.
        copyMissingBlobs(GITLET_DIR, remotePath);

        //if the branch does not exist, create first;
        File rb = join(remotePath, "branch", remoteBranch);

        if (!rb.exists()) {
            //create a branch at remote
            File parentDir = rb.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            try {
                rb.createNewFile();
            } catch (IOException e) {
                System.err.println("???????????O???: " + e.getMessage());
            }
            Branch remBranch = new Branch(remoteBranch);


            //Copy all the commits in local branch
            String copyId = Branch.readCurrHeadCommit();
            Commit p = Commit.readCommit(copyId);
            while (copyId != null && p != null) {
                copyCommit(GITLET_DIR, copyId, remotePath);
                if (p.getParent().isEmpty()
                        || p.getParent().get(0) == null) {
                    break;
                }
                copyId = p.getParent().get(0);
                p = Commit.readCommit(copyId);
            }

            //Set the Branch Head Commit;
            remBranch.changeHeadCommitRemote(Branch.readCurrHeadCommit());
            writeObject(rb, remBranch);
        }

        //Check whether it's a historical commit
        Branch rbranch = readObject(rb, Branch.class);
        String remoteHead = rbranch.getHeadCommit();
        String currHead = Branch.readCurrHeadCommit();
        boolean isHistorical = Objects.equals(remoteHead, currHead)
                || isAncestor(remoteHead, currHead);

        if (!isHistorical) {
            throw new GitletException("Please pull down remote changes before pushing.");
        } else {
            //Copy the commit from local to remote, from the split point
            String copyId = currHead;
            Commit q = Commit.readCommit(copyId);
            while (q != null && (remoteHead == null || !copyId.equals(remoteHead))) {
                copyCommit(GITLET_DIR, copyId, remotePath);
                if (q.getParent().isEmpty()
                        || q.getParent().get(0) == null) {
                    break;
                }
                copyId = q.getParent().get(0);
                q = Commit.readCommit(copyId);
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
        copyMissingBlobs(remotePath, GITLET_DIR);
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
    }

    /** Copy any blobs present in source repo but missing in destination repo. */
    private static void copyMissingBlobs(File sourceGitlet, File destGitlet) {
        File sourceBlobDir = join(sourceGitlet, "blob");
        File destBlobDir = join(destGitlet, "blob");
        List<String> blobFiles = plainFilenamesIn(sourceBlobDir);
        if (blobFiles == null) {
            return;
        }
        for (String blobName : blobFiles) {
            File src = join(sourceBlobDir, blobName);
            File dest = join(destBlobDir, blobName);
            if (dest.exists()) {
                continue;
            }
            try {
                dest.createNewFile();
            } catch (IOException e) {
                System.err.println("???????????O???: " + e.getMessage());
            }
            writeContents(dest, readContents(src));
        }
    }
}
