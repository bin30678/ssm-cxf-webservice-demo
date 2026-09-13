package com.example.cxfdemo.scheduler;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class ScheduledTasksTest {

    private File testDir;
    private File newFile;
    private File oldFile;
    private File subDir;
    private File oldFileInSubDir;

    @Before
    public void setUp() throws IOException {
        testDir = new File(System.getProperty("java.io.tmpdir"), "test_backup_" + System.currentTimeMillis());
        testDir.mkdirs();

        newFile = new File(testDir, "new_backup.txt");
        newFile.createNewFile();

        oldFile = new File(testDir, "old_backup.txt");
        oldFile.createNewFile();
        long tenDaysAgo = System.currentTimeMillis() - (10L * 24L * 60L * 60L * 1000L);
        oldFile.setLastModified(tenDaysAgo);

        subDir = new File(testDir, "sub_folder");
        subDir.mkdirs();
        oldFileInSubDir = new File(subDir, "old_sub_backup.txt");
        oldFileInSubDir.createNewFile();
        oldFileInSubDir.setLastModified(tenDaysAgo);
    }

    @Test
    public void testCleanUpOldFiles_deletesExpiredFilesRecursively() throws Exception {
        ScheduledTasks tasks = new ScheduledTasks();

        Method method = ScheduledTasks.class.getDeclaredMethod("cleanUpOldFiles", File.class, long.class, long.class);
        method.setAccessible(true);

        long maxAgeMillis = 7L * 24L * 60L * 60L * 1000L;
        method.invoke(tasks, testDir, System.currentTimeMillis(), maxAgeMillis);

        Assert.assertTrue("新檔案應該要被保留", newFile.exists());
        Assert.assertFalse("超過 7 天的舊檔案應該要被刪除", oldFile.exists());
        Assert.assertFalse("子目錄裡超過 7 天的舊檔案也應該被刪除", oldFileInSubDir.exists());
        Assert.assertFalse("清空後的子目錄也應該被刪除", subDir.exists());
    }

    @After
    public void tearDown() {
        if (newFile != null && newFile.exists()) newFile.delete();
        if (oldFile != null && oldFile.exists()) oldFile.delete();
        if (oldFileInSubDir != null && oldFileInSubDir.exists()) oldFileInSubDir.delete();
        if (subDir != null && subDir.exists()) subDir.delete();
        if (testDir != null && testDir.exists()) testDir.delete();
    }
}
