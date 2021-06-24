package com.roamblue.cloud.management.service.impl;

import com.roamblue.cloud.common.agent.VmInfoModel;
import com.roamblue.cloud.common.agent.VmModel;
import com.roamblue.cloud.common.bean.ResultUtil;
import com.roamblue.cloud.common.error.CodeException;
import com.roamblue.cloud.common.util.ErrorCode;
import com.roamblue.cloud.management.bean.*;
import com.roamblue.cloud.management.data.entity.HostEntity;
import com.roamblue.cloud.management.data.entity.VmEntity;
import com.roamblue.cloud.management.data.mapper.StorageMapper;
import com.roamblue.cloud.management.data.mapper.VmMapper;
import com.roamblue.cloud.management.service.*;
import com.roamblue.cloud.management.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class AbstractVmService implements VmService {


    @Autowired
    protected StorageService storageService;
    @Autowired
    protected TemplateService templateService;

    @Autowired
    protected VolumeService volumeService;
    @Autowired
    protected NetworkService networkService;
    @Autowired
    protected VmMapper vmMapper;

    @Autowired
    protected StorageMapper storageMapper;

    @Autowired
    protected HostService hostService;

    @Autowired
    protected ClusterService clusterService;

    @Autowired
    protected AgentService agentService;

    @Autowired
    protected AllocateService allocateService;

    @Autowired
    protected OsCategoryService osCategoryService;
    @Autowired
    protected CalculationSchemeService calculationSchemeService;

    private VmNetworkInfo allocateNetwork(int networkId, int vmId) {
        NetworkInfo networkInfo = networkService.findNetworkById(networkId);
        return this.allocateNetwork(networkInfo, vmId);
    }

    protected abstract VmNetworkInfo allocateNetwork(NetworkInfo network, int vmId);

    protected abstract void onBeforeStart(VmEntity vm, HostEntity host);

    protected abstract void onAfterStart(VmEntity vm, HostEntity host);

    protected abstract void onStop(VmEntity vm);

    protected abstract void onDestroy(VmEntity vm);

    public VmEntity createVm(String description,
                             int calculationSchemeId,
                             int clusterId,
                             int storageId,
                             int templateId,
                             long diskSize,
                             int networkId,
                             String instanceType,
                             int groupId) {

        TemplateInfo template = templateService.findTemplateById(templateId);
        List<TemplateRefInfo> templateRefList = templateService.listTemplateRefByTemplateId(template.getId());
        if (templateRefList.isEmpty()) {
            throw new CodeException(ErrorCode.TEMPLATE_NOT_READY, "模版未就绪");
        }
        TemplateRefInfo templateRef = templateRefList.stream().findAny().get();

        StorageInfo templateStorage = storageService.findStorageById(templateRef.getStorageId());

        String parentVolumePath = null;
        if (!template.getType().equals(TemplateType.ISO)) {
            parentVolumePath = "/mnt/" + templateStorage.getTarget() + "/" + templateRef.getTarget();
        }
        VmEntity vmEntity = VmEntity.builder()
                .clusterId(clusterId)
                .vmType(instanceType)
                .vmStatus(VmStatus.CREATING)
                .hostId(0)
                .vmName("")
                .vmIp("")
                .vmDescription(description)
                .calculationSchemeId(calculationSchemeId)
                .vncPort(0)
                .vncPassword("")
                .templateId(templateId)
                .osCategoryId(template.getOsCategoryId())
                .vmIso(template.getType().equals(TemplateType.ISO) ? templateId : 0)
                .groupId(groupId)
                .lastUpdateTime(new Date())
                .createTime(new Date())
                .build();
        vmMapper.insert(vmEntity);
        final String parentVolPath = parentVolumePath;
        try {
            VolumeInfo volumeInfo = this.volumeService.createVolume(clusterId, parentVolPath, storageId, "ROOT-" + vmEntity.getId(), diskSize);
            this.volumeService.attachVm(volumeInfo.getId(), vmEntity.getId());
            VmNetworkInfo network = this.allocateNetwork(networkId, vmEntity.getId());
            vmEntity.setVmIp(network.getIp());
            vmEntity.setVmName("VM" + "-" + vmEntity.getClusterId() + "-" + vmEntity.getId());
            vmEntity.setVmStatus(VmStatus.STOPPED);
            vmMapper.updateById(vmEntity);
            return vmEntity;
        } catch (CodeException err) {
            vmEntity.setVmStatus(VmStatus.ERROR);
            vmMapper.updateById(vmEntity);
            throw err;
        } catch (Exception err) {
            vmEntity.setVmStatus(VmStatus.ERROR);
            vmMapper.updateById(vmEntity);
            throw new CodeException(ErrorCode.SERVER_ERROR, err.getMessage());
        }
    }

    public void initDeviceInfo(VmEntity vm, VmModel kvm, CalculationSchemeInfo calculationSchemeInfo) {
        OsCategoryInfo categoryInfo = this.osCategoryService.findOsCategoryById(vm.getOsCategoryId());
        List<VolumeInfo> volumes = this.volumeService.listVolumeByVmId(vm.getId());
        if (volumes.isEmpty()) {
            throw new CodeException(ErrorCode.VOLUME_NOT_FOUND, "磁盘信息丢失");
        }
        List<VmNetworkInfo> networks = this.networkService.findVmNetworkByVmId(vm.getId());
        if (networks.isEmpty()) {
            throw new CodeException(ErrorCode.NETWORK_NOT_FOUND, "网络信息丢失");
        }
        if (vm.getVmIso() > 0) {
            TemplateInfo template = templateService.findTemplateById(vm.getVmIso());
            TemplateRefInfo templateRef = templateService.listTemplateRefByTemplateId(template.getId()).stream().findAny().get();
            StorageInfo templateStorage = storageService.findStorageById(templateRef.getStorageId());
            String path = "/mnt/" + templateStorage.getTarget() + "/" + templateRef.getTarget();
            kvm.setCdRoom(path);
        }
        List<VmModel.Disk> disks = new ArrayList<>();
        kvm.setDisks(disks);
        for (VolumeInfo volume : volumes) {
            if (!volume.getStatus().equals(VolumeStatus.READY)) {
                throw new CodeException(ErrorCode.VOLUME_NOT_READY, "磁盘未就绪");
            }
            StorageInfo volumeStorage = storageService.findStorageById(volume.getStorageId());
            if (!volumeStorage.getStatus().equals(StorageStatus.READY)) {
                throw new CodeException(ErrorCode.STORAGE_NOT_READY, "存储未就绪");
            }
            String path = "/mnt/" + volumeStorage.getTarget() + "/" + volume.getTarget();
            if (volume.getDevice() == 0) {
                kvm.setRoot(VmModel.RootDisk.builder().driver(categoryInfo.getDiskDriver()).path(path).build());
            } else {
                disks.add(VmModel.Disk.builder().path(path).device(volume.getDevice()).build());
            }
        }
        List<VmModel.Network> kvmNetworks = new ArrayList<>();
        kvm.setNetwroks(kvmNetworks);
        for (int i = 0; i < networks.size(); i++) {
            VmNetworkInfo instanceNetwork = networks.get(i);
            NetworkInfo network = this.networkService.findNetworkById(instanceNetwork.getNetworkId());
            if (!network.getStatus().equals(NetworkStatus.READY)) {
                throw new CodeException(ErrorCode.NETWORK_NOT_READY, "网络未就绪");
            }
            kvmNetworks.add(VmModel.Network.builder().mac(instanceNetwork.getMac()).source(network.getCard()).driver(categoryInfo.getNetworkDriver()).device(instanceNetwork.getDevice()).build());
        }
    }

    public VmEntity startVm(int id, int hostId) {
        VmEntity vm = vmMapper.selectById(id);
        if (vm == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机不存在");
        }
        if (vm.getVmStatus().equals(VmStatus.STOPPED)) {
            this.vmMapper.updateLastActiveTime(id, new Date());
            CalculationSchemeInfo calculationSchemeInfo = calculationSchemeService.findCalculationSchemeById(vm.getCalculationSchemeId());
            HostEntity hostInfo = this.allocateService.allocateHost(vm.getClusterId(), hostId, calculationSchemeInfo.getCpu(), calculationSchemeInfo.getMemory());
            vm.setHostId(hostInfo.getId());
            vm.setVncPassword(RandomStringUtils.randomAlphanumeric(16));
            this.onBeforeStart(vm, hostInfo);
            VmModel kvm = new VmModel();
            kvm.setId(vm.getId());
            kvm.setDescription(vm.getVmDescription());
            kvm.setName(vm.getVmName());
            kvm.setCpu(VmModel.Cpu.builder().cpu(calculationSchemeInfo.getCpu()).speed(calculationSchemeInfo.getSpeed()).build());
            kvm.setMemory(VmModel.Memory.builder().memory(calculationSchemeInfo.getMemory()).build());
            kvm.setPassword(vm.getVncPassword());
            this.initDeviceInfo(vm, kvm, calculationSchemeInfo);
            ResultUtil<VmInfoModel> resultUtil = this.agentService.startVm(hostInfo.getHostUri(), kvm);
            if (resultUtil.getCode() != ErrorCode.SUCCESS) {
                throw new CodeException(resultUtil.getCode(), resultUtil.getMessage());
            }
            try {
                VmInfoModel response = resultUtil.getData();
                vm.setVncPort(response.getVnc());

                this.onAfterStart(vm, hostInfo);
            } catch (Exception err) {
                this.agentService.stopVm(hostInfo.getHostUri(), kvm.getName());
                throw err;
            }
            vm.setHostId(hostInfo.getId());
            vm.setVmStatus(VmStatus.RUNNING);
            vm.setLastUpdateTime(new Date());
            vmMapper.updateById(vm);

        }
        return vm;
    }

    public VmEntity stopVm(int id, boolean force) {
        VmEntity vm = vmMapper.selectById(id);
        if (vm == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机不存在");
        }
        if (vm.getVmStatus().equals(VmStatus.RUNNING)) {

            HostInfo host = this.hostService.findHostById(vm.getHostId());
            ResultUtil<Void> resultUtil;
            if (host == null) {
                resultUtil = ResultUtil.<Void>builder().build();
            } else {
                if (force) {
                    resultUtil = this.agentService.destroyVm(host.getUri(), vm.getVmName());
                } else {
                    resultUtil = this.agentService.stopVm(host.getUri(), vm.getVmName());
                }
            }
            switch (resultUtil.getCode()) {
                case ErrorCode.SUCCESS:
                case ErrorCode.AGENT_VM_NOT_FOUND:
                    vm.setVmStatus(VmStatus.STOPPED);
                    vm.setHostId(0);
                    vm.setVncPort(0);
                    vm.setLastUpdateTime(new Date());
                    vmMapper.updateById(vm);
                    this.onStop(vm);
                    break;
                default:
                    throw new CodeException(resultUtil.getCode(), resultUtil.getMessage());
            }
        }
        return vm;
    }

    public VmEntity rebootVm(int id, boolean force) {
        VmEntity vm = vmMapper.selectById(id);
        if (vm == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机不存在");
        }
        int hostId = vm.getHostId();
        if (force) {
            stopVm(vm.getId(), true);
            vm = startVm(vm.getId(), hostId);
        } else {
            HostEntity host = this.allocateService.allocateHost(vm.getClusterId(), vm.getHostId(), 0, 0);
            ResultUtil<Void> resultUtil = this.agentService.rebootVm(host.getHostUri(), vm.getVmName());
            if (ErrorCode.SUCCESS != resultUtil.getCode()) {
                throw new CodeException(resultUtil.getCode(), resultUtil.getMessage());
            }
        }
        return vm;
    }

    public void destroyVm(int id) {
        VmEntity vm = vmMapper.selectById(id);
        if (vm == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机不存在");
        }
        stopVm(id, true);
        if (!vm.getVmType().equals(VMType.GUEST) || vm.getVmStatus().equals(VmStatus.ERROR)) {
            //
            this.networkService.unBindVmNetworkByVmId(id);
            this.volumeService.destroyByVmId(id);
            this.vmMapper.deleteById(id);
        } else {
            vm.setHostId(0);
            vm.setVncPort(0);
            vm.setVmStatus(VmStatus.DESTROY);
            vm.setRemoveTime(new Date());
            vmMapper.updateById(vm);
        }
        this.onDestroy(vm);
    }

    protected VmInfo initVmInfo(VmEntity vm) {
        VmInfo info = new VmInfo();
        info.setId(vm.getId());
        info.setClusterId(vm.getClusterId());
        info.setHostId(vm.getHostId());
        info.setCalculationSchemeId(vm.getCalculationSchemeId());
        info.setName(vm.getVmName());
        info.setIso(vm.getVmIso());
        info.setDescription(vm.getVmDescription());
        info.setIp(vm.getVmIp());
        info.setType(vm.getVmType());
        info.setStatus(vm.getVmStatus());
        info.setVncPort(vm.getVncPort());
        info.setCreateTime(vm.getCreateTime());
        info.setTemplateId(vm.getTemplateId());
        info.setGroupId(vm.getGroupId());
        return info;
    }

    @Override
    public VmInfo create(String description, int calculationSchemeId, int clusterId, int storageId, int hostId, int templateId, long diskSize, int network, int groupId) {

        log.info("开始创建主机 :description={}", description);
        VmEntity vm = this.createVm(description,
                calculationSchemeId,
                clusterId,
                storageId,
                templateId, diskSize, network, this.getType(), groupId);
        log.info("成功创建主机 :description={}", vm);
        return this.initVmInfo(vm);

    }

    @Override
    public VmInfo start(int id, int hostId) {

        VmEntity vm = vmMapper.selectById(id);
        if (vm == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机不存在");
        }
        log.info("开始启动主机 :id={} host={}", id, hostId);
        vm = this.startVm(id, hostId);
        log.info("成功启动主机 :id={} host={}", id, vm.getHostId());
        return this.initVmInfo(vm);

    }

    @Override
    public VmInfo stop(int id, boolean force) {

        log.info("开始停止主机 :id={}", id);
        VmEntity vm = this.stopVm(id, force);
        log.info("成功停止主机 :id={}", id);
        return this.initVmInfo(vm);

    }

    @Override
    public VmInfo reboot(int id, boolean force) {

        log.info("开始重启主机 :id={}", id);
        VmEntity vm = this.rebootVm(id, force);
        log.info("成功重启主机 :id={}", id);
        return this.initVmInfo(vm);

    }

    @Override
    public void destroy(int id) {

        log.info("开始销毁主机 :id={}", id);
        this.destroyVm(id);
        log.info("成功销毁主机 :id={}", id);


    }


}
