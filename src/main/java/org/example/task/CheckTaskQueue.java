package org.example.task;

import lombok.extern.slf4j.Slf4j;
import org.example.bean.enumtype.TaskStatusEnum;
import org.example.entity.LoginAccount;
import org.example.entity.TaskQueue;
import org.example.exception.ApiException;
import org.example.exception.SysCode;
import org.example.service.LoginService;
import org.example.service.TaskQueueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Eric.Lee
 * Date: 2024/2/17
 */
@Slf4j
@Service("checkTaskQueue")
public class CheckTaskQueue extends BaseQueue {
    private final TaskQueueService taskQueueService;
    private final LoginService loginService;
    private final TaskExecutionService taskExecutionService;

    public CheckTaskQueue(TaskQueueService taskQueueService, LoginService loginService, TaskExecutionService taskExecutionService) {
        this.taskQueueService = taskQueueService;
        this.loginService = loginService;
        this.taskExecutionService = taskExecutionService;
    }

    @Scheduled(fixedDelayString = "${taskQueue.checkDelay:10000}")
    public void checkAndExecuteNeedLoginTasks() {
        log.info("檢查任務序列開始");
        if (!checkTaskEnabled()) return;
        if (isInProgressTaskExists()) return;

        try {
            LoginAccount loginAccount = loginService.getLoginAccount();
            TaskQueue task = getTask(Arrays.asList(TaskStatusEnum.PAUSED, TaskStatusEnum.PENDING));
            updateAndExecuteTask(task, loginAccount);
        } catch (ApiException e) {
            log.info("檢查任務序列發生預期事件 {}", e.getMessage());
        } catch (Exception e) {
            log.error("檢查任務序列發生特殊錯誤", e);
            stopBaseQueue();
        }
        log.info("檢查任務序列結束");
    }

    //private

    /**
     * 檢查是否有正在執行中且需要登入的任務
     *
     * @return 是否有正在執行中的任務
     */
    private boolean isInProgressTaskExists() {
        boolean exists = taskQueueService.checkTasksByStatusAndNeedLogin(TaskStatusEnum.IN_PROGRESS, true);
        if (exists) {
            log.info("有需要登入(NEED LOGIN IG)的任務正在執行中(IN_PROGRESS)，不執行新任務");
        }
        return exists;
    }

    /**
     * 根據任務狀態列表statusList查詢須執行的任務是否存在
     *
     * @param statusList 任務狀態列表
     */
    private TaskQueue getTask(List<TaskStatusEnum> statusList) {
        return statusList.stream()
                .map(status -> taskQueueService.findFirstTaskQueueByStatusAndNeedLogin(status, true))
                .filter(Optional::isPresent)
                .findFirst()
                .orElseThrow(() -> new ApiException(SysCode.NO_TASKS_TO_PERFORM, "沒有需要執行的任務"))
                .get();
    }

    /**
     * 先更新任務狀態，再執行任務
     *
     * @param taskQueue 任務
     */
    private void updateAndExecuteTask(TaskQueue taskQueue, LoginAccount loginAccount) {
        // 更新任務狀態為IN_PROGRESS
        TaskQueue latestTaskQueue = taskQueueService.updateTaskStatus(taskQueue.getId(), TaskStatusEnum.IN_PROGRESS);
        // 執行任務
        taskExecutionService.executeTask(latestTaskQueue, loginAccount);
    }

}
