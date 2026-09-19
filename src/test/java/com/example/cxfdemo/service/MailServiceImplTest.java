package com.example.cxfdemo.service;

import com.example.cxfdemo.config.ConfigSingleton;
import com.example.cxfdemo.model.Mail;
import org.junit.Before;
import org.junit.Test;

public class MailServiceImplTest {

    private MailServiceImpl mailService;

    @Before
    public void setUp() {
        mailService = new MailServiceImpl();
        ConfigSingleton.getInstance().clear();
        ConfigSingleton.getInstance().getMap().put("mail.from", "admin@example.com");
        ConfigSingleton.getInstance().getMap().put("mail.host", "localhost");
        ConfigSingleton.getInstance().getMap().put("mail.port", "25");
    }

    @Test
    public void testMailModelAndConfigIntegration() {
        Mail mail = new Mail("測試主旨", "<h1>測試內容</h1>");
        mail.addTo("user1@example.com");
        mail.addCc("boss@example.com");

        org.junit.Assert.assertEquals("測試主旨", mail.getSubject());
        org.junit.Assert.assertEquals("<h1>測試內容</h1>", mail.getContent());
        org.junit.Assert.assertEquals(1, mail.getTo().size());
        org.junit.Assert.assertEquals(1, mail.getCc().size());

        org.junit.Assert.assertEquals("admin@example.com", ConfigSingleton.getMappingData("mail.from"));
    }
}
