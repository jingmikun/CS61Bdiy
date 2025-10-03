package gitlet;

import java.io.File;
import java.util.Objects;

import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Jingmikun
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            throw new GitletException("Please enter a command.");
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                if (Repository.GITLET_DIR.exists()) {
                    throw new GitletException("A Gitlet version-control system already exists in the current directory.");
                }
                Repository.init();
                break;
            case "add":
                if (!Repository.GITLET_DIR.exists()) {
                    throw new GitletException("Not in an initialized Gitlet directory.");
                }
                if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }

                File newFile = join(Repository.CWD, args[1]);
                Repository.add(newFile);
                break;
            case "commit":
                if (!Repository.GITLET_DIR.exists()) {
                    throw new GitletException("Not in an initialized Gitlet directory.");
                }
                if (args.length == 1) {
                    throw new GitletException("Please enter a commit message.");
                } else if (args.length != 2) {
                    throw new GitletException("Incorrect operands.");
                }
                Repository.commit(args[1]);
                break;
            case "log":
                if (!Repository.GITLET_DIR.exists()) {
                    throw new GitletException("Not in an initialized Gitlet directory.");
                }
                if (args.length != 1) {
                    throw new GitletException("Incorrect operands.");
                }
                Repository.log();
                break;
            case "checkout":
                if (!Repository.GITLET_DIR.exists()) {
                    throw new GitletException("Not in an initialized Gitlet directory.");
                }

                //Case 1:
                if (args.length == 3 && Objects.equals(args[1], "--")) {
                    Repository.checkout(args[2]);
                }

                //Case 2:
                else if (args.length == 4 && Objects.equals(args[2], "--")) {
                    Repository.checkout(args[1], args[3]);
                } else {
                    throw new GitletException("Incorrect operands.");
                }
                break;
            default:
                throw new GitletException("No command with that name exists.");
        }
    }
}

