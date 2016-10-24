package me.mrkirby153.kcuhc;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDFetcherTest {

    @Test
    public void fromBytes() throws Exception {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = UUIDFetcher.toBytes(uuid);
        Assert.assertEquals(uuid, UUIDFetcher.fromBytes(bytes));
    }

    @Test
    public void getUUIDOf() throws Exception {
        Assert.assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), UUIDFetcher.getUUIDOf("Notch"));
    }

    @Test
    public void toBytes() throws Exception {
        UUID uuid = UUID.randomUUID();
        byte[] bytes = UUIDFetcher.toBytes(uuid);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        Assert.assertEquals(uuid.getMostSignificantBits(), byteBuffer.getLong());
        Assert.assertEquals(uuid.getLeastSignificantBits(), byteBuffer.getLong());
    }

}