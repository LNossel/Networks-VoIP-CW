package io.github.lnossel;
import java.nio.ByteBuffer;

public class Security {

    private final int key;

    public Security(int key) {
        this.key = key;
    }

    byte[] applyXOR(byte[] block) {
        ByteBuffer unwrap;
        ByteBuffer cipherText;
        int fourByte;
        unwrap = ByteBuffer.allocate(block.length);

        cipherText = ByteBuffer.wrap(block);
        for (int j = 0; j < block.length/4; j++) {
            fourByte = cipherText.getInt();
            fourByte = fourByte ^ key;
            unwrap.putInt(fourByte);
        }
        block = unwrap.array();
        return block;
    }
}
