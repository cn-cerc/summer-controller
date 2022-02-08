package cn.cerc.mis.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Description;

import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.core.DataValidate;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.ServiceMethod;

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
            DataValidate dataValidate = item.method().getDeclaredAnnotation(DataValidate.class);
            String funcCode = item.method().getName();
            if (dataValidate != null) {
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < dataValidate.value().length; i++)
                    sb.append("\"").append(dataValidate.value()[i]).append("\",");
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

}
