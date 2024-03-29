package cn.chenjun.cloud.agent.operate.impl;

import cn.chenjun.cloud.agent.operate.TaskService;
import cn.chenjun.cloud.agent.operate.annotation.DispatchBind;
import cn.chenjun.cloud.agent.util.TaskIdUtil;
import cn.chenjun.cloud.common.bean.NoneRequest;
import cn.chenjun.cloud.common.util.Constant;
import lombok.extern.slf4j.Slf4j;
import org.libvirt.Connect;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chenjun
 */
@Slf4j
@Service
public class TaskServiceImpl implements TaskService {
    @Override
    @DispatchBind(command = Constant.Command.CHECK_TASK)
    public List<String> checkTask(Connect connect, NoneRequest request) {
        return TaskIdUtil.getTaskList();
    }
}
