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
        ArithmeticCoding arithmeticCoding = new ArithmeticCoding();
        Huffman huffman = new Huffman();
        LZW lzw = new LZW();

        // first test, 10KB file for all
        compressionAlgorithmMetrics(huffman, "lorem10K.txt", "Huffman Algorithm");
        compressionAlgorithmMetrics(lzw, "lorem10K.txt", "LZW Algorithm");
        compressionAlgorithmMetrics(arithmeticCoding, "lorem10K.txt", "Arithmetic Algorithm");

        // second test, individual test for each algorithm
        compressionAlgorithmMetrics(huffman, "lorem160MB.txt", "Huffman Algorithm");
        compressionAlgorithmMetrics(lzw, "Test_LZW.txt", "LZW Algorithm");
        compressionAlgorithmMetrics(arithmeticCoding, "lorem20K.txt", "Arithmetic Algorithm");
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
        System.out.println("Ratio (Compressed/Original) = " + ((float)compressedSize/(float)originalSize));
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
    }
}
