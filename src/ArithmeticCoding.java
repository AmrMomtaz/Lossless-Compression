import java.io.*;

public class ArithmeticCoding {

    public void compress(String filePath) throws IOException {
        File inputFile  = new File(filePath);
        FrequencyTable freqs = getFrequencies(inputFile);
        freqs.increment(256);
        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        OutputStream out = new BufferedOutputStream(new FileOutputStream(filePath + ".AE"));
        writeFrequencies(out, freqs);
        compress(freqs, in, out);
        in.close();
        out.close();
    }

    public void decompress(String filePath) throws IOException {
        File inputFile  = new File(filePath);
        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        OutputStream out = new BufferedOutputStream
                (new FileOutputStream("decompressed_" + filePath.substring(0, filePath.length()-3)));
        FrequencyTable freqs = readFrequencies(in);
        decompress(freqs, in, out);
        in.close();
        out.close();
    }

    private static FrequencyTable getFrequencies(File file) throws IOException {
        FrequencyTable freqs = new FrequencyTable(new int[257]);
        InputStream input = new BufferedInputStream(new FileInputStream(file));
        while (true) {
            int b = input.read();
            if (b == -1) break;
            freqs.increment(b);
        }
        input.close();
        return freqs;
    }

    static void writeFrequencies(OutputStream out, FrequencyTable freqs) throws IOException {
        for (int i = 0; i < 256; i++) {
            int value = freqs.get(i);
            for (int j = 32 - 1; j >= 0; j--) out.write((value >>> j) & 1);
        }
    }

    static void compress(FrequencyTable freqs, InputStream in, OutputStream out) throws IOException {
        ArithmeticEncoder enc = new ArithmeticEncoder(32, out);
        while (true) {
            int symbol = in.read();
            if (symbol == -1) break;
            enc.update(new FrequencyTable(freqs), symbol);
        }
        enc.update(new FrequencyTable(freqs), 256);
        enc.output.write(1);
    }

    private static final class ArithmeticEncoder {

        private final int numStateBits;
        private final long halfRange;
        private final long quarterRange;
        private final long stateMask;
        private long low;
        private long high;
        OutputStream output;
        private int numUnderflow;

        public ArithmeticEncoder(int numBits, OutputStream out) {
            numStateBits = numBits;
            long fullRange = 1L << numStateBits;
            halfRange = fullRange >>> 1;
            quarterRange = halfRange >>> 1;
            stateMask = fullRange - 1;
            low = 0;
            high = stateMask;
            output = out;
            numUnderflow = 0;
        }

        public void update(FrequencyTable freqs, int symbol) throws IOException {
            long range = high - low + 1;
            long total = freqs.getTotal();
            long symLow = freqs.getLow(symbol);
            long symHigh = freqs.getHigh(symbol);
            long newLow = low + symLow * range / total;
            long newHigh = low + symHigh * range / total - 1;
            low = newLow;
            high = newHigh;
            while (((low ^ high) & halfRange) == 0) {
                int bit = (int) (low >>> (numStateBits - 1));
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

    private static final class ArithmeticDecoder {
        private final long halfRange;
        private final long quarterRange;
        private final long stateMask;
        private long low;
        private long high;
        private final InputStream input;
        private long code;

        public ArithmeticDecoder(int numBits, InputStream in) throws IOException {
            long fullRange = 1L << numBits;
            halfRange = fullRange >>> 1;
            quarterRange = halfRange >>> 1;
            stateMask = fullRange - 1;
            low = 0;
            high = stateMask;
            input = in;
            code = 0;
            for (int i = 0; i < numBits; i++) code = code << 1 | Math.max(0, input.read());
        }

        public void update(FrequencyTable freqs, int symbol) throws IOException {
            long range = high - low + 1;
            long total = freqs.getTotal();
            long symLow = freqs.getLow(symbol);
            long symHigh = freqs.getHigh(symbol);
            long newLow  = low + symLow  * range / total;
            long newHigh = low + symHigh * range / total - 1;
            low = newLow;
            high = newHigh;
            while (((low ^ high) & halfRange) == 0) {
                code = ((code << 1) & stateMask) | Math.max(0, input.read());;
                low  = ((low  << 1) & stateMask);
                high = ((high << 1) & stateMask) | 1;
            }
            while ((low & ~high & quarterRange) != 0) {
                code = (code & halfRange) | ((code << 1) & (stateMask >>> 1)) | Math.max(0, input.read());;
                low = (low << 1) ^ halfRange;
                high = ((high ^ halfRange) << 1) | halfRange | 1;
            }
        }

        public int read(FrequencyTable freqs) throws IOException {
            long total = freqs.getTotal();
            long range = high - low + 1;
            long offset = code - low;
            long value = ((offset + 1) * total - 1) / range;
            int start = 0;
            int end = freqs.getSymbolLimit();
            while (end - start > 1) {
                int middle = (start + end) >>> 1;
                if (freqs.getLow(middle) > value)
                    end = middle;
                else
                    start = middle;
            }
            int symbol = start;
            update(freqs, symbol);
            return symbol;
        }
    }

    static FrequencyTable readFrequencies(InputStream in) throws IOException {
        int[] freqs = new int[257];
        for (int i = 0; i < 256; i++) freqs[i] = readInt(in);
        freqs[256] = 1;
        return new FrequencyTable(freqs);
    }

    static void decompress(FrequencyTable freqs, InputStream in, OutputStream out) throws IOException {
        ArithmeticDecoder dec = new ArithmeticDecoder(32, in);
        while (true) {
            int symbol = dec.read(freqs);
            if (symbol == 256) break;
            out.write(symbol);
        }
    }

    private static int readInt(InputStream in) throws IOException {
        int result = 0;
        for (int i = 0; i < 32; i++) result = (result << 1) | in.read();
        return result;
    }

    private static final class FrequencyTable {

        public final int[] frequencies;
        private int[] cumulative;
        private int total;

        public FrequencyTable(int[] freqs) {
            frequencies = freqs.clone();
            total = 0;
            for (int x : frequencies) total = Math.addExact(x, total);
            cumulative = null;
        }

        public FrequencyTable(FrequencyTable freqs) {
            int numSym = freqs.getSymbolLimit();
            frequencies = new int[numSym];
            total = 0;
            for (int i = 0; i < frequencies.length; i++) {
                int x = freqs.get(i);
                frequencies[i] = x;
                total = Math.addExact(x, total);
            }
            cumulative = null;
        }

        public int getSymbolLimit() {
            return frequencies.length;
        }

        public int get(int symbol) {
            return frequencies[symbol];
        }

        public void increment(int symbol) {
            total = Math.addExact(total, 1);
            frequencies[symbol]++;
            cumulative = null;
        }

        public int getTotal() {
            return total;
        }

        public int getLow(int symbol) {
            if (cumulative == null)	initCumulative();
            return cumulative[symbol];
        }

        public int getHigh(int symbol) {
            if (cumulative == null)	initCumulative();
            return cumulative[symbol + 1];
        }

        private void initCumulative() {
            cumulative = new int[frequencies.length + 1];
            int sum = 0;
            for (int i = 0; i < frequencies.length; i++) {
                sum = Math.addExact(frequencies[i], sum);
                cumulative[i + 1] = sum;
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < frequencies.length; i++)
                sb.append(String.format("%d\t%d%n", i, frequencies[i]));
            return sb.toString();
        }
    }
}
