package cn.chenjun.cloud.management.controller;

import cn.chenjun.cloud.common.bean.ResultUtil;
import cn.chenjun.cloud.management.annotation.LoginRequire;
import cn.chenjun.cloud.management.model.NetworkModel;
import cn.chenjun.cloud.management.servcie.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author chenjun
 */
@LoginRequire
@RestController
public class NetworkController extends BaseController {
    @Autowired
    private NetworkService networkService;

    @GetMapping("/api/network/info")
    public ResultUtil<NetworkModel> getNetworkInfo(@RequestParam("networkId") int networkId) {
        return this.lockRun(() -> networkService.getNetworkInfo(networkId));
    }

    @GetMapping("/api/network/all")
    public ResultUtil<List<NetworkModel>> listNetwork() {
        return this.lockRun(() -> networkService.listNetwork());
    }

    @PutMapping("/api/network/create")
    public ResultUtil<NetworkModel> createNetwork(@RequestParam("name") String name,
                                                  @RequestParam("startIp") String startIp,
                                                  @RequestParam("endIp") String endIp,
                                                  @RequestParam("gateway") String gateway,
                                                  @RequestParam("mask") String mask,
                                                  @RequestParam("bridge") String bridge,
                                                  @RequestParam("subnet") String subnet,
                                                  @RequestParam("broadcast") String broadcast,
                                                  @RequestParam("dns") String dns,
                                                  @RequestParam("domain") String domain,
                                                  @RequestParam("type") int type,
                                                  @RequestParam("vlanId") int vlanId,
                                                  @RequestParam("basicNetworkId") int basicNetworkId) {
        return this.lockRun(() -> networkService.createNetwork(name, startIp, endIp, gateway, mask, subnet, broadcast, bridge, dns, domain, type, vlanId, basicNetworkId));
    }

    @PostMapping("/api/network/register")
    public ResultUtil<NetworkModel> registerNetwork(@RequestParam("networkId") int networkId) {
        return this.lockRun(() -> networkService.registerNetwork(networkId));
    }

    @PostMapping("/api/network/maintenance")
    public ResultUtil<NetworkModel> maintenanceNetwork(@RequestParam("networkId") int networkId) {
        return this.lockRun(() -> networkService.maintenanceNetwork(networkId));
    }

    @DeleteMapping("/api/network/destroy")
    public ResultUtil<NetworkModel> destroyNetwork(@RequestParam("networkId") int networkId) {
        return this.lockRun(() -> networkService.destroyNetwork(networkId));
    }
}
