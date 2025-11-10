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
        List<String> allCommit = plainFilenamesIn(commit);
        boolean flag = false;

        if (allCommit != null) {
            for (String c : allCommit) {
                Commit p = readObject(join(commit, c), Commit.class);

                if (Objects.equals(p.getMessage(), message)) {
                    System.out.println(c);
                    flag = true;
                }
            }
        }

        if (!flag) {
            throw new GitletException("Found no commit with that message.");
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
