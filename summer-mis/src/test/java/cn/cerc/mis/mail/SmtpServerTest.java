package cn.cerc.mis.mail;

import java.util.Properties;

import cn.cerc.core.Datetime;

public class SmtpServerTest {

    // @Test
    public void test_aliyun() {
        Properties prop = new Properties();
        prop.setProperty(SmtpServer.MAIL_SMTP_HOST, "smtp.mxhichina.com");
        prop.setProperty(SmtpServer.MAIL_ACCOUNT, "support@diteng.site");
        // prop.setProperty(SmtpServer.MAIL_ALIAS, "地藤管家");
        // prop.setProperty(SmtpServer.MAIL_PASSWORD, "");
        // prop.setProperty(SmtpServer.MAIL_SMTP_DEBUG, "true");

        Mail client = new SmtpServer(prop).createMail("l1091462907@qq.com", "itjun");
        // client.addFile("d:\\a.txt");
        // client("d:\\b.txt");
        client.setSubject("test mail " + new Datetime().toString());
        client.setContent("欢迎使用地藤软件");
        client.send();
    }

    // @Test
    public void test_qq() {
        Properties prop = new Properties();
        prop.setProperty(SmtpServer.MAIL_SMTP_HOST, "smtp.exmail.qq.com");
        prop.setProperty(SmtpServer.MAIL_ACCOUNT, "develop@mimrc.com");
        prop.setProperty(SmtpServer.MAIL_PASSWORD, "Mimrc2011");
        prop.setProperty(SmtpServer.MAIL_ALIAS, "地藤管家");
        prop.setProperty(SmtpServer.MAIL_SMTP_DEBUG, "true");

        Mail client = new SmtpServer(prop).createMail("l1091462907@qq.com", "itjun");
        client.setSubject("test mail " + new Datetime().toString());
        client.setContent("欢迎使用地藤软件");
        client.send();
    }
}
