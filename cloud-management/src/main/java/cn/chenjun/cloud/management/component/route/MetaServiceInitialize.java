package cn.chenjun.cloud.management.component.route;

import cn.chenjun.cloud.common.bean.GuestQmaRequest;
import cn.chenjun.cloud.common.gson.GsonBuilderUtil;
import cn.chenjun.cloud.management.config.ApplicationConfig;
import cn.chenjun.cloud.management.data.entity.ComponentEntity;
import cn.chenjun.cloud.management.data.entity.NetworkEntity;
import cn.chenjun.cloud.management.data.mapper.NetworkMapper;
import cn.hutool.core.io.resource.ResourceUtil;
import com.hubspot.jinjava.Jinjava;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author chenjun
 */
@Component
public class MetaServiceInitialize implements RouteComponentQmaInitialize {
    @Autowired
    private ApplicationConfig applicationConfig;
    @Autowired
    private NetworkMapper networkMapper;

    @Override
    public List<GuestQmaRequest.QmaBody> initialize(ComponentEntity component, int guestId) {
        NetworkEntity network = networkMapper.selectById(component.getNetworkId());
        List<GuestQmaRequest.QmaBody> commands = new ArrayList<>();
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.EXECUTE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.Execute.builder().command("mkdir").args(new String[]{"-p", "/usr/local/meta-service/"}).checkSuccess(true).build())).build());
        String metaServiceShell = new String(Base64.getDecoder().decode(ResourceUtil.readUtf8Str("tpl/meta/meta_shell.tpl").getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        String metaService = new String(Base64.getDecoder().decode(ResourceUtil.readUtf8Str("tpl/meta/meta_service.tpl").getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        String metaPython = new String(Base64.getDecoder().decode(ResourceUtil.readUtf8Str("tpl/meta/meta_py.tpl").getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        Map<String, Object> map = new HashMap<>(2);
        map.put("managerUri", this.applicationConfig.getManagerUri());
        map.put("secret", network.getSecret());
        Jinjava jinjava = new Jinjava();
        metaPython = jinjava.render(metaPython, map);
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.WRITE_FILE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.WriteFile.builder().fileName("/usr/local/meta-service/meta.py").fileBody(metaPython).build())).build());
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.WRITE_FILE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.WriteFile.builder().fileName("/usr/local/meta-service/service.sh").fileBody(metaServiceShell).build())).build());
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.WRITE_FILE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.WriteFile.builder().fileName("/usr/lib/systemd/system/meta-service.service").fileBody(metaService).build())).build());
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.EXECUTE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.Execute.builder().command("chmod").args(new String[]{"a+x", "/usr/local/meta-service/service.sh"}).build())).build());
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.EXECUTE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.Execute.builder().command("systemctl").args(new String[]{"daemon-reload"}).checkSuccess(true).build())).build());
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.EXECUTE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.Execute.builder().command("systemctl").args(new String[]{"enable", "meta-service"}).build())).build());
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.EXECUTE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.Execute.builder().command("systemctl").args(new String[]{"restart", "meta-service"}).build())).build());
        return commands;
    }

    @Override
    public int getOrder() {
        return RouteOrder.META;
    }
}
