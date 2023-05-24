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
        arithmeticCoding.compress("lorem.txt");
        arithmeticCoding.decompress("lorem.txt.AE");

        System.out.println(compareFiles("lorem.txt",
                "decompressed_lorem.txt"));

//        new File("ch05-naming.pdf.AE").delete();
//        new File("decompressed_ch05-naming.pdf").delete();
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
}
