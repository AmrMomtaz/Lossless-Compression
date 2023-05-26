import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Lempel–Ziv–Welch compression algorithm.
 */
public class LZW implements CompressionAlgorithm {

    @Override
    public void compress(String filePath) {
        File compressedFile = new File(changeExtension(filePath, ".lzw", false));

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
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void decompress(String filePath) {
        File inputFile = new File(filePath);
        // file extension should be changed if the original file is not txt file
        File decompressedFile = new File(changeExtension(filePath, ".txt", true));

        // Initialize dictionary with all possible byte values.
        Map<Integer, ByteArrayWrapper> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            byte[] b = new byte[1];
            b[0] = (byte) i;
            dictionary.put(i, new ByteArrayWrapper(b));
        }

        // Read input file into a byte array.
        List<Integer> inputBytes = new ArrayList<>();
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(inputFile))) {
            inputBytes = (ArrayList<Integer>) inputStream.readObject();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        // Decompress the input data.
        ByteArrayOutputStream decompressedData = new ByteArrayOutputStream();
        int currentCode = inputBytes.get(0);
        byte[] currentByte = dictionary.get(currentCode).getBytes();
        try {
            decompressedData.write(currentByte);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 1; i < inputBytes.size(); i++) {
            int nextCode = inputBytes.get(i);
            byte[] nextByte;
            if (dictionary.containsKey(nextCode)) {
                nextByte = dictionary.get(nextCode).getBytes();
            } else {
                nextByte = new byte[currentByte.length + 1];
                System.arraycopy(currentByte, 0, nextByte, 0, currentByte.length);
                nextByte[currentByte.length] = currentByte[0];
            }
            try {
                decompressedData.write(nextByte);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            byte[] addedByte = new byte[currentByte.length + 1];
            System.arraycopy(currentByte, 0, addedByte, 0, currentByte.length);
            addedByte[currentByte.length] = nextByte[0];
            dictionary.put(dictionary.size(), new ByteArrayWrapper(addedByte));
            currentByte = nextByte;
        }


        // Write decompressed data to output file.
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(decompressedFile))) {
            outputStream.write(decompressedData.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        return changeExtension(path, ".lzw", false);
    }

    public String getDecompressedPath(String path) {
        return changeExtension(path, ".txt", true);
    }
}
