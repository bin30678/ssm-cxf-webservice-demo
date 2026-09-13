package com.example.cxfdemo.service;

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

    @Resource(name = "mailSender")
    private JavaMailSender mailSender;

    /**
     * 寄�?Email (?�援?�件?�CC)
     *
     * @param from 寄件�?     * @param to ?�件�?(多�?)
     * @param cc ?�本 (多�?)
     * @param subject 主旨
     * @param body ?��? (?�為 HTML)
     * @param attachmentPaths ?�件檔�?路�?清單
     */
    public void sendMail(String from, List<String> to, List<String> cc, String subject, String body, List<String> attachmentPaths) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            
            // true 表示?�許多部??(Multipart)，支?��?件�? HTML
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
            System.out.println("Email sent successfully to: " + to);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
