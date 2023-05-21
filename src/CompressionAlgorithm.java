/**
 * Represents the compression algorithm
 */
public interface CompressionAlgorithm {

    /**
     * Compresses a given file and writes the compressed file output.
     */
    public void compress(String filePath);

    /**
     * Decompresses a given file and writes the decompressed file output.
     */
    public void decompress(String filePath);
}
