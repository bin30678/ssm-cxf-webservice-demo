package com.example.cxfdemo.service;

import com.example.cxfdemo.dao.TransferTaskDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

@Service
public class AsyncBankTransferService {

    private static final Logger log = LoggerFactory.getLogger(AsyncBankTransferService.class);

    @Resource
    private TransferTaskDao transferTaskDao;

    public void setTransferTaskDao(TransferTaskDao transferTaskDao) {
        this.transferTaskDao = transferTaskDao;
    }

    /**
     * 模擬被外部 API 觸發非同步轉帳任務
     * @return taskId
     */
    public String startAsyncTransferTask() {
        final String taskId = UUID.randomUUID().toString();
        
        // 1. 建立主表紀錄 (初始狀態 0: 待處理)
        insertMasterRecord(taskId, 0);
        log.info("主表已建立，Task ID: {}，開始非同步執行..", taskId);

        // 2. 啟動背景執行緒 (不阻塞原本的 HTTP API 請求)
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isSuccess = false;
                
                // 3. 重試機制: 最多 20 次
                for (int i = 1; i <= 20; i++) {
                    log.info("--- Task {} 開始第 {} 次嘗試 ---", taskId, i);
                    
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
                        log.info("Task {} 處理成功，結束重試迴圈。", taskId);
                        break; 
                        
                    } catch (Exception e) {
                        // 失敗當次，主表不改變狀態，只在子表記錄失敗原因
                        log.warn("Task {} 第 {} 次嘗試發生錯誤: {}", taskId, i, e.getMessage());
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
                    log.error("Task {} 已達到重試上限 (20次)，宣告失敗。", taskId);
                    updateMasterStatus(taskId, 99); // 99 代表徹底失敗
                    insertDetailRecord(taskId, 99, "已達重試上限，任務終止", false);
                }
            }
        }).start();

        return taskId;
    }

    // ==========================================
    // 以下為各個步驟與資料庫操作
    // ==========================================
    
    private String fetchExternalData() throws Exception {
        // 模擬偶爾失敗 (例如模擬網路逾時)
        if (Math.random() < 0.3) throw new Exception("外部系統連線 Timeout");
        return "{ \"data\": \"raw\" }";
    }

    private String processJson(String json) throws Exception {
        return "{ \"data\": \"processed\" }";
    }

    private String transformToBankFormat(String json) throws Exception {
        if (Math.random() < 0.1) throw new Exception("資料欄位檢核失敗，無法轉換");
        return "<bank>formatted</bank>";
    }

    private void sendToBank(String data) throws Exception {
        if (Math.random() < 0.1) throw new Exception("銀行端主機忙線中");
    }

    private String getStatusDesc(int status) {
        switch (status) {
            case 0: return "待處理";
            case 1: return "已取得外部資料";
            case 2: return "JSON處理完成";
            case 3: return "格式轉換完成";
            case 4: return "發送銀行成功";
            case 99: return "失敗終止";
            default: return "處理中(" + status + ")";
        }
    }

    private void insertMasterRecord(String taskId, int status) {
        if (transferTaskDao != null) {
            transferTaskDao.insertMaster(taskId, status, getStatusDesc(status));
        }
        log.info("[DB] 主表已建立 -> Task ID: {}, 狀態: {} ({})", taskId, status, getStatusDesc(status));
    }

    private void updateMasterStatus(String taskId, int status) {
        if (transferTaskDao != null) {
            transferTaskDao.updateMasterStatus(taskId, status, getStatusDesc(status));
        }
        log.info("[DB] 主表狀態更新為: {} ({})", status, getStatusDesc(status));
    }

    private void insertDetailRecord(String taskId, int type, String message, boolean isSuccess) {
        if (transferTaskDao != null) {
            transferTaskDao.insertDetail(taskId, type, message, isSuccess);
        }
        log.info("[DB] 寫入子表 -> 步驟: {}, 成功: {}, 訊息: {}", type, isSuccess, message);
    }
}
