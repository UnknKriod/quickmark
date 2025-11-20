package me.unknkriod.quickmark.serializers;

import me.unknkriod.quickmark.Quickmark;
import me.unknkriod.quickmark.mark.Mark;
import me.unknkriod.quickmark.mark.MarkType;
import me.unknkriod.quickmark.utils.Base85Encoder;
import net.minecraft.util.math.BlockPos;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Serializes and deserializes marks and remove commands using Base85 encoding for network transmission.
 */
public class MarkSerializer {
    // [M][UUID id][byte type][int x][int y][int z][UUID sender][long timestamp]
    // [M] — type byte.
    //
    // UUID id — 16 bytes.
    //
    // byte type — 0 = NORMAL, 1 = DANGER.
    //
    // x,y,z — 4 bytes each (int32).
    //
    // UUID sender — 16 bytes.
    //
    // timestamp — 8 bytes (long).

    // Remove:
    // [X][UUID id]

    /**
     * Serializes a mark into a Base85-encoded string.
     */
    public static String serializeMark(Mark mark) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        out.write('M'); // Message type: Mark
        out.writeBytes(Base85Encoder.uuidToBytes(mark.getId())); // Mark ID
        out.write((byte) (mark.getType() == MarkType.DANGER ? 1 : 0)); // Type: 0=Normal, 1=Danger

        // Position coordinates
        ByteBuffer coords = ByteBuffer.allocate(12);
        coords.putInt(mark.getPosition().getX());
        coords.putInt(mark.getPosition().getY());
        coords.putInt(mark.getPosition().getZ());
        out.writeBytes(coords.array());

        out.writeBytes(Base85Encoder.uuidToBytes(mark.getPlayerId())); // Sender UUID

        // Creation timestamp
        ByteBuffer ts = ByteBuffer.allocate(8);
        ts.putLong(mark.getCreationTime());
        out.writeBytes(ts.array());

        return Base85Encoder.encode(out.toByteArray());
    }

    /**
     * Deserializes a Base85-encoded string into a Mark object.
     */
    public static Mark deserializeMark(String encoded) {
        try {
            byte[] data = Base85Encoder.decode(encoded);
            if (data[0] != 'M') return null;

            int pos = 1;

            UUID id = Base85Encoder.bytesToUuid(data, pos);
            pos += 16;

            MarkType type = (data[pos++] == 1) ? MarkType.DANGER : MarkType.NORMAL;

            int x = ByteBuffer.wrap(data, pos, 4).getInt(); pos += 4;
            int y = ByteBuffer.wrap(data, pos, 4).getInt(); pos += 4;
            int z = ByteBuffer.wrap(data, pos, 4).getInt(); pos += 4;

            UUID sender = Base85Encoder.bytesToUuid(data, pos);
            pos += 16;

            long timestamp = ByteBuffer.wrap(data, pos, 8).getLong();

            return new Mark(type, new BlockPos(x, y, z), id, sender, timestamp);

        } catch (Exception e) {
            Quickmark.LOGGER.error("Failed to deserialize mark: " + e.getMessage());
            return null;
        }
    }

    /**
     * Serializes a remove command for a mark.
     */
    public static String serializeRemoveCommand(UUID markId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write('X'); // Message type: Remove
        out.writeBytes(Base85Encoder.uuidToBytes(markId));
        return Base85Encoder.encode(out.toByteArray());
    }

    /**
     * Deserializes a remove command into the mark ID.
     */
    public static UUID deserializeRemoveCommand(String encoded) {
        try {
            byte[] data = Base85Encoder.decode(encoded);
            if (data[0] != 'X') return null;
            return Base85Encoder.bytesToUuid(data, 1);
        } catch (Exception e) {
            return null;
        }
    }
}