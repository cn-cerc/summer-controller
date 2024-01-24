package cn.cerc.mis.mail;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mail {
    private static final Logger log = LoggerFactory.getLogger(Mail.class);

    private SmtpServer server;
    private InternetAddress to;
    private String subject;
    private String content = "";
    private List<String> files = new ArrayList<>();

    public Mail(String toEmailAddress) {
        super();
        try {
            this.to = new InternetAddress(toEmailAddress);
        } catch (AddressException e) {
            log.error("{} -> error {}", toEmailAddress, e.getMessage(), e);
        }
    }

    public Mail(String toEmailAddress, String toPersonalName) {
        super();
        try {
            this.to = new InternetAddress(toEmailAddress, toPersonalName);
        } catch (UnsupportedEncodingException e) {
            log.error("{} -> error {}", toEmailAddress, e.getMessage(), e);
        }
    }

    public boolean send() {
        try {
            server.send(this);
            return true;
        } catch (UnsupportedEncodingException | MessagingException | GeneralSecurityException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean send(String subject, String content) {
        this.subject = subject;
        this.content = content;
        return this.send();
    }

    public SmtpServer getServer() {
        return server;
    }

    public void setServer(SmtpServer server) {
        this.server = server;
    }

    public List<String> getFiles() {
        return files;
    }

    public void addFile(String file) {
        this.files.add(file);
    }

    public InternetAddress getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
