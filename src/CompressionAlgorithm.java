import java.io.IOException;

/**
 * Represents the compression algorithm
 */
public interface CompressionAlgorithm {

    /**
     * Compresses a given file and writes the compressed file output.
     */
    void compress(String filePath) throws IOException;

    /**
     * Decompresses a given file and writes the decompressed file output.
     */
    void decompress(String filePath) throws IOException;

    /**
     * Returns the compressed file name
     */
    String getCompressedPath(String path);

    /**
     * Returns the decompressed file name
     */
    String getDecompressedPath(String path);
}
