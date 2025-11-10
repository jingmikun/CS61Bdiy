
            try {
                join(removal, thisBlobId).createNewFile();
            } catch (IOException e) {
                System.err.println("创建文件时发生IO错误: " + e.getMessage());
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
