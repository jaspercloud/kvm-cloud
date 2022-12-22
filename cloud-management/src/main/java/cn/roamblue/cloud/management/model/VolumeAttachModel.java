package cn.roamblue.cloud.management.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolumeAttachModel {
    private int guestDiskId;
    private int guestId;
    private String description;
    private int deviceId;
}
