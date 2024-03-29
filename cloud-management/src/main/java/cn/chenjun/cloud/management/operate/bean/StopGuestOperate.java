package cn.chenjun.cloud.management.operate.bean;

import cn.chenjun.cloud.management.util.Constant;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author chenjun
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StopGuestOperate extends BaseOperateParam {
    private int guestId;
    private boolean force;
    private boolean destroy;

    @Override
    public int getType() {
        return Constant.OperateType.STOP_GUEST;
    }
}
