import java.util.Arrays;
import java.util.List;

public class ByteArrayWrapper {
    private final byte[] bytes;

    public ByteArrayWrapper(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public ByteArrayWrapper(List<Byte> bytes) {
        this.bytes = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            this.bytes[i] = bytes.get(i);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ByteArrayWrapper) {
            ByteArrayWrapper other = (ByteArrayWrapper) obj;
            return Arrays.equals(bytes, other.bytes);
        }
        return false;
    }
}
