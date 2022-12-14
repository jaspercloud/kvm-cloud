package cn.roamblue.cloud.management.operate.bean;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RebootGuestOperate extends BaseOperateParam {
    private int guestId;
}
