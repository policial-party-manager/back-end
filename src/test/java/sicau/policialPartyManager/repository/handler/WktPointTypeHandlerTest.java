package sicau.policialPartyManager.repository.handler;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WKT ↔ MySQL 内部 POINT 二进制的往返测试。
 * <p>
 * 期望字节序列在真实 MySQL 8.0.33 上验证过：
 * ST_AsBinary(POINT(103.96 30.57)) 为 21 字节小端 WKB，
 * 加 4 字节 SRID=0 前缀后即可直接写入 POINT 列并原样读回。
 */
class WktPointTypeHandlerTest {

    private static final byte[] EXPECTED_103_96_30_57 = {
            0x00, 0x00, 0x00, 0x00,                       // SRID = 0
            0x01,                                          // little-endian
            0x01, 0x00, 0x00, 0x00,                        // geometry type = Point
            (byte) 0x3D, (byte) 0x0A, (byte) 0xD7, (byte) 0xA3,
            (byte) 0x70, (byte) 0xFD, (byte) 0x59, (byte) 0x40,  // x = 103.96
            (byte) 0x52, (byte) 0xB8, (byte) 0x1E, (byte) 0x85,
            (byte) 0xEB, (byte) 0x91, (byte) 0x3E, (byte) 0x40   // y = 30.57
    };

    @Test
    void encodesWktToInternalBytes() {
        byte[] bytes = WktPointTypeHandler.toInternalBytes(103.96, 30.57);
        assertEquals(25, bytes.length);
        assertEquals(0, Arrays.compare(EXPECTED_103_96_30_57, bytes),
                () -> "got " + Arrays.toString(bytes));
    }

    @Test
    void decodesInternalBytesBackToWkt() {
        String wkt = WktPointTypeHandler.fromInternalBytes(EXPECTED_103_96_30_57.clone());
        assertEquals("POINT(103.96 30.57)", wkt);
    }

    @Test
    void roundtripKeepsWholeNumbersClean() {
        byte[] bytes = WktPointTypeHandler.toInternalBytes(104, 31);
        assertEquals("POINT(104 31)", WktPointTypeHandler.fromInternalBytes(bytes));
    }

    @Test
    void decodesNullAndShortInput() {
        assertNull(WktPointTypeHandler.fromInternalBytes(null));
        assertNull(WktPointTypeHandler.fromInternalBytes(new byte[10]));
    }

    @Test
    void setParameterRejectsGarbageWkt() {
        WktPointTypeHandler handler = new WktPointTypeHandler();
        Exception e = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () ->
                        handler.setNonNullParameter(null, 1, "NOT A POINT", null));
        // PreparedStatement 为 null 也应在解析 WKT 时就失败（不触达 SQL）
        assertTrue(e.getMessage().contains("地点格式不正确"), e.getMessage());
    }
}
