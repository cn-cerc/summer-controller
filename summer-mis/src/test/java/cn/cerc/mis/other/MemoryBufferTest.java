package cn.cerc.mis.other;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@Slf4j
public class MemoryBufferTest {

    @Test
    @Ignore
    public void test_connect() {
        try (MemoryBuffer buff = new MemoryBuffer(BufferType.test, "test");) {
            if (buff.Connected()) {
                if (buff.isNull()) {
                    buff.setField("Code_", "1000");
                    buff.setField("Name_", "Jason");
                    buff.setField("num", 0);
                    log.info("Init memcached.");
                } else {
                    log.info("read memcached.");
                }
                buff.setField("num", buff.getInt("num") + 1);
            } else {
                assertEquals("联系不上 Memcahced 服务器！", "ok", "erro");
            }
        }
    }

    @Test
    @Ignore
    public void test_read_write() {
        String data = "AAA";
        try (MemoryBuffer buff = new MemoryBuffer(BufferType.test, "test");) {
            buff.setField("A", data);
        }
        try (MemoryBuffer buff = new MemoryBuffer(BufferType.test, "test");) {
            assertEquals(data, buff.getString("A"));
        }
        MemoryBuffer.delete(BufferType.test, "test");
        try (MemoryBuffer buff = new MemoryBuffer(BufferType.test, "test");) {
            assertEquals(null, buff.getRecord().getField("A"));
        }
    }
}
