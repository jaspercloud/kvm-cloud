package cn.chenjun.cloud.management.component.route;

import cn.chenjun.cloud.common.bean.GuestQmaRequest;
import cn.chenjun.cloud.common.gson.GsonBuilderUtil;
import cn.chenjun.cloud.management.data.entity.ComponentEntity;
import cn.chenjun.cloud.management.data.entity.GuestEntity;
import cn.chenjun.cloud.management.data.mapper.GuestMapper;
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
public class NginxInitialize implements RouteComponentQmaInitialize {
    @Autowired
    private GuestMapper guestMapper;
    @Override
    public List<GuestQmaRequest.QmaBody> initialize(ComponentEntity component, int guestId) {
        GuestEntity guest = this.guestMapper.selectById(guestId);
        List<GuestQmaRequest.QmaBody> commands = new ArrayList<>();
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.EXECUTE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.Execute.builder().command("yum").args(new String[]{"install", "-y", "nginx"}).checkSuccess(true).build())).build());
        String nginxConfig = new String(Base64.getDecoder().decode(ResourceUtil.readUtf8Str("tpl/nginx.tpl").getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        Map<String, Object> map = new HashMap<>(0);
        map.put("ip", guest.getGuestIp());
        Jinjava jinjava = new Jinjava();
        nginxConfig = jinjava.render(nginxConfig, map);
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.WRITE_FILE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.WriteFile.builder().fileName("/etc/nginx/nginx.conf").fileBody(nginxConfig).build())).build());
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.EXECUTE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.Execute.builder().command("systemctl").args(new String[]{"enable", "nginx"}).checkSuccess(true).build())).build());
        commands.add(GuestQmaRequest.QmaBody.builder().command(GuestQmaRequest.QmaType.EXECUTE).data(GsonBuilderUtil.create().toJson(GuestQmaRequest.Execute.builder().command("systemctl").args(new String[]{"restart", "nginx"}).checkSuccess(true).build())).build());
        return commands;
    }

    @Override
    public int getOrder() {
        return RouteOrder.NGINX;
    }
}
