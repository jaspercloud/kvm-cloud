package cn.roamblue.cloud.management.v2.operate.impl;

import cn.roamblue.cloud.common.bean.BasicBridgeNetwork;
import cn.roamblue.cloud.common.bean.ResultUtil;
import cn.roamblue.cloud.common.bean.VlanNetwork;
import cn.roamblue.cloud.common.util.Constant;
import cn.roamblue.cloud.common.util.ErrorCode;
import cn.roamblue.cloud.management.util.GsonBuilderUtil;
import cn.roamblue.cloud.management.util.SpringContextUtils;
import cn.roamblue.cloud.management.v2.data.entity.HostEntity;
import cn.roamblue.cloud.management.v2.data.entity.NetworkEntity;
import cn.roamblue.cloud.management.v2.data.mapper.HostMapper;
import cn.roamblue.cloud.management.v2.data.mapper.NetworkMapper;
import cn.roamblue.cloud.management.v2.operate.OperateFactory;
import cn.roamblue.cloud.management.v2.operate.bean.CreateNetworkOperate;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * @author chenjun
 */
public class CreateNetworkOperateImpl extends AbstractOperate<CreateNetworkOperate, ResultUtil<Void>> {

    protected CreateNetworkOperateImpl() {
        super(CreateNetworkOperate.class);
    }

    @Override
    public void operate(CreateNetworkOperate param) {
        NetworkMapper networkMapper = SpringContextUtils.getBean(NetworkMapper.class);
        HostMapper hostMapper = SpringContextUtils.getBean(HostMapper.class);
        NetworkEntity network = networkMapper.selectById(param.getId());
        List<HostEntity> hosts = hostMapper.selectList(new QueryWrapper<HostEntity>().eq("cluster_id", network.getClusterId()));
        ResultUtil<Void> resultUtil=null;
        for (HostEntity host : hosts) {
            if (Objects.equals(cn.roamblue.cloud.management.v2.util.Constant.HostStatus.READY, host.getStatus())) {
                if (Objects.equals(cn.roamblue.cloud.management.v2.util.Constant.NetworkType.BASIC, network.getType())) {
                    BasicBridgeNetwork basicBridgeNetwork = BasicBridgeNetwork.builder()
                            .bridge(network.getBridge())
                            .ip(host.getIp())
                            .geteway(network.getGateway())
                            .nic(host.getNic())
                            .netmask(network.getMask()).build();
                    resultUtil=this.call(host, param, Constant.Command.NETWORK_CREATE_BASIC, basicBridgeNetwork);
                } else {

                    NetworkEntity basicNetworkEntity = networkMapper.selectById(network.getParentId());

                    BasicBridgeNetwork basicBridgeNetwork = BasicBridgeNetwork.builder()
                            .bridge(basicNetworkEntity.getBridge())
                            .ip(host.getIp())
                            .geteway(basicNetworkEntity.getGateway())
                            .nic(host.getNic())
                            .netmask(basicNetworkEntity.getMask()).build();
                    VlanNetwork vlan = VlanNetwork.builder()
                            .vlanId(network.getVlanId())
                            .netmask(network.getMask())
                            .basic(basicBridgeNetwork)
                            .ip(null)
                            .bridge(network.getBridge())
                            .geteway(network.getGateway())
                            .build();
                    resultUtil=this.call(host, param, Constant.Command.NETWORK_CREATE_VLAN, vlan);
                }
                if(resultUtil.getCode()!= ErrorCode.SUCCESS){
                    break;
                }
            }
        }
        OperateFactory.onCallback(param,"", GsonBuilderUtil.create().toJson(resultUtil));
    }

    @Override
    public Type getCallResultType() {
        return new TypeToken<ResultUtil<Void>>() {
        }.getType();
    }

    @Override
    public void onCallback(String hostId, CreateNetworkOperate param, ResultUtil<Void> resultUtil) {
        NetworkMapper networkMapper = SpringContextUtils.getBean(NetworkMapper.class);
        NetworkEntity network = networkMapper.selectById(param.getId());
        if(resultUtil.getCode()==ErrorCode.SUCCESS){
            network.setStatus(cn.roamblue.cloud.management.v2.util.Constant.NetworkStatus.READY);
        }else{
            network.setStatus(cn.roamblue.cloud.management.v2.util.Constant.NetworkStatus.ERROR);
        }
        networkMapper.updateById(network);
    }
}
