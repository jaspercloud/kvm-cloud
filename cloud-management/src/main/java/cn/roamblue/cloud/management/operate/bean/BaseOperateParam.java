package cn.roamblue.cloud.management.operate.bean;

import cn.roamblue.cloud.common.gson.GsonBuilderUtil;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * @author chenjun
 */
@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseOperateParam {
    private String taskId;
    private String title;

    @Override
    public String toString() {
        return GsonBuilderUtil.create().toJson(this);
    }
}
