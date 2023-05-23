import java.io.*;

/**
 * Implementation of the arithmetic coding algorithm.
 */
public class ArithmeticCoding implements CompressionAlgorithm {

    protected final long halfRange;
    protected final long quarterRange;
    protected final long stateMask;
    protected long low;
    protected long high;

    public ArithmeticCoding() {
        halfRange = (1L << 32) >>> 1;
        quarterRange = halfRange >>> 1;
        stateMask = (1L << 32) - 1;
        low = 0;
        high = stateMask;
    }

    //
    // Public Methods
    //

    @Override
    public void compress(String filePath) throws IOException {
        ProbabilitiesTable frequencies = getFrequencies(filePath);
        frequencies.increment(256);
        try (InputStream in = new BufferedInputStream(new FileInputStream(filePath));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(filePath + ".AE"))) {
            compress(frequencies, in, out);
        }
    }

    @Override
    public void decompress(String filePath) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(filePath));
             OutputStream out = new BufferedOutputStream
                 (new FileOutputStream("decompressed_" + filePath.substring(0, filePath.length()-3)))) {
            ProbabilitiesTable frequencies = readFrequencies(in);
            decompress(frequencies, in, out);
        }
    }

    //
    // Private methods
    //

    /**
     * Creates and returns a new probabilities' table after accessing a given file path.
     */
    private static ProbabilitiesTable getFrequencies(String filePath) throws IOException {
        ProbabilitiesTable frequencies = new ProbabilitiesTable(new int[257]);
        try (InputStream input = new BufferedInputStream(new FileInputStream(filePath))) {
            while (true) {
                int b = input.read();
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
        (ProbabilitiesTable frequencies, InputStream in, OutputStream out) throws IOException {

        // Write the frequencies
        for (int i = 0; i < 256; i++)
            for (int j = 32 - 1; j >= 0; j--)
                out.write((frequencies.get(i) >>> j) & 1);

        ArithmeticEncoder arithmeticEncoder = new ArithmeticEncoder(out);
        while (true) {
            int symbol = in.read();
            if (symbol == -1) break;
            arithmeticEncoder.update(new ProbabilitiesTable(frequencies), symbol);
        }
        arithmeticEncoder.update(new ProbabilitiesTable(frequencies), 256);
        arithmeticEncoder.output.write(1);
    }

    /**
     * Reads a frequency table from a given input stream and returns it.
     */
    private static ProbabilitiesTable readFrequencies(InputStream in) throws IOException {
        int[] frequencies = new int[257];
        for (int i = 0; i < 256; i++) frequencies[i] = nextInt(in);
        frequencies[256] = 1;
        return new ProbabilitiesTable(frequencies);
    }

    /**
     * Decompress helper function. Creates an instance of ArithmeticDecoder
     * and decompresses a given InputStream to produce the decompressed file.
     */
    private static void decompress
            (ProbabilitiesTable frequencies, InputStream in, OutputStream out) throws IOException {

        ArithmeticDecoder arithmeticDecoder = new ArithmeticDecoder(in);
        while (true) {
            int symbol = arithmeticDecoder.nextSymbol(frequencies);
            if (symbol == 256) break;
            out.write(symbol);
        }
    }

    /**
     * Reads and returns an integer from InputStream.
     */
    private static int nextInt(InputStream in) throws IOException {
        int result = 0;
        for (int i = 0; i < 32; i++) result = (result << 1) | in.read();
        return result;
    }

    //
    // Classes
    //

    /**
     * Implementation of the arithmetic encoder.
     */
    private static final class ArithmeticEncoder extends ArithmeticCoding {

        OutputStream output;
        private int numUnderflow = 0;

        public ArithmeticEncoder(OutputStream out) { output = out; }

        /**
         * Updates the encoder state.
         */
        public void update(ProbabilitiesTable frequencies, int symbol) throws IOException {
            long temp = low + frequencies.getHigh(symbol) * (high - low + 1) / frequencies.getTotal() - 1;
            low += frequencies.getLow(symbol)  * (high - low + 1) / frequencies.getTotal();
            high = temp;
            while (((low ^ high) & halfRange) == 0) {
                int bit = (int) (low >>> (31));
                output.write(bit);
                for (; numUnderflow > 0; numUnderflow--) output.write(bit ^ 1);
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

        private final InputStream input;
        private long code;

        public ArithmeticDecoder(InputStream in) throws IOException {
            input = in; code = 0;
            for (int i = 0; i < 32; i++) code = code << 1 | Math.max(0, input.read());
        }

        /**
         * Updates the decoder state.
         */
        public void update(ProbabilitiesTable frequencies, int symbol) throws IOException {
            long temp = (low + frequencies.getHigh(symbol) * (high - low + 1) / frequencies.getTotal() - 1);
            low += frequencies.getLow(symbol)  * (high - low + 1) / frequencies.getTotal();
            high = temp;
            while (((low ^ high) & halfRange) == 0) {
                code = ((code << 1) & stateMask) | Math.max(0, input.read());
                low  = ((low  << 1) & stateMask);
                high = ((high << 1) & stateMask) | 1;
            }
            while ((low & ~high & quarterRange) != 0) {
                code = (code & halfRange) | ((code << 1) & (stateMask >>> 1)) | Math.max(0, input.read());
                low = (low << 1) ^ halfRange;
                high = ((high ^ halfRange) << 1) | halfRange | 1;
            }
        }

        /**
         * Gets the next symbol.
         */
        public int nextSymbol(ProbabilitiesTable frequencies) throws IOException {
            long value = (((code - low) + 1) * frequencies.getTotal() - 1) / (high - low + 1);
            int start = 0, end = frequencies.getSymbolLimit();
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
    private static final class ProbabilitiesTable {

        public final int[] occurrences;
        private int[] cumulative = null;
        private int total = 0;

        public ProbabilitiesTable(int[] frequencies) {
            occurrences = frequencies.clone();
            for (int x : occurrences) total = Math.addExact(x, total);
        }

        public ProbabilitiesTable(ProbabilitiesTable frequencies) {
            int numSym = frequencies.getSymbolLimit();
            occurrences = new int[numSym];
            total = 0;
            for (int i = 0; i < occurrences.length; i++) {
                int x = frequencies.get(i);
                occurrences[i] = x;
                total = Math.addExact(x, total);
            }
            cumulative = null;
        }

        public int getSymbolLimit() { return occurrences.length; }

        public int get(int symbol) { return occurrences[symbol]; }

        public void increment(int symbol) {
            total = Math.addExact(total, 1);
            occurrences[symbol]++;
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
            cumulative = new int[occurrences.length + 1];
            int sum = 0;
            for (int i = 0; i < occurrences.length; i++) {
                sum = Math.addExact(occurrences[i], sum);
                cumulative[i + 1] = sum;
            }
        }
    }
}
