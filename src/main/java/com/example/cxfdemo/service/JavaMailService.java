package com.example.cxfdemo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;

@Service
public class JavaMailService {

    private static final Logger log = LoggerFactory.getLogger(JavaMailService.class);

    @Resource(name = "mailSender")
    private JavaMailSender mailSender;

    /**
     * 寄送 Email (支援附件與 CC)
     *
     * @param from 寄件者
     * @param to 收件者 (多個)
     * @param cc 副本 (多個)
     * @param subject 主旨
     * @param body 內容 (可為 HTML)
     * @param attachmentPaths 附件檔案路徑清單
     */
    public void sendMail(String from, List<String> to, List<String> cc, String subject, String body, List<String> attachmentPaths) {
        log.info("JavaMailService.sendMail called from: {}, to: {}, subject: {}", from, to, subject);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            
            // true 表示為許多部分 (Multipart)，支援附件與 HTML
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(from);
            
            if (to != null && !to.isEmpty()) {
                helper.setTo(to.toArray(new String[0]));
            }
            
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }
            
            helper.setSubject(subject);
            helper.setText(body, true); // true = isHtml
            
            if (attachmentPaths != null) {
                for (String path : attachmentPaths) {
                    FileSystemResource file = new FileSystemResource(new File(path));
                    helper.addAttachment(file.getFilename(), file);
                }
            }
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
            
        } catch (Exception e) {
            log.error("JavaMailService.sendMail failed for to: {}", to, e);
        }
    }
}
