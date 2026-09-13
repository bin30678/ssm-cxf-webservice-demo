package com.example.cxfdemo.controller;

import com.example.cxfdemo.scheduler.ScheduledTasks;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class TaskControllerTest {

    private File testDir;
    private File oldFile;
    private File newFile;
    private TaskController controller;

    @Before
    public void setUp() throws Exception {
        testDir = new File(System.getProperty("java.io.tmpdir"), "test_ctrl_backup_" + System.currentTimeMillis());
        testDir.mkdirs();

        newFile = new File(testDir, "new_file.txt");
        newFile.createNewFile();

        oldFile = new File(testDir, "old_file.txt");
        oldFile.createNewFile();
        long tenDaysAgo = System.currentTimeMillis() - (10L * 24L * 60L * 60L * 1000L);
        oldFile.setLastModified(tenDaysAgo);

        controller = new TaskController();
        ScheduledTasks tasks = new ScheduledTasks();

        Field scheduledTasksField = TaskController.class.getDeclaredField("scheduledTasks");
        scheduledTasksField.setAccessible(true);
        scheduledTasksField.set(controller, tasks);
    }

    @Test
    public void testCleanBackupTrigger() {
        String jsonResponse = controller.cleanBackup(testDir.getAbsolutePath(), 7);
        Assert.assertNotNull(jsonResponse);

        JsonObject json = new JsonParser().parse(jsonResponse).getAsJsonObject();
        Assert.assertTrue(json.get("success").getAsBoolean());
        Assert.assertEquals(1, json.get("deletedFilesCount").getAsInt());
        Assert.assertFalse("過期檔案應被刪除", oldFile.exists());
        Assert.assertTrue("新檔案應被保留", newFile.exists());
    }

    @Test
    public void testCleanBackupNonExistentDir() {
        String jsonResponse = controller.cleanBackup("Z:/non_existent_folder_xyz", 7);
        Assert.assertNotNull(jsonResponse);

        JsonObject json = new JsonParser().parse(jsonResponse).getAsJsonObject();
        Assert.assertFalse(json.get("success").getAsBoolean());
    }

    @After
    public void tearDown() {
        if (newFile != null && newFile.exists()) newFile.delete();
        if (oldFile != null && oldFile.exists()) oldFile.delete();
        if (testDir != null && testDir.exists()) testDir.delete();
    }
}
