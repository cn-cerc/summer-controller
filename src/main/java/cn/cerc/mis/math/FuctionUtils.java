package cn.cerc.mis.math;

public class FuctionUtils {

    public static String fixSpace(String text) {
        if (text.length() == 0)
            return "";
        var value = text.trim();
        if (value.length() == 0)
            return " ";

        boolean find = false;
        StringBuffer sb = new StringBuffer();
        for (var i = 0; i < value.length(); i++) {
            char tmp = value.charAt(i);
            if (tmp == ' ') {
                if (find)
                    continue;
                find = true;
                sb.append(tmp);
            } else if (tmp == '\n') {
                if (find)
                    sb.deleteCharAt(sb.length() - 1);
                sb.append(tmp);
                find = true;
            } else {
                find = false;
                sb.append(tmp);
            }
        }
        var tmp = text.charAt(text.length() - 1);
        if (tmp == '\n' || tmp == ' ')
            sb.append(tmp);
        return sb.toString();
    }

}
