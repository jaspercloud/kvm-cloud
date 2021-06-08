package com.roamblue.cloud.management.service.impl;

import com.roamblue.cloud.common.error.CodeException;
import com.roamblue.cloud.common.util.ErrorCode;
import com.roamblue.cloud.management.bean.VmNetworkInfo;
import com.roamblue.cloud.management.data.entity.VmNetworkEntity;
import com.roamblue.cloud.management.data.mapper.VmNetworkMapper;
import com.roamblue.cloud.management.service.NetworkAllocateService;
import com.roamblue.cloud.management.util.IpType;
import com.roamblue.cloud.management.util.NetworkType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BriaggeNetworkAllocateServiceImpl implements NetworkAllocateService {

    @Autowired
    private VmNetworkMapper vmNetworkMapper;

    @Override
    public VmNetworkInfo allocateManagerAddress(int networkId, int vmId) {
        return this.allocate(networkId, vmId, IpType.MANAGER);
    }

    @Override
    public VmNetworkInfo allocateGuestAddress(int networkId, int vmId) {
        return this.allocate(networkId, vmId, IpType.GUEST);
    }

    private VmNetworkInfo allocate(int networkId, int vmId, String ipType) {
        int deviceId = 0;
        List<Integer> deviceIds = vmNetworkMapper.findByVmId(vmId).stream().map(VmNetworkEntity::getVmDevice).collect(Collectors.toList());

        while (deviceIds.contains(deviceId)) {
            deviceId++;
        }
        VmNetworkEntity instanceNetworkEntity = null;
        boolean allocate = false;
        List<VmNetworkEntity> list = vmNetworkMapper.findByNetworkIdAndIpType(networkId, ipType).stream().filter(t -> t.getVmId().equals(0)).collect(Collectors.toList());
        while (list.size() > 0 && !allocate) {
            instanceNetworkEntity = list.remove(0);
            allocate = vmNetworkMapper.allocateNetwork(instanceNetworkEntity.getId(), vmId) > 0;
        }
        if (!allocate) {
            throw new CodeException(ErrorCode.NETWORK_NOT_SPACE, "网络不可用或无可用地址");
        }
        instanceNetworkEntity.setVmDevice(deviceId);
        instanceNetworkEntity.setVmId(vmId);
        VmNetworkInfo info = this.initInstanceNetwork(instanceNetworkEntity);
        log.info("申请网络信息成功.networkIds={} vmId={}", networkId, vmId);
        return info;
    }

    @Override
    public String getType() {
        return NetworkType.BRIDGEE;
    }

    private VmNetworkInfo initInstanceNetwork(VmNetworkEntity entity) {
        return VmNetworkInfo.builder()
                .id(entity.getId())
                .networkId(entity.getNetworkId())
                .clusterId(entity.getClusterId())
                .vmId(entity.getVmId())
                .device(entity.getVmDevice())
                .mac(entity.getNetworkMac())
                .ip(entity.getNetworkIp())
                .status(entity.getNetworkStatus())
                .createTime(entity.getCreateTime())
                .build();
    }

}