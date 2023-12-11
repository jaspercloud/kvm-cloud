package cn.chenjun.cloud.management.operate.impl;

import cn.chenjun.cloud.common.bean.ResultUtil;
import cn.chenjun.cloud.common.util.ErrorCode;
import cn.chenjun.cloud.management.data.entity.HostEntity;
import cn.chenjun.cloud.management.data.entity.NetworkEntity;
import cn.chenjun.cloud.management.operate.bean.CreateNetworkOperate;
import cn.chenjun.cloud.management.operate.bean.InitHostNetworkOperate;
import cn.chenjun.cloud.management.util.Constant;
import cn.chenjun.cloud.management.websocket.message.NotifyData;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 创建网络
 *
 * @author chenjun
 */
@Component
@Slf4j
public class CreateNetworkOperateImpl extends AbstractOperate<CreateNetworkOperate, ResultUtil<Void>> {


    @Override
    public void operate(CreateNetworkOperate param) {
        List<HostEntity> hosts = hostMapper.selectList(new QueryWrapper<>());
        List<Integer> hostIds = hosts.stream().filter(t -> Objects.equals(Constant.HostStatus.ONLINE, t.getStatus())).map(HostEntity::getHostId).collect(Collectors.toList());
        InitHostNetworkOperate operate = InitHostNetworkOperate.builder().taskId(UUID.randomUUID().toString())
                .title(param.getTitle())
                .networkId(param.getNetworkId())
                .networkId(param.getNetworkId())
                .nextHostIds(hostIds)
                .build();
        this.operateTask.addTask(operate);
        this.onSubmitFinishEvent(param.getTaskId(), ResultUtil.success());
    }

    @Override
    public Type getCallResultType() {
        return new TypeToken<ResultUtil<Void>>() {
        }.getType();
    }

    @Override
    public void onFinish(CreateNetworkOperate param, ResultUtil<Void> resultUtil) {
        if (resultUtil.getCode() != ErrorCode.SUCCESS) {
            NetworkEntity network = networkMapper.selectById(param.getNetworkId());
            if (network != null && Objects.equals(network.getStatus(), Constant.NetworkStatus.CREATING)) {
                network.setStatus(Constant.NetworkStatus.ERROR);
                networkMapper.updateById(network);
            }
        }

        this.eventService.publish(NotifyData.<Void>builder().id(param.getNetworkId()).type(cn.chenjun.cloud.common.util.Constant.NotifyType.UPDATE_NETWORK).build());

    }

    @Override
    public int getType() {
        return cn.chenjun.cloud.management.util.Constant.OperateType.CREATE_NETWORK;
    }
}
