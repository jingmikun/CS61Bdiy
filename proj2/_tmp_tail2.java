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
        newCommit.createCommitFile();
        clearStaging();

        //add thisCommit into commit tree (curr branch) and change the head pointer
        Branch.addCommit(Branch.readCurrBranchName(), sha1(serialize(newCommit)));
        Head.writeHeadId(sha1(serialize(newCommit)));
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

    /** The case I checkout：checkout with merely the file name,
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
