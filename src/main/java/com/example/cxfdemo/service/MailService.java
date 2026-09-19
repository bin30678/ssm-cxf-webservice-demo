package com.example.cxfdemo.service;

import com.example.cxfdemo.model.Mail;

/**
 * 郵件發送服務介面 (MailService)
 */
public interface MailService {

    /**
     * 依據 Mail 傳入之主旨、內文、收件者與附件發送郵件。
     * 寄件者 (from) 將動態透過 ConfigSingleton.getMappingData() 讀取。
     *
     * @param mail 郵件模型 DTO
     */
    void sendMail(Mail mail);
}
