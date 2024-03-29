package cn.chenjun.cloud.common.bean;

import cn.chenjun.cloud.common.util.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 基础网络信息
 *
 * @author chenjun
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BasicBridgeNetwork {
    private String poolId;
    /**
     * 桥接名称
     */
    private String bridge;
    /**
     * 桥接方式
     */
    private  Constant.NetworkBridgeType bridgeType;

}
