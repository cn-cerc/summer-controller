package cn.cerc.mis.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Description;

import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.EntityImpl;
import cn.cerc.db.core.EntityKey;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.ado.EntityQuery;
import cn.cerc.mis.core.DataValidate;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceMethod;
import cn.cerc.mis.core.ServiceState;

public final class ServiceSign {
    private String id;
    private int version;
    private Set<String> properties;
    private ServiceServerImpl server;

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

    public DataSet call(IHandle handle, DataSet dataIn) {
        try {
            if (server == null)
                return LocalServer.call(this, handle, dataIn);
            else
                return server.call(this, handle, dataIn);
        } catch (Throwable e) {
            e.printStackTrace();
            return new DataSet().setMessage(e.getMessage());
        }
    }

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
        items.sort((t1, t2) -> t1.method().getName().toLowerCase().compareTo(t2.method().getName().toLowerCase()));
        for (ServiceMethod item : items) {
            description = item.method().getDeclaredAnnotation(Description.class);
            if (description != null)
                System.out.println(String.format("/** %s */", description.value()));
            DataValidate[] dataValidates = item.method().getDeclaredAnnotationsByType(DataValidate.class);
            String funcCode = item.method().getName();
            if (dataValidates.length > 0) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < dataValidates.length; i++)
                    sb.append("\"").append(dataValidates[i].value()).append("\",");
                sb.delete(sb.length() - 1, sb.length());
                if (item.version().ordinal() > 0)
                    System.out.println(String.format(
                            "public static final ServiceSign %s = new ServiceSign(\"%s.%s\").setVersion(%d).setProperties(Set.of(%s));",
                            funcCode, clazz.getSimpleName(), funcCode, item.version().ordinal(), sb.toString()));
                else
                    System.out.println(String.format(
                            "public static final ServiceSign %s = new ServiceSign(\"%s.%s\").setProperties(Set.of(%s));",
                            funcCode, clazz.getSimpleName(), funcCode, sb.toString()));
            } else {
                if (item.version().ordinal() > 0)
                    System.out.println(String.format(
                            "public static final ServiceSign %s = new ServiceSign(\"%s.%s\").setVersion(%d);", funcCode,
                            clazz.getSimpleName(), funcCode, item.version().ordinal()));
                else
                    System.out.println(String.format("public static final ServiceSign %s = new ServiceSign(\"%s.%s\");",
                            funcCode, clazz.getSimpleName(), funcCode));
            }
        }

        System.out.println("}");
    }

    /**
     * 服务返回结果转换为指定的业务对象
     * 
     * @param <T>    业务对象实体类
     * @param handle 句柄
     * @param clazz  业务对象实体类class
     * @param values 对应实体类的缓存key
     * @return 指定的实体对象
     */
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
            DataSet dataOut = this.call(handle, dataIn);
            if (dataOut.state() == ServiceState.OK)
                return Optional.of(dataOut.current().asEntity(clazz));
            return Optional.empty();
        }
    }

}
