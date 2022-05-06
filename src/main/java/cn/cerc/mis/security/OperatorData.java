package cn.cerc.mis.security;

import java.util.ArrayList;
import java.util.List;

public class OperatorData {
    private static final String GUEST_DEFAULT = "guest.default";
    private static final String ALL_USER_PASS = "all.user.pass";

    private String permission;
    private String version;
    private List<String> items = new ArrayList<>();
    private boolean children = false;

    public OperatorData(String permission) {
        this(permission, false);
    }

    public OperatorData(Enum<?> permission) {
        this(permission.name().replaceAll("_", "."));
    }

    public OperatorData(String permission, boolean children) {
        super();
        if (ALL_USER_PASS.equals(permission))
            this.permission = Permission.GUEST;
        else
            this.permission = permission;
        this.children = children;
    }

    public OperatorData(String permission, String operator) {
        super();
        if (ALL_USER_PASS.equals(permission) || GUEST_DEFAULT.equals(permission)) {
            this.permission = Permission.GUEST;
            return;
        }

        this.permission = permission;
        this.children = true;
        switch (operator) {
        case "Execute":
            break;
        case "Append":
            this.add(Operators.INSERT);
            break;
        case "Modify":
            this.add(Operators.UPDATE);
            break;
        case "Delete":
            this.add(Operators.DELETE);
            break;
        case "Final":
            this.add(Operators.FINISH);
            break;
        case "Cancel":
            this.add(Operators.CANCEL);
            break;
        case "Recycle":
            this.add(Operators.NULLIFY);
            break;
        case "Print":
            this.add(Operators.REPORT);
            break;
        case "Output":
            this.add(Operators.EXPORT);
            break;
        default:
            this.add(operator);
        }
    }

    public String getPermission() {
        return permission;
    }

    private final OperatorData writeValue(boolean setExists, String value) {
        if (setExists)
            items.add(value);
        else
            items.remove(value);
        return this;
    }

    public OperatorData insert(boolean value) {
        return writeValue(value, Operators.INSERT);
    }

    public OperatorData update(boolean value) {
        return writeValue(value, Operators.UPDATE);
    }

    public OperatorData delete(boolean value) {
        return writeValue(value, Operators.DELETE);
    }

    public OperatorData finish(boolean value) {
        return writeValue(value, Operators.FINISH);
    }

    public OperatorData cancel(boolean value) {
        return writeValue(value, Operators.CANCEL);
    }

    public OperatorData nullify(boolean value) {
        return writeValue(value, Operators.NULLIFY);
    }

    public OperatorData report(boolean value) {
        return writeValue(value, Operators.REPORT);
    }

    public OperatorData export(boolean value) {
        return writeValue(value, Operators.EXPORT);
    }

    public OperatorData design(boolean value) {
        return writeValue(value, Operators.DESIGN);
    }

    public OperatorData add(String item) {
        if (!this.children)
            throw new RuntimeException("children is disabled");
        if (this.items.indexOf(item) == -1)
            this.items.add(item);
        return this;
    }

    public OperatorData addAll() {
        return this.add("*");
    }

    public String getVersion() {
        return version;
    }

    public OperatorData setVersion(String version) {
        this.version = version;
        return this;
    }

    public boolean isChildren() {
        return children;
    }

    public OperatorData setChildren(boolean children) {
        if (this.children != children) {
            if (items.size() > 0 && !children)
                throw new RuntimeException("items size > 0");
            this.children = children;
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.permission);
        if (this.children)
            sb.append("[");
        if (this.items.size() > 0) {
            for (int i = 0; i < items.size(); i++) {
                sb.append(items.get(i));
                if (i < (items.size() - 1))
                    sb.append(",");
            }
        }
        if (this.children)
            sb.append("]");
        if (this.version != null)
            sb.append("#").append(this.version);
        return sb.toString();
    }

    public static void main(String[] args) {
        OperatorData data = new OperatorData("abc");
        data.setChildren(true);
        data.add("a").add("b").report(true);
        data.setVersion("3").addAll();
        System.out.println(data);
    }

}
