package cn.chenjun.cloud.agent.operate;

import cn.chenjun.cloud.common.bean.BasicBridgeNetwork;
import cn.chenjun.cloud.common.bean.VlanNetwork;
import org.libvirt.Connect;

/**
 * @author chenjun
 */
public interface NetworkOperate {
    /**
     * 创建基础网络
     *
     * @param connect
     * @param request
     * @throws Exception
     */
    Void createBasic(Connect connect, BasicBridgeNetwork request) throws Exception;

    /**
     * 创建Vlan网络
     *
     * @param connect
     * @param request
     * @throws Exception
     */
    Void createVlan(Connect connect, VlanNetwork request) throws Exception;

    /**
     * 删除基础网络信息
     *
     * @param connect
     * @param request
     * @throws Exception
     */
    Void destroyBasic(Connect connect, BasicBridgeNetwork request) throws Exception;

    /**
     * 删除Vlan网络信息
     *
     * @param connect
     * @param request
     * @throws Exception
     */
    Void destroyVlan(Connect connect, VlanNetwork request) throws Exception;
}
