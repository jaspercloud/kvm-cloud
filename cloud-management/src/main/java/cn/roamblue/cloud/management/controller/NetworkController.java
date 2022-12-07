package cn.roamblue.cloud.management.controller;

import cn.roamblue.cloud.common.bean.ResultUtil;
import cn.roamblue.cloud.management.model.NetworkModel;
import cn.roamblue.cloud.management.servcie.NetworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NetworkController {
    @Autowired
    private NetworkService networkService;

    @GetMapping("/api/network/info")
    public ResultUtil<NetworkModel> getNetworkInfo(@RequestParam("networkId") int networkId) {
        return networkService.getNetworkInfo(networkId);
    }

    @GetMapping("/api/network/all")
    public ResultUtil<List<NetworkModel>> listNetwork() {
        return networkService.listNetwork();
    }

    @PutMapping("/api/network/create")
    public ResultUtil<NetworkModel> createNetwork(@RequestParam("name") String name,
                                                  @RequestParam("startIp") String startIp,
                                                  @RequestParam("endIp") String endIp,
                                                  @RequestParam("gateway") String gateway,
                                                  @RequestParam("mask") String mask,
                                                  @RequestParam("bridge") String bridge,
                                                  @RequestParam("dns") String dns,
                                                  @RequestParam("type") int type,
                                                  @RequestParam("vlanId") int vlanId,
                                                  @RequestParam("basicNetworkId") int basicNetworkId) {
        return networkService.createNetwork(name, startIp, endIp, gateway, mask, bridge, dns, type, vlanId, basicNetworkId);
    }

    @DeleteMapping("/api/network/destroy")
    public ResultUtil<NetworkModel> destroyNetwork(@RequestParam("networkId") int networkId) {
        return networkService.destroyNetwork(networkId);
    }
}
