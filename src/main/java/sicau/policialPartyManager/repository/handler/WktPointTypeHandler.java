package sicau.policialPartyManager.repository.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL {@code POINT} 列与 WKT 字符串（{@code POINT(lng lat)}）之间的转换器。
 * <p>
 * MySQL 8 的 GEOMETRY 系列列不接受普通字符串直接写入（报 1416），
 * 必须写内部二进制格式：4 字节 SRID（小端）+ 1 字节字节序标志 + 4 字节几何类型
 * + 坐标 double 序列。本处理器固定 SRID=0（不引入经纬度轴序约束；若将来要做
 * 地理围栏/空间索引再升级到 4326，并把 WKT 轴序调整为 lat-first）。
 * <p>
 * 空串与 {@code null} 都写 NULL；读库把二进制还原为 WKT 字符串。
 */
@MappedTypes(String.class)
public class WktPointTypeHandler extends BaseTypeHandler<String> {

    private static final Pattern POINT_WKT =
            Pattern.compile("^POINT\\s*\\(\\s*(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)\\s*\\)$",
                    Pattern.CASE_INSENSITIVE);

    /** MySQL 内部几何类型码：Point = 1 */
    private static final int GeometryTypePoint = 1;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        String wkt = parameter.trim();
        if (wkt.isEmpty()) {
            ps.setNull(i, java.sql.Types.BINARY);
            return;
        }
        Matcher m = POINT_WKT.matcher(wkt);
        if (!m.matches()) {
            throw new IllegalArgumentException("地点格式不正确，应为 POINT(经度 纬度)，收到：" + wkt);
        }
        double x = Double.parseDouble(m.group(1));
        double y = Double.parseDouble(m.group(2));
        ps.setBytes(i, toInternalBytes(x, y));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return fromInternalBytes(rs.getBytes(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return fromInternalBytes(rs.getBytes(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return fromInternalBytes(cs.getBytes(columnIndex));
    }

    /** WKT(x y) → MySQL 内部二进制（SRID=0，小端） */
    static byte[] toInternalBytes(double x, double y) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 4 + 16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0);                       // SRID = 0
        buffer.put((byte) 1);                   // 字节序标志：1 = 小端
        buffer.putInt(GeometryTypePoint);       // geometry type: Point
        buffer.putDouble(x);
        buffer.putDouble(y);
        return buffer.array();
    }

    /** MySQL 内部二进制 → WKT 字符串；NULL / 空 / 非 Point → null */
    static String fromInternalBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 25) {
            return null;
        }
        ByteOrder order = bytes[4] == 1 ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(order);
        buffer.position(4);          // skip SRID
        buffer.get();                // byte order flag（已据此选定端序）
        int type = buffer.getInt();
        if (type != GeometryTypePoint) {
            // 本列是 POINT（无 Z/M 维），其他类型不识别，按空处理
            return null;
        }
        double x = buffer.getDouble();
        double y = buffer.getDouble();
        return "POINT(" + trimNumber(x) + " " + trimNumber(y) + ")";
    }

    private static String trimNumber(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }
}
