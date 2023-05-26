import java.io.*;

/**
 * Implementation of the arithmetic coding algorithm.
 */
public class ArithmeticCoding implements CompressionAlgorithm {

    // Constants
    protected static final long halfRange = (1L << 32) >>> 1;
    protected static final long quarterRange = ((1L << 32) >>> 1) >>> 1;
    protected static final long stateMask = (1L << 32) - 1;

    // State variables
    protected long low = 0;
    protected long high = (1L << 32) - 1;

    public ArithmeticCoding() { }

    //
    // Public Methods
    //

    @Override
    public void compress(String filePath) throws IOException {
        FrequencyTable frequencies = getFrequencies(filePath);
        frequencies.increment(256);
        try (InputStream in = new BufferedInputStream(new FileInputStream(filePath));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(filePath + ".AE"))) {
            writeFrequencies(frequencies, out);
            compress(frequencies, in, out);
        }
    }

    @Override
    public void decompress(String filePath) throws IOException {
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath));
             OutputStream outputStream = new BufferedOutputStream
                 (new FileOutputStream("decompressed_" + filePath.substring(0, filePath.length()-3)))) {
            FrequencyTable frequencies = readFrequencies(inputStream);
            decompress(frequencies, inputStream, outputStream);
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
        return frequencies;
    }

    /**
     * Compression helper function. Creates an instance of ArithmeticEncoder
     * and compresses a given InputStream to produce the compressed file.
     */
    private static void compress
        (FrequencyTable frequencies, InputStream inputStream, OutputStream outputStream) throws IOException {

        ArithmeticEncoder arithmeticEncoder = new ArithmeticEncoder(outputStream);
        while (true) {
            int symbol = inputStream.read();
            if (symbol == -1) break;
            arithmeticEncoder.update(frequencies, symbol);
        }
        arithmeticEncoder.update(frequencies, 256);
        outputStream.write(1);
    }

    /**
     * Writes the frequency table.
     */
    private static void writeFrequencies
        (FrequencyTable frequencies, OutputStream out) throws IOException {
        for (int i = 0; i < 256; i++) {
            int num = frequencies.getFrequency(i);
            out.write((num >> 24) & 0xFF);
            out.write((num >> 16) & 0xFF);
            out.write((num >> 8) & 0xFF);
            out.write(num & 0xFF);
        }
    }

    /**
     * Reads a frequency table from a given input stream and returns it.
     */
    private static FrequencyTable readFrequencies(InputStream in) throws IOException {
        int[] frequencies = new int[257]; frequencies[256] = 1;
        for (int i = 0; i < 256; i++)
            frequencies[i] = in.read() << 24 | in.read() << 16 |
                in.read() << 8 | in.read();
        return new FrequencyTable(frequencies);
    }

    /**
     * Decompress helper function. Creates an instance of ArithmeticDecoder
     * and decompresses a given InputStream to produce the decompressed file.
     */
    private static void decompress
            (FrequencyTable frequencies, InputStream inputStream, OutputStream outputStream) throws IOException {

        ArithmeticDecoder arithmeticDecoder = new ArithmeticDecoder(inputStream);
        while (true) {
            int symbol = arithmeticDecoder.nextSymbol(frequencies);
            if (symbol == 256) break;
            outputStream.write(symbol);
        }
    }

    //
    // Classes
    //

    /**
     * Implementation of the arithmetic encoder.
     */
    private static final class ArithmeticEncoder extends ArithmeticCoding {

        private final OutputStream output;
        private int numUnderflow = 0;

        public ArithmeticEncoder(OutputStream out) { output = out; }

        /**
         * Updates the encoder state.
         */
        public void update(FrequencyTable frequencies, int symbol) throws IOException {
            long temp = low + frequencies.getHigh(symbol) * (high - low + 1) / frequencies.getTotal() - 1;
            low += frequencies.getLow(symbol)  * (high - low + 1) / frequencies.getTotal();
            high = temp;
            while (((low ^ high) & halfRange) == 0) {
                int bit = (int) (low >>> (31));
                output.write(bit);
                for ( ; numUnderflow > 0 ; numUnderflow--) output.write(bit ^ 1);
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
    private static final class ArithmeticDecoder extends ArithmeticCoding {

        private final InputStream inputStream;
        private long code;

        public ArithmeticDecoder(InputStream inputStream) throws IOException {
            this.inputStream = inputStream; code = 0;
            for (int i = 0; i < 32; i++) code = code << 1 | Math.max(0, this.inputStream.read());
        }

        /**
         * Updates the decoder state.
         */
        public void update(FrequencyTable frequencies, int symbol) throws IOException {
            long temp = (low + frequencies.getHigh(symbol) * (high - low + 1) / frequencies.getTotal() - 1);
            low += frequencies.getLow(symbol)  * (high - low + 1) / frequencies.getTotal();
            high = temp;
            while (((low ^ high) & halfRange) == 0) {
                code = ((code << 1) & stateMask) | Math.max(0, inputStream.read());
                low  = ((low  << 1) & stateMask);
                high = ((high << 1) & stateMask) | 1;
            }
            while ((low & ~high & quarterRange) != 0) {
                code = (code & halfRange) | ((code << 1) & (stateMask >>> 1)) | Math.max(0, inputStream.read());
                low = (low << 1) ^ halfRange;
                high = ((high ^ halfRange) << 1) | halfRange | 1;
            }
        }

        /**
         * Gets the next symbol.
         */
        public int nextSymbol(FrequencyTable frequencies) throws IOException {
            long value = (((code - low) + 1) * frequencies.getTotal() - 1) / (high - low + 1);
            int start = 0, end = 257;
            while (end - start > 1) {
                int middle = (start + end) >>> 1;
                if (frequencies.getLow(middle) > value) end = middle;
                else start = middle;
            }
            update(frequencies, start);
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
        }

        public int getFrequency(int symbol) { return frequencies[symbol]; }

        public void increment(int symbol) {
            total++;
            frequencies[symbol]++;
            cumulative = null;
        }

        public int getTotal() { return total; }

        public int getLow(int symbol) {
            if (cumulative == null)	updateCumulative();
            return cumulative[symbol];
        }

        public int getHigh(int symbol) {
            if (cumulative == null)	updateCumulative();
            return cumulative[symbol + 1];
        }

        private void updateCumulative() {
            int sum = 0;
            cumulative = new int[frequencies.length + 1];
            for (int i = 0; i < frequencies.length; i++) {
                sum += frequencies[i];
                cumulative[i + 1] = sum;
            }
        }
    }
}
