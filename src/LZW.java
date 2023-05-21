import java.io.*;
import java.nio.file.Files;
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
        File inputFile = new File(filePath);
        File compressedFile = new File(changeExtension(filePath, ".lzw", false));

        // Initialize dictionary with all possible byte values.
        Map<String, Integer> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put("" + (char) i, i);
        }

        // Read input file into a byte array.
        byte[] inputBytes = new byte[0];
        try {
            inputBytes = Files.readAllBytes(inputFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Compress the input data.
        List<Integer> outputBytes = new ArrayList<>();
        ByteArrayOutputStream compressedData = new ByteArrayOutputStream();
        String current = "";
        for (byte b : inputBytes) {
            String next = current + (char) b;
            if (dictionary.containsKey(next)) {
                current = next;
            } else {
                Integer outByte = dictionary.get(current);
                compressedData.write(outByte.byteValue());
                outputBytes.add(outByte);
                dictionary.put(next, dictionary.size());
                current = "" + (char) b;
            }
        }
        if (!current.equals("")) {
            Integer outByte = dictionary.get(current);
            compressedData.write(outByte.byteValue());
            outputBytes.add(outByte);
        }

        // Write compressed data to output file.
        try (DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(compressedFile))) {
            for (Integer i: outputBytes) {
                outputStream.writeInt(i);
            }
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
        Map<Integer, String> dictionary = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            dictionary.put(i, "" + (char) i);
        }

        // Read input file into a byte array.
        List<Integer> inputBytes = new ArrayList<>();
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(inputFile));
            while (inputStream.available() != 0) {
                inputBytes.add(inputStream.readInt());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Decompress the input data.
        ByteArrayOutputStream decompressedData = new ByteArrayOutputStream();
        int currentCode = inputBytes.get(0);
        String currentString = dictionary.get(currentCode);
        try {
            decompressedData.write(currentString.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = 1; i < inputBytes.size(); i++) {
            int nextCode = inputBytes.get(i);
            String nextString;
            if (dictionary.containsKey(nextCode)) {
                nextString = dictionary.get(nextCode);
            } else if (nextCode == dictionary.size()) {
                nextString = currentString + currentString.charAt(0);
            } else {
                throw new IllegalArgumentException("Bad compressed data: " +nextCode);
            }
            try {
                decompressedData.write(nextString.getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dictionary.put(dictionary.size(), currentString + nextString.charAt(0));
            currentString = nextString;
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

}
