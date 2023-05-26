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
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.out.println("FIRST");
        System.out.println("==================================");
        // first test 10KB
        compressionAlgorithmMetrics(new Huffman(), "lorem10K.txt", "Huffman Algorithm");
        compressionAlgorithmMetrics(new LZW(), "lorem10K.txt", "LZW Algorithm");
        compressionAlgorithmMetrics(new ArithmeticCoding(), "lorem10K.txt", "Arithmetic Algorithm");

        System.out.println("SECOND");
        System.out.println("==================================");
        // second test same character 4.7 MB
        compressionAlgorithmMetrics(new Huffman(), "Test_LZW.txt", "Huffman Algorithm");
        compressionAlgorithmMetrics(new LZW(), "Test_LZW.txt", "LZW Algorithm");
        compressionAlgorithmMetrics(new ArithmeticCoding(), "Test_LZW.txt", "Arithmetic Algorithm");

        System.out.println("THIRD");
        System.out.println("==================================");
        // third test 258 MB
        compressionAlgorithmMetrics(new Huffman(), "lorem250M.txt", "Huffman Algorithm");
        compressionAlgorithmMetrics(new LZW(), "lorem250M.txt", "LZW Algorithm");
        compressionAlgorithmMetrics(new ArithmeticCoding(), "lorem250M.txt", "Arithmetic Algorithm");

        System.out.println("FORTH");
        System.out.println("==================================");
        // forth test 504 MB
        compressionAlgorithmMetrics(new Huffman(), "lorem504M.txt", "Huffman Algorithm");
        compressionAlgorithmMetrics(new LZW(), "lorem504M.txt", "LZW Algorithm");
        compressionAlgorithmMetrics(new ArithmeticCoding(), "lorem504M.txt", "Arithmetic Algorithm");

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
            (CompressionAlgorithm compressionAlgorithm, String path, String msg) throws IOException, NoSuchAlgorithmException {

        String compressionPath = compressionAlgorithm.getCompressedPath(path);
        String decompressionPath = compressionAlgorithm.getDecompressedPath(compressionPath);

        compress(compressionAlgorithm, path, "Compression - " + msg);
        getCompressionRate(path, compressionPath);
        decompress(compressionAlgorithm, compressionPath, "Decompression - " + msg);

        if(compareFiles(path, decompressionPath))
            System.out.println("Hash Digest are equal in " + msg);
        else
            System.out.println("Hash Digest NOT equal in " + msg);

        System.out.println("============================================");
    }
}
