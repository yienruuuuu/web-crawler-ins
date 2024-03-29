package org.example.task;

import org.example.entity.LoginAccount;
import org.example.entity.TaskQueue;

/**
 * @author Eric.Lee
 * Date:2024/2/19
 */
public interface TaskExecutionService {
    /**
     * 採策略模式執行任務
     *
     * @param task 任務
     */
    void executeTask(TaskQueue task, LoginAccount loginAccount);
}
