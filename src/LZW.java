import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Implementation of Lempel–Ziv–Welch compression algorithm.
 */
public class LZW implements CompressionAlgorithm {

    @Override
    public void compress(String filePath) throws IOException {
        File compressedFile = new File(getCompressedPath(filePath));

        // Initialize dictionary with all possible byte values.
        Map<ByteArrayWrapper, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            byte[] b = new byte[1];
            b[0] = (byte) i;
            dictionary.put(new ByteArrayWrapper(b), i);
        }

        // Read input file into a byte array.
        byte[] inputBytes;
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath))) {
            inputBytes = inputStream.readAllBytes();
        }

        // Compress the input data.
        List<Integer> outputBytes = new ArrayList<>();
        byte[] current = new byte[0];
        for (byte b : inputBytes) {
            byte[] next = new byte[current.length + 1];
            System.arraycopy(current, 0, next, 0, current.length);
            next[current.length] = b;
            if (dictionary.containsKey(new ByteArrayWrapper(next))) {
                current = next;
            } else {
                Integer outByte = dictionary.get(new ByteArrayWrapper(current));
                outputBytes.add(outByte);
                dictionary.put(new ByteArrayWrapper(next), dictionary.size());
                current = new byte[1];
                current[0] = b;
            }
        }
        if (current.length > 0) {
            Integer outByte = dictionary.get(new ByteArrayWrapper(current));
            outputBytes.add(outByte);
        }

        // Write compressed data to output file.
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(compressedFile))) {
            outputStream.writeObject(outputBytes);
        }
    }

    @Override
    public void decompress(String filePath) throws IOException {
        File inputFile = new File(filePath);
        // file extension should be changed if the original file is not txt file
        File decompressedFile = new File(getDecompressedPath(filePath));

        // Initialize dictionary with all possible byte values.
        Map<Integer, ByteArrayWrapper> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            byte[] b = new byte[1];
            b[0] = (byte) i;
            dictionary.put(i, new ByteArrayWrapper(b));
        }

        // Read input file into a byte array.
        List<Integer> inputBytes;
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(inputFile))) {
            inputBytes = (ArrayList<Integer>) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Decompress the input data.
        ByteArrayOutputStream decompressedData = new ByteArrayOutputStream();
        int currentCode = inputBytes.get(0);
        byte[] currentByte = dictionary.get(currentCode).bytes();
        decompressedData.write(currentByte);
        for (int i = 1; i < inputBytes.size(); i++) {
            int nextCode = inputBytes.get(i);
            byte[] nextByte;
            if (dictionary.containsKey(nextCode)) {
                nextByte = dictionary.get(nextCode).bytes();
            } else {
                nextByte = new byte[currentByte.length + 1];
                System.arraycopy(currentByte, 0, nextByte, 0, currentByte.length);
                nextByte[currentByte.length] = currentByte[0];
            }
            decompressedData.write(nextByte);
            byte[] addedByte = new byte[currentByte.length + 1];
            System.arraycopy(currentByte, 0, addedByte, 0, currentByte.length);
            addedByte[currentByte.length] = nextByte[0];
            dictionary.put(dictionary.size(), new ByteArrayWrapper(addedByte));
            currentByte = nextByte;
        }


        // Write decompressed data to output file.
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(decompressedFile))) {
            outputStream.write(decompressedData.toByteArray());
        }
    }

    public static String changeExtension(String filePath, String newExtension, boolean addDecompressed) {
        int i = filePath.lastIndexOf('.');
        String newFilePath = filePath.substring(0, i);
        if(addDecompressed)
            newFilePath = newFilePath + "_decompressed";
        return newFilePath + newExtension;
    }

    public String getCompressedPath(String path) {
        return path + ".lzw";
    }

    public String getDecompressedPath(String path) {
        String originalFilePath = path.substring(0, path.length() - 4);
        return changeExtension(originalFilePath, originalFilePath.substring(originalFilePath.lastIndexOf('.')), true);
    }

    private record ByteArrayWrapper(byte[] bytes) {

        @Override
            public int hashCode() {
                return Arrays.hashCode(bytes);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj instanceof ByteArrayWrapper other) {
                    return Arrays.equals(bytes, other.bytes);
                }
                return false;
            }
        }
}