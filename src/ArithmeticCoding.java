import java.io.*;

/**
 * Implementation of the arithmetic coding algorithm.
 */
public class ArithmeticCoding implements CompressionAlgorithm {

    // Constants
    private static final long halfRange = (1L << 32) >>> 1;
    private static final long quarterRange = ((1L << 32) >>> 1) >>> 1;
    private static final long stateMask = (1L << 32) - 1;

    //
    // Public Methods
    //

    @Override
    public void compress(String filePath) throws IOException {
        FrequencyTable frequencies = getFrequencies(filePath);
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
             BitOutputStream bitOutputStream = new BitOutputStream(new BufferedOutputStream
                 (new FileOutputStream(filePath + ".AE")))) {

            ArithmeticEncoder arithmeticEncoder = new ArithmeticEncoder(bitOutputStream);
            writeFrequencies(frequencies, bitOutputStream);
            while (true) {
                int symbol = inputStream.read();
                if (symbol == -1) break;
                arithmeticEncoder.updateState(frequencies, symbol);
            }
            arithmeticEncoder.updateState(frequencies, 256);
            bitOutputStream.writeBit(1);
        }
    }

    @Override
    public void decompress(String filePath) throws IOException {
        try (BitInputStream bitInputStream = new BitInputStream(new BufferedInputStream
                (new FileInputStream(filePath)), new File(filePath).length());
             OutputStream outputStream = new BufferedOutputStream
                (new FileOutputStream("decompressed_" + filePath.substring(0, filePath.length()-3)))) {

            FrequencyTable frequencies = readFrequencies(bitInputStream);
            ArithmeticDecoder arithmeticDecoder = new ArithmeticDecoder(bitInputStream);
            while (true) {
                int symbol = arithmeticDecoder.nextSymbol(frequencies);
                if (symbol == 256) break;
                outputStream.write(symbol);
            }
        }
    }
    public String getCompressedPath(String filePath) {
        return filePath + ".AE";
    }
    public String getDecompressedPath(String filePath) {
        return "decompressed_" + filePath.substring(0, filePath.length()-3);
    }

    //
    // Private methods
    //

    /**
     * Creates and returns a new frequency table after processing a given file path.
     */
    private static FrequencyTable getFrequencies(String filePath) throws IOException {
        FrequencyTable frequencies = new FrequencyTable(new int[257]);
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath))) {
            while (true) {
                int b = inputStream.read();
                if (b == -1) break;
                frequencies.increment(b);
            }
        }
        frequencies.increment(256);
        frequencies.initializeCumulative();
        return frequencies;
    }

    /**
     * Reads a frequency table from a given input stream and returns it.
     */
    private static FrequencyTable readFrequencies
        (BitInputStream bitInputStream) throws IOException {

        int[] frequencies = new int[257];
        frequencies[256] = 1;
        for (int i = 0; i < 256; i++) {
            int frequency = 0;
            for (int j = 0 ; j < 32 ; j++) {
                frequency <<= 1;
                frequency |= bitInputStream.readBit();
            }
            frequencies[i] = frequency;
        }
        return new FrequencyTable(frequencies);
    }

    /**
     * Writes the frequency table.
     */
    private static void writeFrequencies
        (FrequencyTable frequencies, BitOutputStream out) throws IOException {
        for (int i = 0; i < 256; i++) {
            int num = frequencies.getFrequency(i);
            out.writeByte((num >> 24) & 0xFF);
            out.writeByte((num >> 16) & 0xFF);
            out.writeByte((num >> 8) & 0xFF);
            out.writeByte(num & 0xFF);
        }
    }

    //
    // Classes
    //

    /**
     * Implementation of the arithmetic encoder.
     */
    private static final class ArithmeticEncoder {

        private long low = 0;
        private long high = stateMask;
        private int numUnderflow = 0;
        private final BitOutputStream bitOutputStream;

        public ArithmeticEncoder(BitOutputStream bitOutputStream) { this.bitOutputStream = bitOutputStream; }

        /**
         * Updates the encoder state.
         */
        public void updateState(FrequencyTable frequencies, int symbol) throws IOException {
            long temp = low + frequencies.getHigh(symbol) * (high - low + 1) / frequencies.getTotal() - 1;
            low += frequencies.getLow(symbol)  * (high - low + 1) / frequencies.getTotal();
            high = temp;
            while (((low ^ high) & halfRange) == 0) {
                int bit = (int) (low >>> (31));
                bitOutputStream.writeBit(bit);
                for ( ; numUnderflow > 0 ; numUnderflow--) bitOutputStream.writeBit(bit ^ 1);
                low = ((low << 1) & stateMask);
                high = ((high << 1) & stateMask) | 1;
            }
            while ((low & ~high & quarterRange) != 0) {
                numUnderflow++;
                low = (low << 1) ^ halfRange;
                high = ((high ^ halfRange) << 1) | halfRange | 1;
            }
        }
    }

    /**
     * Implementation of the arithmetic decoder.
     */
    private static final class ArithmeticDecoder {

        private long code = 0;
        private long low = 0;
        private long high = stateMask;
        private final BitInputStream bitInputStream;

        public ArithmeticDecoder(BitInputStream bitInputStream) throws IOException {
            this.bitInputStream = bitInputStream;
            for (int i = 0; i < 32; i++) code = code << 1 | Math.max(0, this.bitInputStream.readBit());
        }

        /**
         * Updates the decoder state.
         */
        public void updateState(FrequencyTable frequencies, int symbol) throws IOException {
            long temp = (low + frequencies.getHigh(symbol) * (high - low + 1) / frequencies.getTotal() - 1);
            low += frequencies.getLow(symbol)  * (high - low + 1) / frequencies.getTotal();
            high = temp;
            while (((low ^ high) & halfRange) == 0) {
                int bit = Math.max(0, bitInputStream.readBit());
                code = ((code << 1) & stateMask) | bit;
                low  = ((low  << 1) & stateMask);
                high = ((high << 1) & stateMask) | 1;
            }
            while ((low & ~high & quarterRange) != 0) {
                int bit = Math.max(0, bitInputStream.readBit());
                code = (code & halfRange) | ((code << 1) & (stateMask >>> 1)) | bit;
                low = (low << 1) ^ halfRange;
                high = ((high ^ halfRange) << 1) | halfRange | 1;
            }
        }

        /**
         * Gets the next symbol of the compressed text.
         */
        public int nextSymbol(FrequencyTable frequencies) throws IOException {
            long value = (((code - low) + 1) * frequencies.getTotal() - 1) / (high - low + 1);
            int start = 0, end = 257;
            while (end - start > 1) {
                int middle = (start + end) >>> 1;
                if (frequencies.getLow(middle) > value) end = middle;
                else start = middle;
            }
            updateState(frequencies, start);
            return start;
        }
    }

    /**
     * Represents the probabilities' table which holds the cumulative probability
     * and the occurrences of each character.
     */
    private static final class FrequencyTable {

        public final int[] frequencies;
        private int[] cumulative = null;
        private int total = 0;

        public FrequencyTable(int[] frequencies) {
            this.frequencies = frequencies;
            for (int x : this.frequencies) total += x;
            initializeCumulative();
        }

        public int getFrequency(int symbol) { return frequencies[symbol]; }

        public void increment(int symbol) {
            total++;
            frequencies[symbol]++;
        }

        public int getTotal() { return total; }

        public int getLow(int symbol) { return cumulative[symbol]; }

        public int getHigh(int symbol) { return cumulative[symbol+1]; }

        public void initializeCumulative() {
            int sum = 0;
            cumulative = new int[frequencies.length + 1];
            for (int i = 0 ; i < frequencies.length ; i++) {
                sum += frequencies[i];
                cumulative[i + 1] = sum;
            }
        }
    }

    /**
     * Used to write bits using output stream.
     */
    private static final class BitOutputStream implements AutoCloseable {
        private final OutputStream outputStream;
        private int byteToWrite;
        private int bitsLeft;

        public BitOutputStream(OutputStream outputStream) {
            this.outputStream = outputStream;
            this.byteToWrite = 0;
            this.bitsLeft = 8;
        }

        public void writeBit(int bit) throws IOException {
            byteToWrite <<= 1;
            byteToWrite |= bit;
            bitsLeft--;
            if (bitsLeft == 0) {
                outputStream.write(byteToWrite);
                byteToWrite = 0;
                bitsLeft = 8;
            }
        }

        public void writeByte(int b) throws IOException { this.outputStream.write(b); }

        @Override
        public void close() throws IOException {
            outputStream.write((8 - bitsLeft));
            outputStream.write(byteToWrite);
            outputStream.close();
        }
    }

    /**
     * Used to read bits given InputStream.
     */
    private static final class BitInputStream implements AutoCloseable {
        private final InputStream inputStream;
        private final long fileSize;
        private long byteIndex;
        private int currentByte;
        private int bitsLeft;

        public BitInputStream(InputStream inputStream, long fileSize) {
            this.inputStream = inputStream;
            this.fileSize = fileSize;
            this.byteIndex = 0L;
            this.currentByte = 0;
            this.bitsLeft = 0;
        }

        public int readBit() throws IOException {
            if (bitsLeft == 0) {
                byteIndex++;
                currentByte = inputStream.read();
                if (currentByte == -1) return -1;
                if (byteIndex == fileSize-1) {
                    bitsLeft = currentByte;
                    currentByte = inputStream.read();
                    byteIndex++;
                }
                else bitsLeft = 8;
            }
            int temp = currentByte;
            temp >>= (bitsLeft-1);
            bitsLeft--;
            return temp & 1;
        }

        @Override
        public void close() throws IOException { this.inputStream.close(); }
    }
}
