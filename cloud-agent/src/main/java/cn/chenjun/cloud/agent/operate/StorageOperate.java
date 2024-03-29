package cn.chenjun.cloud.agent.operate;

import cn.chenjun.cloud.common.bean.StorageCreateRequest;
import cn.chenjun.cloud.common.bean.StorageDestroyRequest;
import cn.chenjun.cloud.common.bean.StorageInfo;
import cn.chenjun.cloud.common.bean.StorageInfoRequest;
import org.libvirt.Connect;

import java.util.List;

/**
 * @author chenjun
 */
public interface StorageOperate {
    /**
     * 获取存储池信息
     *
     * @param connect
     * @param request
     * @return
     * @throws Exception
     */
    StorageInfo getStorageInfo(Connect connect, StorageInfoRequest request) throws Exception;

    /**
     * 批量获取存储池信息
     *
     * @param connect
     * @param batchRequest
     * @return
     * @throws Exception
     */
    List<StorageInfo> batchStorageInfo(Connect connect, List<StorageInfoRequest> batchRequest) throws Exception;

    /**
     * 初始化存储池信息
     *
     * @param connect
     * @param request
     * @return
     * @throws Exception
     */
    StorageInfo create(Connect connect, StorageCreateRequest request) throws Exception;

    /**
     * 销毁存储池
     *
     * @param connect
     * @param request
     * @throws Exception
     */
    Void destroy(Connect connect, StorageDestroyRequest request) throws Exception;
}
