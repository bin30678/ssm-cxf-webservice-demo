package com.example.cxfdemo.service;

import com.example.cxfdemo.config.ConfigSingleton;
import com.example.cxfdemo.model.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.Properties;

/**
 * 郵件發送服務實作類別 (MailServiceImpl)
 * Mail 相關連線設定 (Host, Port, Account, Auth) 直接於 Impl 內部程式碼手動建立，自 ConfigSingleton 讀取，不透過 Spring 設定檔。
 */
@Service("mailService")
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    /**
     * 於 Impl 內部直接建立與配置 JavaMailSenderImpl
     */
    private JavaMailSenderImpl createMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // 從 ConfigSingleton 讀取連線屬性
        String host = ConfigSingleton.getMappingData("mail.host");
        if (host == null || host.trim().isEmpty()) {
            host = ConfigSingleton.getMappingData("mail.smtp.host");
        }
        if (host == null || host.trim().isEmpty()) {
            host = "smtp.example.com";
        }

        String portStr = ConfigSingleton.getMappingData("mail.port");
        if (portStr == null || portStr.trim().isEmpty()) {
            portStr = ConfigSingleton.getMappingData("mail.smtp.port");
        }
        int port = 25;
        if (portStr != null && !portStr.trim().isEmpty()) {
            try {
                port = Integer.parseInt(portStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        String username = ConfigSingleton.getMappingData("mail.username");
        if (username == null) {
            username = ConfigSingleton.getMappingData("mail.smtp.username");
        }

        String password = ConfigSingleton.getMappingData("mail.password");
        if (password == null) {
            password = ConfigSingleton.getMappingData("mail.smtp.password");
        }

        String protocol = ConfigSingleton.getMappingData("mail.transport.protocol");
        if (protocol == null || protocol.trim().isEmpty()) {
            protocol = "smtp";
        }

        mailSender.setHost(host);
        mailSender.setPort(port);
        if (username != null) mailSender.setUsername(username);
        if (password != null) mailSender.setPassword(password);
        mailSender.setProtocol(protocol);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.auth", ConfigSingleton.getMappingData("mail.smtp.auth") != null ? ConfigSingleton.getMappingData("mail.smtp.auth") : "true");
        props.put("mail.smtp.starttls.enable", ConfigSingleton.getMappingData("mail.smtp.starttls.enable") != null ? ConfigSingleton.getMappingData("mail.smtp.starttls.enable") : "true");
        props.put("mail.debug", ConfigSingleton.getMappingData("mail.debug") != null ? ConfigSingleton.getMappingData("mail.debug") : "false");

        log.info("Configured JavaMailSenderImpl internally with host: {}, port: {}, protocol: {}", host, port, protocol);
        return mailSender;
    }

    @Override
    public void sendMail(Mail mail) {
        if (mail == null) {
            log.warn("MailServiceImpl.sendMail called with null mail object.");
            return;
        }

        // 從 ConfigSingleton 讀取寄信者 (from)
        String from = ConfigSingleton.getMappingData("mail.from");
        if (from == null || from.trim().isEmpty()) {
            from = ConfigSingleton.getMappingData("system.mail.from");
        }
        if (from == null || from.trim().isEmpty()) {
            from = "no-reply@example.com";
        }

        log.info("MailServiceImpl.sendMail starting... From: {}, Subject: {}", from, mail.getSubject());

        try {
            // 直接於 Impl 內部建立 MailSender
            JavaMailSenderImpl mailSender = createMailSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);

            if (mail.getTo() != null && !mail.getTo().isEmpty()) {
                helper.setTo(mail.getTo().toArray(new String[0]));
            }

            if (mail.getCc() != null && !mail.getCc().isEmpty()) {
                helper.setCc(mail.getCc().toArray(new String[0]));
            }

            helper.setSubject(mail.getSubject() != null ? mail.getSubject() : "");
            helper.setText(mail.getContent() != null ? mail.getContent() : "", true);

            if (mail.getAttachmentPaths() != null && !mail.getAttachmentPaths().isEmpty()) {
                for (String path : mail.getAttachmentPaths()) {
                    File file = new File(path);
                    if (file.exists()) {
                        FileSystemResource resource = new FileSystemResource(file);
                        helper.addAttachment(resource.getFilename(), resource);
                        log.info("Added mail attachment: {}", resource.getFilename());
                    } else {
                        log.warn("Attachment file not found at path: {}", path);
                    }
                }
            }

            mailSender.send(message);
            log.info("MailServiceImpl.sendMail successfully sent email to: {}", mail.getTo());

        } catch (Exception e) {
            log.error("MailServiceImpl.sendMail failed for mail: {}", mail, e);
            throw new RuntimeException("Send mail failed", e);
        }
    }
}
