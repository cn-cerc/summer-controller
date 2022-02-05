package cn.cerc.mis.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.cerc.db.core.ClassData;
import cn.cerc.db.core.DataSet;
import cn.cerc.mis.core.IService;
import cn.cerc.mis.core.IStatus;

public final class ServiceSign {
    private String id;
    private IServiceServer server;

    public ServiceSign(String id) {
        super();
        this.id = id;
    }

    public ServiceSign(String id, IServiceServer server) {
        super();
        this.id = id;
        this.server = server;
    }

    public String id() {
        return id;
    }

    public IServiceServer server() {
        return this.server;
    }

    public static void buildSourceCode(Class<?> clazz) {
        if (!IService.class.isAssignableFrom(clazz)) {
            System.out.println(String.format("// %s skip: it's not service", clazz.getSimpleName()));
            return;
        }
        final String fmt = "public static final ServiceSign %s = new ServiceSign(\"%s.%s\");";
        System.out.println(String.format("public static class %s {", clazz.getSimpleName()));
        List<String> items = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getModifiers() != ClassData.PUBLIC)
                continue;
            if (method.getReturnType() == boolean.class && method.getParameterCount() == 0)
                items.add(method.getName());
        }
        if (items.size() > 0) {
            items.sort((t1, t2) -> t1.toLowerCase().compareTo(t2.toLowerCase()));
            System.out.println("// version 1");
            for (String item : items)
                System.out.println(String.format(fmt, item, clazz.getSimpleName(), item));
        }

        items.clear();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getModifiers() != ClassData.PUBLIC)
                continue;
            if (method.getReturnType() == IStatus.class && method.getParameterCount() == 2)
                items.add(method.getName());
        }
        if (items.size() > 0) {
            items.sort((t1, t2) -> t1.toLowerCase().compareTo(t2.toLowerCase()));
            System.out.println("// version 2");
            for (String item : items)
                System.out.println(String.format(fmt, item, clazz.getSimpleName(), item));
        }

        items.clear();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getModifiers() != ClassData.PUBLIC)
                continue;
            if (method.getReturnType() == DataSet.class && method.getParameterCount() == 2)
                items.add(method.getName());
        }
        if (items.size() > 0) {
            items.sort((t1, t2) -> t1.toLowerCase().compareTo(t2.toLowerCase()));
            System.out.println("// version 3");
            for (String item : items)
                System.out.println(String.format(fmt, item, clazz.getSimpleName(), item));
        }
        System.out.println("}");
    }

}
