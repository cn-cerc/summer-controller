package cn.cerc.mis.log;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import cn.cerc.db.core.ClassConfig;
import cn.cerc.db.core.Curl;
import cn.cerc.db.core.DataRow;
import cn.cerc.db.core.Utils;
import cn.cerc.db.queue.AbstractQueue;
import cn.cerc.db.queue.QueueServiceEnum;

@Component
public class QueueJayunLog extends AbstractQueue {
    private static final ClassConfig config = new ClassConfig();
    public static final String prefix = "qc";

    public QueueJayunLog() {
        super();
        this.setService(QueueServiceEnum.RabbitMQ);
        this.setPushMode(true);
    }

    public String push(JayunLogBuilder logData) {
        try {
            return super.push(new Gson().toJson(logData));
        } catch (Exception e) {
            return "error";
        }
    }

    @Override
    public boolean consume(String message, boolean repushOnError) {
        String site = config.getString("cn.knowall.site", "");
        if (Utils.isEmpty(site))
            return true;

        JayunLogBuilder data = new Gson().fromJson(message, JayunLogBuilder.class);
        String profile = "cn.knowall.token";
        String token = config.getString(profile, "");
        if (Utils.isEmpty(token)) {
            System.err.println(String.format("%s 项目日志配置 %s 为空", data.getProject(), profile));
            return true;
        }

        executor.submit(() -> {
            try {
                Curl curl = new Curl();
                curl.put("origin", String.format("%s:%s", data.getId(), data.getLine()));
                curl.put("message", data.getMessage());
                curl.put("level", data.getLevel());
                curl.put("group", data.getException());
                curl.put("machine", data.getHostname());
                curl.put("timestamp", data.getTimestamp());
                StringBuilder builder = new StringBuilder();
                String[] lines = data.getStack();
                if (!Utils.isEmpty(lines)) {
                    for (String line : lines) {
                        if (line.contains("\t"))
                            line = line.replaceAll("\t", System.lineSeparator() + "\t");
                        builder.append(line);
                    }
                }
                String stack = builder.toString();
                curl.put("data0", stack);
                Object args = data.getArgs();
                if (args != null) {
                    if (args instanceof Collection<?> collection) {
                        int i = 1;
                        for (Object val : collection) {
                            curl.put(String.format("data%s", i++), val);
                        }
                    } else if (args instanceof Object[] arr) {
                        for (int i = 0; i < arr.length; i++) {
                            curl.put(String.format("data%s", i + 1), arr[i]);
                        }
                    } else {
                        curl.put("data1", String.valueOf(args));
                    }
                }
                String response = curl.doPost(String.format("%s/public/log1?token=%s", site, token));
                if (Utils.isEmpty(response))
                    System.err.println(String.format("token: %s, json: %s", token, message));
                else {
                    DataRow row = new DataRow().setJson(response);
                    if (!row.getBoolean("result"))
                        System.err.println(row.getString("message"));
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        });
        return true;
    }

    public static String key(String key) {
        return String.format("%s.%s", prefix, key);
    }

}
