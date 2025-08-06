package me.unknkriod.quickmark.serializers;

import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkType;
import me.unknkriod.quickmark.utils.Base85Encoder;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MarkSerializerTest {

    private Mark createTestMark() {
        return new Mark(
                MarkType.NORMAL,
                new BlockPos(10, 64, -5),
                UUID.randomUUID(),
                System.currentTimeMillis()
        );
    }

    // ---------- MARK ----------

    @Test
    void testSerializeDeserializeMark() {
        Mark mark = createTestMark();
        String encoded = MarkSerializer.serializeMark(mark);
        Mark decoded = MarkSerializer.deserializeMark(encoded);

        assertNotNull(decoded);
        assertEquals(mark.getType(), decoded.getType());
        assertEquals(mark.getPosition(), decoded.getPosition());
        assertEquals(mark.getPlayerId(), decoded.getPlayerId());
        assertEquals(mark.getCreationTime(), decoded.getCreationTime());
    }

    @Test
    void testInvalidMarkReturnsNull() {
        // Испорченные данные — неверный префикс
        byte[] badData = Base85Encoder.uuidToBytes(UUID.randomUUID());
        badData[0] = 'A'; // Префикс не 'M'
        String encoded = Base85Encoder.encode(badData);

        assertNull(MarkSerializer.deserializeMark(encoded));

        // Строка некорректной длины (не /5)
        assertThrows(IllegalArgumentException.class,
                () -> Base85Encoder.decode("abcd"));
    }

    // ---------- REMOVE ----------

    @Test
    void testSerializeDeserializeRemove() {
        UUID id = UUID.randomUUID();
        String encoded = MarkSerializer.serializeRemoveCommand(id);
        UUID decoded = MarkSerializer.deserializeRemoveCommand(encoded);

        assertNotNull(decoded);
        assertEquals(id, decoded);
    }

    @Test
    void testInvalidRemoveReturnsNull() {
        // Испорченный префикс
        byte[] badData = Base85Encoder.uuidToBytes(UUID.randomUUID());
        badData[0] = 'A'; // Префикс не 'X'
        String encoded = Base85Encoder.encode(badData);

        assertNull(MarkSerializer.deserializeRemoveCommand(encoded));
    }
}
