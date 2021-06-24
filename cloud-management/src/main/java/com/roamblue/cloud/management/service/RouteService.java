package com.roamblue.cloud.management.service;

/**
 * @author chenjun
 */
public interface RouteService extends VmService {
    /**
     * 启动Route，负责分发dhcp
     *
     * @param clusterId
     */
    void start(int clusterId);
}
