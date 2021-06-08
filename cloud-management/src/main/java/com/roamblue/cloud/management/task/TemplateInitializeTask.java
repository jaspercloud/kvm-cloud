package com.roamblue.cloud.management.task;

import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.roamblue.cloud.common.bean.ResultUtil;
import com.roamblue.cloud.common.util.ErrorCode;
import com.roamblue.cloud.management.data.entity.HostEntity;
import com.roamblue.cloud.management.data.entity.StorageEntity;
import com.roamblue.cloud.management.data.entity.TemplateEntity;
import com.roamblue.cloud.management.data.entity.TemplateRefEntity;
import com.roamblue.cloud.management.data.mapper.HostMapper;
import com.roamblue.cloud.management.data.mapper.StorageMapper;
import com.roamblue.cloud.management.data.mapper.TemplateMapper;
import com.roamblue.cloud.management.data.mapper.TemplateRefMapper;
import com.roamblue.cloud.management.service.LockService;
import com.roamblue.cloud.management.util.LockKeyUtil;
import com.roamblue.cloud.management.util.TemplateStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TemplateInitializeTask extends AbstractTask {

    @Autowired
    private TemplateMapper templateMapper;
    @Autowired
    private TemplateRefMapper templateRefMapper;

    @Autowired
    private StorageMapper storageMapper;
    @Autowired
    private HostMapper hostMapper;

    @Autowired
    private LockService lockService;

    @Override
    protected int getInterval() {
        return 10000;
    }

    @Override
    protected String getName() {
        return "TemplateInitializeTask";
    }

    @Override
    protected void call() {
        List<TemplateEntity> list = templateMapper.selectAll();
        for (TemplateEntity template : list) {
            if (!template.getTemplateUri().startsWith("http")) {
                continue;
            }
            if (templateRefMapper.selectCount(new QueryWrapper<TemplateRefEntity>().eq("template_id", template.getId())) == 0) {
                lockService.tryRun(LockKeyUtil.getTemplateLockKey(template.getId()), () -> {
                    download(template);
                    return null;
                }, 1, TimeUnit.HOURS);

            }
        }
    }

    private void download(TemplateEntity template) {
        try {
            Optional<HostEntity> hostOptional = hostMapper.selectList(new QueryWrapper<HostEntity>().eq("cluster_id", template.getClusterId())).stream().findAny();
            if (!hostOptional.isPresent()) {
                return;
            }
            Optional<StorageEntity> storageOptional = storageMapper.selectList(new QueryWrapper<StorageEntity>().eq("cluster_id", template.getClusterId())).stream().findAny();
            if (!storageOptional.isPresent()) {
                return;
            }
            HostEntity hostEntity = hostOptional.get();
            StorageEntity storageEntity = storageOptional.get();
            TemplateRefEntity templateRefEntity = TemplateRefEntity.builder()
                    .templateId(template.getId())
                    .clusterId(template.getClusterId())
                    .storageId(storageEntity.getId())
                    .templateTarget(UUID.randomUUID().toString().replace("-", ""))
                    .createTime(new Date())
                    .build();
            Map<String, Object> map = new HashMap<>();
            map.put("path", "/mnt/" + storageEntity.getStorageTarget() + "/" + templateRefEntity.getTemplateTarget());
            map.put("uri", template.getTemplateUri());
            log.info("开始下载模版信息.template={} path", template, map.get("path"));
            Gson gson = new Gson();
            ResultUtil<Long> downloadResult = gson.fromJson(HttpUtil.post(hostEntity.getHostUri() + "/download/template", map), new TypeToken<ResultUtil<Long>>() {
            }.getType());
            if (downloadResult.getCode() == ErrorCode.SUCCESS) {
                template.setTemplateSize(downloadResult.getData());
                template.setTemplateStatus(TemplateStatus.READY);
                templateRefEntity.setTemplateStatus(TemplateStatus.READY);
                templateMapper.updateById(template);
                templateRefMapper.insert(templateRefEntity);
                log.info("成功下载模版.template={}", template);
            } else {
                log.error("下载模版失败.template={}.msg={}", template, downloadResult.getMessage());
            }
        } catch (Exception err) {
            log.error("下载模版失败.template={}", template, err);
        }
    }
}