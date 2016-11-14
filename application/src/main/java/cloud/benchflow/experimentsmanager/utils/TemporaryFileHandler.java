package cloud.benchflow.experimentsmanager.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Simone D'Avico (simonedavico@gmail.com)
 *         <p>
 *         Created on 02/12/15.
 */
public class TemporaryFileHandler implements AutoCloseable {

    private File file;

    public TemporaryFileHandler(final InputStream in, final Path path) throws IOException {
        Files.copy(in, path);
        this.file = path.toFile();//new File(path.toString());
    }

    public File getFile() {
        return file;
    }

    @Override
    public void close() throws IOException {
        Files.deleteIfExists(file.toPath());
    }
}
