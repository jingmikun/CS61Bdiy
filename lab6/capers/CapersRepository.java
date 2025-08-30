package capers;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static capers.Utils.*;

/** A repository for Capers 
 * @author TODO
 * The structure of a Capers Repository is as follows:
 *
 * .capers/ -- top level folder for all persistent data in your lab12 folder
 *    - dogs/ -- folder containing all of the persistent data for dogs
 *    - story -- file containing the current story
 *
 * TODO: change the above structure if you do something different.
 */
public class CapersRepository {
    /** Current Working Directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** Main metadata folder. */
    static final File CAPERS_FOLDER = Utils.join(CWD,".capers"); // TODO Hint: look at the `join`
    /**
     * Does required filesystem operations to allow for persistence.
     * (creates any necessary folders or files)  vb
     * Remember: recommended structure (you do not have to follow):
     *
     * .capers/ -- top level folder for all persistent data in your lab12 folder
     *    - dogs/ -- folder containing all of the persistent data for dogs
     *    - story -- file containing the current story
     */
    public static void setupPersistence() {
        try {
            // Create the main .capers folder
            CAPERS_FOLDER.mkdir();

            // Define paths for the story file and dogs folder
            File story = Utils.join(CAPERS_FOLDER, "story.txt"); // Note: original code had "story"
            File dogs = Utils.join(CAPERS_FOLDER, "dogs");   // Using "dogs" to match comment

            // Create the dogs sub-folder
            dogs.mkdir();

            // Try to create the story file
            story.createNewFile();
        } catch (IOException e) {
            // If any file operation fails, this block will run
            System.err.println("An error occurred during persistence setup.");
        }
    }

    /**
     * Appends the first non-command argument in args
     * to a file called `story` in the .capers directory.
     * @param text String of the text to be appended to the story
     */
    public static void writeStory(String text) {
        File story_repo = Utils.join(CAPERS_FOLDER, "story.txt");

        String final_text = text + "\n";
        String existing_story = readContentsAsString(story_repo);
        writeContents(story_repo,existing_story,final_text);

        String whole_story = readContentsAsString(story_repo);
        System.out.println(whole_story);
    }

    /**
     * Creates and persistently saves a dog using the first
     * three non-command arguments of args (name, breed, age).
     * Also prints out the dog's information using toString().
     */
    public static void makeDog(String name, String breed, int age) {
        Dog new_dog = new Dog(name, breed, age);
        new_dog.saveDog();

        System.out.println(new_dog.toString());
    }

    /**
     * Advances a dog's age persistently and prints out a celebratory message.
     * Also prints out the dog's information using toString().
     * Chooses dog to advance based on the first non-command argument of args.
     * @param name String name of the Dog whose birthday we're celebrating.
     */
    public static void celebrateBirthday(String name) {
        Dog mydog = Dog.fromFile(name);
        mydog.haveBirthday();
        mydog.saveDog();
    }
}
