package cn.roamblue.cloud.agent.service.impl;

import cn.roamblue.cloud.agent.service.KvmVolumeService;
import cn.roamblue.cloud.common.agent.VolumeModel;
import lombok.extern.slf4j.Slf4j;
import org.anarres.qemu.image.QEmuImage;
import org.libvirt.StoragePool;
import org.libvirt.StorageVol;
import org.libvirt.StorageVolInfo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chenjun
 */
@Slf4j
@Service
public class KvmVolumeServiceImpl extends AbstractKvmService implements KvmVolumeService {
    @Override
    public List<VolumeModel> listVolume(String storageName) {
        return super.execute(connect -> {
            StoragePool storagePool = connect.storagePoolLookupByName(storageName);
            storagePool.refresh(0);
            String[] volumes = storagePool.listVolumes();
            List<VolumeModel> list = new ArrayList<>();
            for (String volume : volumes) {
                list.add(getVolume(storageName, volume));
            }
            return list;
        });
    }

    @Override
    public VolumeModel getVolume(String storageName, String volumeName) {
        return super.execute(connect -> {
            StoragePool storagePool = connect.storagePoolLookupByName(storageName);
            try {
                StorageVol storageVol = storagePool.storageVolLookupByName(volumeName);

                StorageVolInfo storageVolInfo = storageVol.getInfo();

                String type = null;
                if (storageVolInfo != null && storageVolInfo.type != null) {
                    type = storageVolInfo.type.toString();
                }
                return VolumeModel.builder().storage(storageName)
                        .name(volumeName)
                        .type(type)
                        .path(storageVol.getPath())
                        .capacity(storageVolInfo.capacity)
                        .allocation(storageVolInfo.allocation)
                        .build();
            } catch (Exception e) {
                storagePool.refresh(0);
                throw e;
            }
        });
    }

    @Override
    public VolumeModel reSize(String storageName, String volumeName, long size) {

        return super.execute(connect -> {
            StoragePool storagePool = connect.storagePoolLookupByName(storageName);
            StorageVol storageVol = storagePool.storageVolLookupByName(volumeName);
            storageVol.resize(size, 0);
            StorageVolInfo storageVolInfo = storageVol.getInfo();
            return VolumeModel.builder().storage(storageName)
                    .name(volumeName)
                    .type(storageVolInfo.type.toString())
                    .path(storageVol.getPath())
                    .capacity(storageVolInfo.capacity)
                    .allocation(storageVolInfo.allocation)
                    .build();
        });
    }

    @Override
    public void destroyVolume(String storage, String volume) {
        try {
            super.execute(connect -> {
                StoragePool storagePool = connect.storagePoolLookupByName(storage);
                StorageVol storageVol = storagePool.storageVolLookupByName(volume);
                storageVol.wipe();
                storageVol.delete(0);
                return null;
            });
            log.info("destroy volume.storage={} volume={}", storage, volume);
        } catch (Exception err) {
            log.error("destroy volume fail.storage={} volume={}", storage, volume, err);
        }
    }

    @Override
    public VolumeModel createVolume(String storage, String volume, String path, long capacityGb, String backingVolume) {

        return super.execute(connect -> {
            StringBuilder sb = new StringBuilder();
            sb.append("<volume type='file'>")
                    .append("<name>").append(volume).append("</name>")
                    .append("<source></source>");
            if (capacityGb > 0) {
                sb.append("<capacity unit='GiB'>").append(capacityGb).append("</capacity>");
                sb.append("<allocation>0</allocation>");
            }
            sb.append("<target>")
                    .append("<path>").append(path).append("</path>")
                    .append("<format type='qcow2'/>")
                    .append("<permissions>")
                    .append("<mode>0600</mode>")
                    .append("<owner>0</owner>")
                    .append("<group>0</group>")
                    .append("</permissions>")
                    .append("</target>");
            if (!StringUtils.isEmpty(backingVolume)) {
                sb.append("<backingStore>")
                        .append("<path>").append(backingVolume).append("</path>")
                        .append("<format type='qcow2'/>")
                        .append("<permissions>")
                        .append("<mode>0600</mode>")
                        .append("<owner>107</owner>")
                        .append("<group>107</group>")
                        .append("</permissions>")
                        .append("</backingStore>");
            }
            sb.append("</volume>");
            StoragePool storagePool = connect.storagePoolLookupByName(storage);
            StorageVol storageVol = storagePool.storageVolCreateXML(sb.toString(), 0);
            StorageVolInfo storageVolInfo = storageVol.getInfo();
            storagePool.refresh(0);
            log.info("create volume.storage={} volume={} xml={}", storage, volume, sb.toString());
            return VolumeModel.builder().storage(storage)
                    .name(volume)
                    .path(storageVol.getPath())
                    .type(storageVolInfo.type.toString())
                    .capacity(storageVolInfo.capacity)
                    .allocation(storageVolInfo.allocation)
                    .build();
        });
    }

    @Override
    public VolumeModel cloneVolume(String sourceStorage, String sourceVolume, String targetStorage, String targetVolume, String path) {
        return super.execute(connect -> {
            StoragePool sourceStoragePool = connect.storagePoolLookupByName(sourceStorage);
            StoragePool targetStoragePool = connect.storagePoolLookupByName(targetStorage);
            StorageVol sourceVol = sourceStoragePool.storageVolLookupByName(sourceVolume);
            StringBuilder sb = new StringBuilder();
            sb.append("<volume type='file'>")
                    .append("<name>").append(targetVolume).append("</name>")
                    .append("<source></source>")
                    .append("<target>")
                    .append("<path>").append(path).append("</path>")
                    .append("<format type='qcow2'/>")
                    .append("<permissions>")
                    .append("<mode>0600</mode>")
                    .append("<owner>107</owner>")
                    .append("<group>107</group>")
                    .append("</permissions>")
                    .append("</target>")
                    .append("</volume>");

            StorageVol targetVol = targetStoragePool.storageVolCreateXMLFrom(sb.toString(), sourceVol, 0);
            StorageVolInfo storageVolInfo = targetVol.getInfo();
            return VolumeModel.builder().storage(targetStorage)
                    .name(targetVolume)
                    .type(storageVolInfo.type.toString())
                    .path(targetVol.getPath())
                    .capacity(storageVolInfo.capacity)
                    .allocation(storageVolInfo.allocation)
                    .build();
        });
    }

}
