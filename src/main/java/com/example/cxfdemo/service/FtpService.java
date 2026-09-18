package com.example.cxfdemo.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Service
public class FtpService {

    private static final Logger log = LoggerFactory.getLogger(FtpService.class);

    public boolean uploadFile(String server, int port, String user, String pass, String remoteDirPath, File localFile) {
        log.info("FtpService.uploadFile connecting to server: {}:{}, remoteDirPath: {}", server, port, remoteDirPath);
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Change to remote directory
            if (!ftpClient.changeWorkingDirectory(remoteDirPath)) {
                ftpClient.makeDirectory(remoteDirPath);
                ftpClient.changeWorkingDirectory(remoteDirPath);
            }

            try (InputStream inputStream = new FileInputStream(localFile)) {
                log.info("Start uploading file: {}", localFile.getName());
                boolean done = ftpClient.storeFile(localFile.getName(), inputStream);
                if (done) {
                    log.info("The file {} was uploaded successfully to FTP server.", localFile.getName());
                    return true;
                }
            }
        } catch (Exception ex) {
            log.error("FtpService.uploadFile error: {}", ex.getMessage(), ex);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (Exception ex) {
                log.error("Failed to disconnect FTP client", ex);
            }
        }
        return false;
    }
}
