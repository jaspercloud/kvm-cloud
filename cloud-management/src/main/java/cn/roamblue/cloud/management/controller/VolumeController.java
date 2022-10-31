package cn.roamblue.cloud.management.controller;

import cn.roamblue.cloud.common.bean.ResultUtil;
import cn.roamblue.cloud.management.bean.VolumeInfo;
import cn.roamblue.cloud.management.bean.VolumeSnapshot;
import cn.roamblue.cloud.management.ui.VolumeUiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 磁盘管理
 *
 * @author chenjun
 */
@RestController
public class VolumeController {
    @Autowired
    private VolumeUiService volumeUiService;

    /**
     * 磁盘列表
     *
     * @return
     */
    @GetMapping("/management/volume")
    public ResultUtil<List<VolumeInfo>> listVolume() {
        return volumeUiService.listVolume();
    }

    /**
     * 搜索磁盘列表
     *
     * @param clusterId 集群ID
     * @param vmId      VM ID
     * @param storageId 存储池ID
     * @return
     */
    @GetMapping("/management/volume/search")
    public ResultUtil<List<VolumeInfo>> search(@RequestParam("clusterId") int clusterId,
                                               @RequestParam("vmId") int vmId,
                                               @RequestParam("storageId") int storageId) {
        return volumeUiService.search(clusterId, storageId, vmId);
    }

    /**
     * 获取磁盘信息
     *
     * @param id 磁盘ID
     * @return
     */
    @PostMapping("/management/volume/info")
    public ResultUtil<VolumeInfo> findVolumeById(@RequestParam("id") int id) {
        return volumeUiService.findVolumeById(id);
    }

    /**
     * 创建磁盘
     *
     * @param clusterId 集群ID
     * @param storageId 存储池
     * @param name      磁盘名称
     * @param size      磁盘大小GB
     * @return
     */
    @PostMapping("/management/volume/create")
    public ResultUtil<VolumeInfo> createVolume(
            @RequestParam("clusterId") int clusterId,
            @RequestParam("storageId") int storageId,
            @RequestParam("name") String name,
            @RequestParam("size") long size) {
        return volumeUiService.createVolume(clusterId, storageId, name, size);
    }

    /**
     * 销毁磁盘
     *
     * @param id
     * @return
     */
    @PostMapping("/management/volume/destroy")
    public ResultUtil<VolumeInfo> destroyVolumeById(@RequestParam("id") int id) {
        return volumeUiService.destroyVolumeById(id);
    }

    /**
     * 恢复磁盘
     *
     * @param id 磁盘ID
     * @return
     */
    @PostMapping("/management/volume/resume")
    public ResultUtil<VolumeInfo> resume(@RequestParam("id") int id) {
        return volumeUiService.resume(id);
    }

    /**
     * 扩容磁盘
     *
     * @param id   磁盘ID
     * @param size 磁盘大小
     * @return
     */
    @PostMapping("/management/volume/resize")
    public ResultUtil<VolumeInfo> resize(@RequestParam("id") int id, @RequestParam("size") long size) {
        return volumeUiService.resize(id, size);
    }


    /**
     * 获取磁盘快照列表
     *
     * @param id
     * @return
     */
    @GetMapping("/management/volume/snapshot")
    public ResultUtil<List<VolumeSnapshot>> listVolumeSnapshot(@RequestParam("id") int id) {
        return volumeUiService.listVolumeSnapshot(id);
    }

    /**
     * 创建磁盘快照
     *
     * @param id
     * @return
     */
    @PostMapping("/management/volume/snapshot/create")
    public ResultUtil<VolumeSnapshot> createVolumeSnapshot(@RequestParam("id") int id) {
        return volumeUiService.createVolumeSnapshot(id);
    }

    /**
     * 恢复磁盘快照
     *
     * @param id
     * @param name
     * @return
     */
    @PostMapping("/management/volume/snapshot/revert")
    public ResultUtil<Void> revertVolumeSnapshot(@RequestParam("id") int id, @RequestParam("name") String name) {
        return volumeUiService.revertVolumeSnapshot(id, name);
    }

    /**
     * 删除磁盘快照
     *
     * @param id
     * @param name
     * @return
     */
    @PostMapping("/management/volume/snapshot/delete")
    public ResultUtil<Void> deleteVolumeSnapshot(@RequestParam("id") int id, @RequestParam("name") String name) {
        return volumeUiService.deleteVolumeSnapshot(id, name);
    }
}
