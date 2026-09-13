package com.example.cxfdemo.service;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class AsyncBankTransferService {

    /**
     * 模擬被外部 API 觸發非同步轉帳任務
     */
    public void startAsyncTransferTask() {
        final String taskId = UUID.randomUUID().toString();
        
        // 1. 建立主表紀錄 (初始狀態 0: 待處理)
        insertMasterRecord(taskId, 0);
        System.out.println("主表已建立，Task ID: " + taskId + "，開始非同步執行..");

        // 2. 啟動背景執行緒 (不阻塞原本的 HTTP API 請求)
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isSuccess = false;
                
                // 3. 重試機制: 最多 20 次
                for (int i = 1; i <= 20; i++) {
                    System.out.println("--- 開始第 " + i + " 次嘗試 ---");
                    
                    try {
                        // 步驟 1: 取得外部資料
                        String rawData = fetchExternalData();
                        updateMasterStatus(taskId, 1);
                        insertDetailRecord(taskId, 1, "步驟1成功: 取得資料", true);
                        
                        // 步驟 2: 處理 JSON
                        String processedJson = processJson(rawData);
                        updateMasterStatus(taskId, 2);
                        insertDetailRecord(taskId, 2, "步驟2成功: 處理 JSON", true);
                        
                        // 步驟 3: 轉換為特定銀行要求格式
                        String bankFormatData = transformToBankFormat(processedJson);
                        updateMasterStatus(taskId, 3);
                        insertDetailRecord(taskId, 3, "步驟3成功: 轉換格式", true);
                        
                        // 步驟 4: 發送給銀行
                        sendToBank(bankFormatData);
                        updateMasterStatus(taskId, 4); // 4 代表大功告成
                        insertDetailRecord(taskId, 4, "步驟4成功: 發送銀行成功", true);
                        
                        // 四個步驟都沒拋出異常，代表成功！跳出迴圈
                        isSuccess = true;
                        System.out.println("Task " + taskId + " 處理成功，結束重試迴圈。");
                        break; 
                        
                    } catch (Exception e) {
                        // 失敗當次，主表不改變狀態，只在子表記錄失敗原因
                        System.out.println("第 " + i + " 次嘗試發生錯誤: " + e.getMessage());
                        insertDetailRecord(taskId, -1, "發生錯誤: " + e.getMessage(), false);
                        
                        // Sleep 1 秒後重跑下一迴圈 (這裡為測試展示，縮短為 1 秒)
                        try {
                            Thread.sleep(1000); // 實務上可能為 60000 毫秒
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                
                // 4. 如果跑滿 20 次仍然失敗
                if (!isSuccess) {
                    System.out.println("Task " + taskId + " 已達到重試上限 (20次)，宣告失敗。");
                    updateMasterStatus(taskId, 99); // 99 代表徹底失敗
                    insertDetailRecord(taskId, 99, "已達重試上限，任務終止", false);
                }
            }
        }).start();
    }

    // ==========================================
    // 以下為模擬各個步驟與資料庫操作 (使用 Console 模擬)
    // ==========================================
    
    private String fetchExternalData() throws Exception {
        // 模擬偶爾失敗 (例如模擬網路逾時)
        if (Math.random() < 0.5) throw new Exception("外部系統連線 Timeout");
        return "{ \"data\": \"raw\" }";
    }

    private String processJson(String json) throws Exception {
        return "{ \"data\": \"processed\" }";
    }

    private String transformToBankFormat(String json) throws Exception {
        if (Math.random() < 0.2) throw new Exception("資料欄位檢核失敗，無法轉換");
        return "<bank>formatted</bank>";
    }

    private void sendToBank(String data) throws Exception {
        if (Math.random() < 0.3) throw new Exception("銀行端主機忙線中");
    }

    private void insertMasterRecord(String taskId, int status) {
        // 實務上用 DAO: INSERT INTO master_table (task_id, status) VALUES (...)
    }

    private void updateMasterStatus(String taskId, int status) {
        // 實務上用 DAO: UPDATE master_table SET status = ? WHERE task_id = ?
        System.out.println("[DB] 主表狀態更新為: " + status);
    }

    private void insertDetailRecord(String taskId, int type, String message, boolean isSuccess) {
        // 實務上用 DAO: INSERT INTO detail_table (task_id, type, message, success, create_time)
        System.out.println("[DB] 寫入子表 -> 步驟: " + type + ", 成功: " + isSuccess + ", 訊息: " + message);
    }
}
