package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Utils.*;

/**
 * A blob contains a copy of the file to be staged and the name of the file.
 * it can be serialized for being contained in the commit file;
 */
public class Blob implements Serializable {

    private String content;
    private String filename;

    public Blob(File file) {
        content = readContentsAsString(file);
        filename = file.getName();
    }

    public String getContent() {
        return content;
    }

    public String getFilename() {
        return filename;
    }

    public static String readBlobContent(String blobID) {
        return readObject(join(Repository.blob, blobID), Blob.class).getContent();
    }
}
