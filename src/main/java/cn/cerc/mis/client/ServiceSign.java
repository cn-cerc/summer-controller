package cn.cerc.mis.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Description;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.ado.EntityQuery;
import cn.cerc.mis.core.DataValidate;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.LocalService;
import cn.cerc.mis.core.ServiceMethod;
import cn.cerc.mis.core.ServiceState;

public final class ServiceSign extends ServiceProxy implements ServiceSignImpl, InvocationHandler {
    private final String id;
    private int version;
    private Set<String> properties;
    private ServiceServerImpl server;

    private Class<?> headStructure;
    private Class<?> bodyStructure;

    public ServiceSign(String id) {
        super();
        this.id = id;
    }

    public ServiceSign(String id, ServiceServerImpl server) {
        super();
        this.id = id;
        this.server = server;
    }

    public String id() {
        return id;
    }

    public ServiceServerImpl server() {
        return this.server;
    }

    public int version() {
        return version;
    }

    public ServiceSign setVersion(int version) {
        this.version = version;
        return this;
    }

    public Set<String> properties() {
        return properties;
    }

    public ServiceSign setProperties(Set<String> properties) {
        this.properties = properties;
        return this;
    }

    @Override
    public ServiceSign sign() {
        return this;
    }

    @Override
    public ServiceSign call(IHandle handle) {
        return call(handle, new DataSet());
    }

    @Override
    public ServiceSign call(IHandle handle, DataRow headIn) {
        DataSet dataIn = new DataSet();
        dataIn.head().copyValues(headIn);
        return call(handle, dataIn);
    }

    @Override
    public ServiceSign call(IHandle handle, DataSet dataIn) {
        this.setSession(handle.getSession());
        DataSet dataOut = null;
        try {
            if (server == null)
                dataOut = LocalService.call(this.id, handle, dataIn);
            else
                dataOut = server.call(this, handle, dataIn);
        } catch (Throwable e) {
            e.printStackTrace();
            dataOut = new DataSet().setMessage(e.getMessage());
        }
        this.setDataOut(dataOut);
        return this;
    }

    @Override
    public Object head() {
        if (this.headStructure == null)
            throw new RuntimeException("not define interface: headStructure");
        return dataOut().head().asRecord(headStructure);
    }

    @Override
    public List<Object> body() {
        if (this.bodyStructure == null)
            throw new RuntimeException("not define interface: bodyStructure");
        List<Object> result = new ArrayList<>();
        dataOut().forEach(item -> result.add(item.asRecord(bodyStructure)));
        return result;
    }

    public String getExportKey() {
        return ServiceExport.build(this, this.dataIn());
    }

    /**
     * 生成指定服务类的签名定义
     */
    public static void buildSourceCode(Class<?> clazz) {
        if (!IService.class.isAssignableFrom(clazz)) {
            System.out.println(String.format("// %s skip: it's not service", clazz.getSimpleName()));
            return;
        }
        Description description = clazz.getDeclaredAnnotation(Description.class);
        if (description != null)
            System.out.println(String.format("/** %s */", description.value()));
        System.out.println(String.format("public static class %s {", clazz.getSimpleName()));

        List<ServiceMethod> items = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            ServiceMethod sm = ServiceMethod.build(clazz, method.getName());
            if (sm != null)
                items.add(sm);
        }
        items.sort(Comparator.comparing(t -> t.method().getName().toLowerCase()));
        for (ServiceMethod svc : items) {
            description = svc.method().getDeclaredAnnotation(Description.class);
            if (description != null)
                System.out.println(String.format("/** %s */", description.value()));

            // 检查是否有重复校验的字段
            String function = svc.method().getName();
            DataValidate[] dataValidates = svc.method().getDeclaredAnnotationsByType(DataValidate.class);
            List<String> duplicates = Arrays.stream(dataValidates)
                    .collect(Collectors.groupingBy(e -> e.value(), Collectors.counting())).entrySet().stream()
                    .filter(e -> e.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toList());
            if (duplicates.size() > 0)
                throw new RuntimeException(String.format("服务对象 %s 重复定义元素 %s", function, String.join(", ", duplicates)));

            if (dataValidates.length > 0) {
                StringBuilder builder = new StringBuilder();
                for (DataValidate dataValidate : dataValidates)
                    builder.append("\"").append(dataValidate.value()).append("\",");
                builder.delete(builder.length() - 1, builder.length());

                if (svc.version().ordinal() > 0)
                    System.out.println(String.format(
                            "public static final ServiceSign %s = new ServiceSign(\"%s.%s\").setVersion(%d).setProperties(Set.of(%s));",
                            function, clazz.getSimpleName(), function, svc.version().ordinal(), builder.toString()));
                else
                    System.out.println(String.format(
                            "public static final ServiceSign %s = new ServiceSign(\"%s.%s\").setProperties(Set.of(%s));",
                            function, clazz.getSimpleName(), function, builder.toString()));
            } else {
                if (svc.version().ordinal() > 0)
                    System.out.println(String.format(
                            "public static final ServiceSign %s = new ServiceSign(\"%s.%s\").setVersion(%d);", function,
                            clazz.getSimpleName(), function, svc.version().ordinal()));
                else
                    System.out.println(String.format("public static final ServiceSign %s = new ServiceSign(\"%s.%s\");",
                            function, clazz.getSimpleName(), function));
            }
        }
        System.out.println("}");
    }

    /**
     * 业务对象建议使用 asRecord
     * 
     * 服务返回结果转换为指定的业务对象
     *
     * @param <T>    业务对象实体类
     * @param handle 句柄
     * @param clazz  业务对象实体类class
     * @param values 对应实体类的缓存key
     * @return 指定的实体对象
     */
    @Deprecated
    public <T extends EntityImpl> Optional<T> findOne(IHandle handle, Class<T> clazz, String... values) {
        if (this.server() == null || this.server().isLocal(handle, this))
            return EntityQuery.findOne(handle, clazz, values);
        else {
            EntityKey entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
            DataSet dataIn = new DataSet();
            DataRow headIn = dataIn.head();
            int site = entityKey.corpNo() ? 1 : 0;
            String[] fields = entityKey.fields();
            for (int i = site; i < fields.length; i++)
                headIn.setValue(fields[i], values[i - site]);
            DataSet dataOut = this.call(handle, dataIn).dataOut();
            if (dataOut.state() == ServiceState.OK)
                return Optional.of(dataOut.current().asEntity(clazz));
            return Optional.empty();
        }
    }

    /**
     * 业务对象建议使用 asRecord
     */
    @Deprecated
    public <T extends EntityImpl> Set<T> findMany(IHandle handle, Class<T> clazz, String... values) {
        if (this.server() == null || this.server().isLocal(handle, this))
            return EntityQuery.findMany(handle, clazz, values);
        else {
            Set<T> set = new LinkedHashSet<>();
            EntityKey entityKey = clazz.getDeclaredAnnotation(EntityKey.class);
            DataSet dataIn = new DataSet();
            DataRow headIn = dataIn.head();
            int site = entityKey.corpNo() ? 1 : 0;
            String[] fields = entityKey.fields();
            if (values != null && values.length > 0) {
                for (int i = site; i < fields.length; i++)
                    headIn.setValue(fields[i], values[i - site]);
            }
            DataSet dataOut = this.call(handle, dataIn).dataOut();
            if (dataOut.state() != ServiceState.OK)
                return set;

            dataOut.records().stream().map(item -> item.asEntity(clazz)).forEach(set::add);
            return set;
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().equals("sign"))
            return this.sign();
        else if (method.getName().equals("call"))
            return method.invoke(this, args);
        else if (method.getName().equals("head"))
            return this.head();
        else if (method.getName().equals("body"))
            return this.body();
        else
            throw new RuntimeException("not support method: " + method.getName());
    }

    public static ServiceSignImpl build(String id) {
        return build(id, null, ServiceSignImpl.class);
    }

    public static ServiceSignImpl build(String id, ServiceServerImpl server) {
        return build(id, server, ServiceSignImpl.class);
    }

    public static <T> T build(String id, Class<T> clazz) {
        return build(id, null, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> T build(String id, ServiceServerImpl server, Class<T> clazz) {
        ServiceSign sign = new ServiceSign(id, server);
        try {
            Method head = clazz.getMethod("head");
            if (head != null && head.getReturnType() != Object.class)
                sign.headStructure = head.getReturnType();
        } catch (NoSuchMethodException | SecurityException e) {
        }
        try {
            Method body = clazz.getMethod("body");
            if (body != null) {
                if (body.getReturnType() != List.class)
                    throw new RuntimeException("only support List<Body>");
                Type genericReturnType = body.getGenericReturnType();
                ParameterizedType pt = (ParameterizedType) genericReturnType;
                if (!"?".equals(pt.getActualTypeArguments()[0].getTypeName()))
                    sign.bodyStructure = (Class<?>) pt.getActualTypeArguments()[0];
            }
        } catch (NoSuchMethodException | SecurityException e) {
        }
        return (T) Proxy.newProxyInstance(ServiceSign.class.getClassLoader(), new Class[] { clazz }, sign);
    }

}
