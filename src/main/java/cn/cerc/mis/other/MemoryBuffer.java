package cn.cerc.mis.other;

import cn.cerc.db.core.ClassResource;
import cn.cerc.db.core.Utils;
import cn.cerc.db.redis.RedisRecord;
import cn.cerc.mis.SummerMIS;
import cn.cerc.mis.core.IBufferKey;
import cn.cerc.mis.core.SystemBuffer;

public class MemoryBuffer extends RedisRecord implements AutoCloseable {

    private static final ClassResource res = new ClassResource(MemoryBuffer.class, SummerMIS.ID);

    public MemoryBuffer(Enum<? extends IBufferKey> bufferType, String... keys) {
        super();
        this.setKey(buildKey(bufferType, keys));
    }

    public static void delete(Enum<? extends IBufferKey> bufferType, String... keys) {
        RedisRecord buffer = new RedisRecord(buildKey(bufferType, keys));
        buffer.clear();
    }

    public static String buildKey(Enum<? extends IBufferKey> bufferType, String... keys) {
        if (!(bufferType instanceof IBufferKey)) {
            throw new RuntimeException(res.getString(1, "错误的初始化参数！"));
        }

        IBufferKey bufferKey = (IBufferKey) bufferType;
        if (keys.length < bufferKey.getMinimumNumber()) {
            throw new RuntimeException(res.getString(3, "参数数量不足！"));
        }

        if (keys.length > bufferKey.getMaximumNumber()) {
            throw new RuntimeException(res.getString(4, "参数数量过多！"));
        }

        StringBuilder result = new StringBuilder();
        result.append(prefix(bufferType));
        for (String key : keys) {
            if (key == null)
                throw new RuntimeException(res.getString(2, "传值有误！"));
            result.append(".").append(key);
        }
        return result.toString();
    }

    /**
     * 获取缓存类型的前缀
     */
    public static int prefix(Enum<? extends IBufferKey> bufferType) {
        IBufferKey bufferKey = (IBufferKey) bufferType;
        int prefix = bufferKey.getStartingPoint() + bufferType.ordinal();
        return prefix;
    }

    public static String buildObjectKey(Class<?> class1) {
        return String.format("%d.%s", SystemBuffer.UserObject.ClassName.ordinal(), class1.getName());
    }

    public static String buildObjectKey(Class<?> class1, int version) {
        return String.format("%d.%s.%d", SystemBuffer.UserObject.ClassName.ordinal(), class1.getName(), version);
    }

    public static String buildObjectKey(Class<?> class1, String field) {
        if (Utils.isEmpty(field))
            throw new RuntimeException("field is empty");
        return String.format("%d.%s.%s", SystemBuffer.UserObject.ClassName.ordinal(), class1.getName(), field);
    }

    public static String buildObjectKey(Class<?> class1, String field, int version) {
        if (Utils.isEmpty(field))
            throw new RuntimeException("key field is empty");
        return String.format("%d.%s.%s.%d", SystemBuffer.UserObject.ClassName.ordinal(), class1.getName(), field,
                version);
    }

    @Override
    public final void close() {
        this.post();
    }

    public static void main(String[] args) {
        System.out.println(buildObjectKey(MemoryBuffer.class));
        System.out.println(buildObjectKey(MemoryBuffer.class, 1));
        System.out.println(buildObjectKey(MemoryBuffer.class, "admin"));
        System.out.println(buildObjectKey(MemoryBuffer.class, "admin", 1));
    }
}
