package cn.cerc.mis.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Description;

import cn.cerc.db.core.ClassData;
import cn.cerc.db.core.DataSet;
import cn.cerc.db.core.IHandle;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.IStatus;

public final class ServiceSign {
    private String id;
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
            System.out.println(String.format("@Description(\"%s\")", description.value()));
        System.out.println(String.format("public static class %s {", clazz.getSimpleName()));
        List<Method> items = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getModifiers() != ClassData.PUBLIC)
                continue;
            if (method.getReturnType() == boolean.class && method.getParameterCount() == 0)
                items.add(method);
        }
        printList(clazz, items, 1);

        items.clear();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getModifiers() != ClassData.PUBLIC)
                continue;
            if (method.getReturnType() == IStatus.class && method.getParameterCount() == 2)
                items.add(method);
        }
        printList(clazz, items, 2);

        items.clear();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getModifiers() != ClassData.PUBLIC)
                continue;
            if (method.getReturnType() == DataSet.class && method.getParameterCount() == 2)
                items.add(method);
        }
        printList(clazz, items, 3);
        System.out.println("}");
    }

    private static void printList(Class<?> clazz, List<Method> items, int version) {
        if (items.size() == 0)
            return;
        items.sort((t1, t2) -> t1.getName().toLowerCase().compareTo(t2.getName().toLowerCase()));
        System.out.println("// version " + version);
        final String fmt = "public static final ServiceSign %s = new ServiceSign(\"%s.%s\");";
        for (Method item : items) {
            Description description = item.getDeclaredAnnotation(Description.class);
            if (description != null)
                System.out.println(String.format("@Description(\"%s\")", description.value()));
            System.out.println(String.format(fmt, item.getName(), clazz.getSimpleName(), item.getName()));
        }
    }

}
