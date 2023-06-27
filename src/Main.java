import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Main driver code (used for testing).
 */
public class Main {
    private static final String FILE_PATH = "lorem.txt"; // File to be tested

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        compressionAlgorithmMetrics(new Huffman(), FILE_PATH, "Huffman Algorithm");
        compressionAlgorithmMetrics(new LZW(), FILE_PATH, "LZW Algorithm");
        compressionAlgorithmMetrics(new ArithmeticCoding(), FILE_PATH, "Arithmetic Algorithm");
    }

    private static boolean compareFiles(String file1, String file2) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest1 = getDigest(file1, md);
        byte[] digest2 = getDigest(file2, md);
        return MessageDigest.isEqual(digest1, digest2);
    }

    private static byte[] getDigest(String fileName, MessageDigest md) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileName);
             DigestInputStream dis = new DigestInputStream(fis, md)) {
            byte[] buffer = new byte[4096];
            while (dis.read(buffer) != -1) { }
            return md.digest();
        }
    }

    private static void compress(CompressionAlgorithm compressionAlgorithm, String path, String msg) throws IOException {
        long start = System.currentTimeMillis();
        compressionAlgorithm.compress(path);
        System.out.println(msg + " "+ (System.currentTimeMillis() - start) + " ms");
    }

    private static void getCompressionRate(String originalPath, String compressedPath) {
        long originalSize = new File(originalPath).length();
        long compressedSize = new File(compressedPath).length();
        System.out.println("Ratio Original/Compressed = " + ((float)originalSize/(float)compressedSize));
    }

    private static void decompress(CompressionAlgorithm compressionAlgorithm, String path, String msg) throws IOException {
        long start = System.currentTimeMillis();
        compressionAlgorithm.decompress(path);
        System.out.println(msg + " " + (System.currentTimeMillis() - start) + " ms");
    }

    private static void compressionAlgorithmMetrics
            (CompressionAlgorithm compressionAlgorithm, String path, String algorithmName)
            throws IOException, NoSuchAlgorithmException {

        String compressionPath = compressionAlgorithm.getCompressedPath(path);
        String decompressionPath = compressionAlgorithm.getDecompressedPath(compressionPath);

        compress(compressionAlgorithm, path, "Compression - " + algorithmName);
        getCompressionRate(path, compressionPath);
        decompress(compressionAlgorithm, compressionPath, "Decompression - " + algorithmName);

        if (compareFiles(path, decompressionPath))
            System.out.println("Hash Digest are equal in " + algorithmName);
        else
            System.out.println("Hash Digest NOT equal in " + algorithmName);

        System.out.println("============================================");
    }
}
