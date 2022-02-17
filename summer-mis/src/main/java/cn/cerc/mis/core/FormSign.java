package cn.cerc.mis.core;

public class FormSign {
    private String id;
    private String value;

    public FormSign(String text) {
        String items[] = text.split("\\.");
        switch (items.length) {
        case 1: {
            this.id = text;
            this.value = "execute";
            break;
        }
        case 2: {
            this.id = items[0];
            this.value = items[1];
            break;
        }
        default:
            throw new RuntimeException("error value: " + text);
        }
    }

    public final String getId() {
        return id;
    }

    public final void setId(String id) {
        this.id = id;
    }

    public final String getValue() {
        return value;
    }

    public final void setValue(String value) {
        this.value = value;
    }

}
