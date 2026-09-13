package com.example.cxfdemo.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Service
public class FtpService {

    public boolean uploadFile(String server, int port, String user, String pass, String remoteDirPath, File localFile) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            // Change to remote directory
            if (!ftpClient.changeWorkingDirectory(remoteDirPath)) {
                // If it doesn't exist, you might need to create it (simplified here)
                ftpClient.makeDirectory(remoteDirPath);
                ftpClient.changeWorkingDirectory(remoteDirPath);
            }

            try (InputStream inputStream = new FileInputStream(localFile)) {
                System.out.println("Start uploading first file");
                boolean done = ftpClient.storeFile(localFile.getName(), inputStream);
                if (done) {
                    System.out.println("The file is uploaded successfully.");
                    return true;
                }
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }
}
