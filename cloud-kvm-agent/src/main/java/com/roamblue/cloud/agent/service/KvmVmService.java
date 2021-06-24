package com.roamblue.cloud.agent.service;

import com.roamblue.cloud.common.agent.VmInfoModel;
import com.roamblue.cloud.common.agent.VmModel;
import com.roamblue.cloud.common.agent.VmStaticsModel;

import java.util.List;

/**
 * @author chenjun
 */
public interface KvmVmService {
    /**
     * 获取VM列表
     *
     * @return
     */
    List<VmInfoModel> listVm();

    /**
     * 获取VM监控指标
     *
     * @return
     */
    List<VmStaticsModel> listVmStatics();

    /**
     * 根据名称获取VM
     *
     * @param name
     * @return
     */
    VmInfoModel findByName(String name);

    /**
     * 重启VM
     *
     * @param name
     */
    void restart(String name);

    /**
     * 销毁VM
     *
     * @param name
     */
    void destroy(String name);

    /**
     * 停止VM
     *
     * @param name
     */
    void stop(String name);

    /**
     * 附加设备
     *
     * @param name
     * @param xml
     */
    void attachDevice(String name, String xml);

    /**
     * 取消附加设备
     *
     * @param name
     * @param xml
     */
    void detachDevice(String name, String xml);

    /**
     * 启动
     *
     * @param info
     * @return
     */
    VmInfoModel start(VmModel info);

    /**
     * 更新设备
     *
     * @param name
     * @param xml
     */
    void updateDevice(String name, String xml);
}
