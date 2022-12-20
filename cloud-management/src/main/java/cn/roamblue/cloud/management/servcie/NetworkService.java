package cn.roamblue.cloud.management.servcie;

import cn.roamblue.cloud.common.bean.ResultUtil;
import cn.roamblue.cloud.common.error.CodeException;
import cn.roamblue.cloud.common.util.ErrorCode;
import cn.roamblue.cloud.management.annotation.Lock;
import cn.roamblue.cloud.management.data.entity.GuestNetworkEntity;
import cn.roamblue.cloud.management.data.entity.NetworkEntity;
import cn.roamblue.cloud.management.model.GuestNetworkModel;
import cn.roamblue.cloud.management.model.NetworkModel;
import cn.roamblue.cloud.management.operate.bean.BaseOperateParam;
import cn.roamblue.cloud.management.operate.bean.CreateNetworkOperate;
import cn.roamblue.cloud.management.operate.bean.DestroyNetworkOperate;
import cn.roamblue.cloud.management.util.Constant;
import cn.roamblue.cloud.management.util.IpCaculate;
import cn.roamblue.cloud.management.util.RedisKeyUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NetworkService extends AbstractService {

    @Lock(value = RedisKeyUtil.GLOBAL_LOCK_KEY, write = false)
    public ResultUtil<List<GuestNetworkModel>> listGuestNetworks(int guestId) {
        List<GuestNetworkEntity> networkList = guestNetworkMapper.selectList(new QueryWrapper<GuestNetworkEntity>().eq("guest_id", guestId));
        networkList.sort(Comparator.comparingInt(GuestNetworkEntity::getDeviceId));
        List<GuestNetworkModel> models = networkList.stream().map(this::initGuestNetwork).collect(Collectors.toList());
        return ResultUtil.success(models);
    }

    @Lock(value = RedisKeyUtil.GLOBAL_LOCK_KEY, write = false)
    public ResultUtil<NetworkModel> getNetworkInfo(int networkId) {
        NetworkEntity network = this.networkMapper.selectById(networkId);
        if (network == null) {
            throw new CodeException(ErrorCode.NETWORK_NOT_FOUND, "网络不存在");
        }
        return ResultUtil.success(this.initGuestNetwork(network));
    }

    @Lock(value = RedisKeyUtil.GLOBAL_LOCK_KEY, write = false)
    public ResultUtil<List<NetworkModel>> listNetwork() {
        List<NetworkEntity> networkList = this.networkMapper.selectList(new QueryWrapper<>());
        List<NetworkModel> models = networkList.stream().map(this::initGuestNetwork).collect(Collectors.toList());
        return ResultUtil.success(models);
    }

    @Lock(RedisKeyUtil.GLOBAL_LOCK_KEY)
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<NetworkModel> createNetwork(String name, String startIp, String endIp, String gateway, String mask, String bridge, String dns, int type, int vlanId, int basicNetworkId) {
        NetworkEntity network = NetworkEntity.builder()
                .name(name)
                .startIp(startIp)
                .endIp(endIp)
                .gateway(gateway)
                .mask(mask)
                .bridge(bridge)
                .dns(dns)
                .type(type)
                .vlanId(vlanId)
                .basicNetworkId(basicNetworkId)
                .status(Constant.NetworkStatus.CREATING).build();
        networkMapper.insert(network);
        List<String> ips = IpCaculate.parseIpRange(startIp, endIp);
        for (String ip : ips) {
            GuestNetworkEntity guestNetwork = GuestNetworkEntity.builder()
                    .guestId(0)
                    .ip(ip)
                    .networkId(network.getNetworkId())
                    .mac(IpCaculate.getMacAddrWithFormat(":"))
                    .driveType("")
                    .deviceId(0)
                    .build();
            this.guestNetworkMapper.insert(guestNetwork);
        }

        BaseOperateParam operateParam = CreateNetworkOperate.builder().taskId(UUID.randomUUID().toString()).title("创建网络[" + network.getName() + "]").networkId(network.getNetworkId()).build();
        this.operateTask.addTask(operateParam);
        return ResultUtil.success(this.initGuestNetwork(network));
    }
    @Lock(RedisKeyUtil.GLOBAL_LOCK_KEY)
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<NetworkModel> registerNetwork(int networkId) {
        NetworkEntity network = this.networkMapper.selectById(networkId);
        if (network == null) {
            throw new CodeException(ErrorCode.NETWORK_NOT_FOUND, "网络不存在");
        }
        network.setStatus(Constant.NetworkStatus.CREATING);
        this.networkMapper.updateById(network);
        BaseOperateParam operateParam = CreateNetworkOperate.builder().taskId(UUID.randomUUID().toString()).title("注册网络[" + network.getName() + "]").networkId(network.getNetworkId()).build();
        this.operateTask.addTask(operateParam);
        return ResultUtil.success(this.initGuestNetwork(network));
    }

    @Lock(RedisKeyUtil.GLOBAL_LOCK_KEY)
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<NetworkModel> maintenanceNetwork(int networkId) {
        NetworkEntity network = this.networkMapper.selectById(networkId);
        if (network == null) {
            throw new CodeException(ErrorCode.NETWORK_NOT_FOUND, "网络不存在");
        }
        network.setStatus(Constant.NetworkStatus.MAINTENANCE);
        this.networkMapper.updateById(network);
        return ResultUtil.success(this.initGuestNetwork(network));
    }

    @Lock(RedisKeyUtil.GLOBAL_LOCK_KEY)
    @Transactional(rollbackFor = Exception.class)
    public ResultUtil<NetworkModel> destroyNetwork(int networkId) {
        NetworkEntity network = this.networkMapper.selectById(networkId);
        if (network == null) {
            throw new CodeException(ErrorCode.NETWORK_NOT_FOUND, "网络不存在");
        }

        if (guestNetworkMapper.selectCount(new QueryWrapper<GuestNetworkEntity>().eq("network_id", networkId).ne("guest_id", 0)) > 0) {
            throw new CodeException(ErrorCode.SERVER_ERROR, "当前网络被其他虚拟机引用，请首先删除虚拟机");
        }
        network.setStatus(Constant.NetworkStatus.DESTROY);
        networkMapper.updateById(network);
        this.guestNetworkMapper.delete(new QueryWrapper<GuestNetworkEntity>().eq("network_id", networkId));
        BaseOperateParam operateParam = DestroyNetworkOperate.builder().taskId(UUID.randomUUID().toString()).title("销毁网络[" + network.getName() + "]").networkId(networkId).build();
        this.operateTask.addTask(operateParam);
        return ResultUtil.success(this.initGuestNetwork(network));
    }
}
