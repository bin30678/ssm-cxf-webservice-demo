package com.example.cxfdemo.service;

import com.example.cxfdemo.model.FileUploadRecord;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class FileStorageServiceTest {

    private Path tempDir;
    private FileStorageService fileStorageService;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test_upload_dir_");
        // 設�??��?100 bytes
        fileStorageService = new FileStorageService(tempDir.toString(), 100L);
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    public void testStore_normalFile_shouldSucceed() throws IOException {
        byte[] content = "Hello, World! This is a test file.".getBytes();
        InputStream input = new ByteArrayInputStream(content);

        FileUploadRecord record = fileStorageService.store(input, "my test file@#$.txt", "text/plain", "Test description");

        Assert.assertNotNull(record);
        Assert.assertEquals("my test file@#$.txt", record.getOriginalName());
        Assert.assertEquals("text/plain", record.getContentType());
        Assert.assertEquals((long) content.length, (long) record.getSizeBytes());
        Assert.assertEquals("Test description", record.getDescription());
        Assert.assertNotNull(record.getCreatedAt());

        // 檔�??��?字�??�該被�?�?        Assert.assertTrue(record.getStoredName().contains("my_test_file___.txt"));

        // 檢查檔�??�否?�實寫入?��?
        Path storedFile = tempDir.resolve(record.getStoredName());
        Assert.assertTrue(Files.exists(storedFile));
        Assert.assertEquals(content.length, Files.size(storedFile));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStore_exceedMaxSize_shouldThrowExceptionAndCleanUp() throws IOException {
        // 超�? 100 bytes (120 bytes)
        byte[] content = new byte[120];
        InputStream input = new ByteArrayInputStream(content);

        try {
            fileStorageService.store(input, "large_file.bin", "application/octet-stream", "Too large");
        } finally {
            // 驗�?沒�?殘�??��??��?檔�?
            long fileCount = Files.list(tempDir).count();
            Assert.assertEquals(0, fileCount);
        }
    }

    @Test
    public void testStore_emptyOrPathTraversalFilename() throws IOException {
        byte[] content = "Safe content".getBytes();
        InputStream input = new ByteArrayInputStream(content);

        FileUploadRecord record = fileStorageService.store(input, "../../evil_script.sh", "text/x-shellscript", "Security check");

        // 路�??��?符�??�被清除，只?��?檔�??��?並替?��?安全字�?
        Assert.assertFalse(record.getStoredName().contains(".."));
        Assert.assertTrue(record.getStoredName().contains("evil_script.sh"));
    }
}
