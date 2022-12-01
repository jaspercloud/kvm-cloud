package cn.roamblue.cloud.agent.operate.impl;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.roamblue.cloud.agent.operate.OsOperate;
import cn.roamblue.cloud.agent.util.XmlUtil;
import cn.roamblue.cloud.common.agent.VmInfoModel;
import cn.roamblue.cloud.common.bean.*;
import cn.roamblue.cloud.common.error.CodeException;
import cn.roamblue.cloud.common.util.Constant;
import cn.roamblue.cloud.common.util.ErrorCode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.dom4j.DocumentException;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.LibvirtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class OsOperateImpl implements OsOperate {
    private final int MAX_DEVICE_COUNT = 5;
    private final int MIN_DISK_DEVICE_ID = MAX_DEVICE_COUNT;
    private final int MIN_NIC_DEVICE_ID = MIN_DISK_DEVICE_ID + MAX_DEVICE_COUNT;

    @Override
   public VmInfoModel getGustInfo(Connect connect,GuestInfoRequest request) throws Exception {
       Domain domain = this.findDomainByName(connect, request.getName());
       if (domain != null) {
           return this.initVmResponse(domain);
       }else{
           throw new CodeException(ErrorCode.VM_NOT_FOUND,"虚拟机没有运行:"+request.getName());
       }
   }

   @Override
   public List<VmInfoModel> batchGustInfo(Connect connect,List<GuestInfoRequest> batchRequest) throws Exception{
       Set<String> names= batchRequest.stream().map(GuestInfoRequest::getName).collect(Collectors.toSet());
       Map<String,VmInfoModel> map=new HashMap<>();
       List<VmInfoModel> list=new ArrayList<>();
       int[] ids = connect.listDomains();
       for (int id : ids) {
           Domain domain = connect.domainLookupByID(id);
           if (names.contains(domain.getName())) {
               map.put(domain.getName(), initVmResponse(domain));
           }
       }
       String[] namesOfDefinedDomain = connect.listDefinedDomains();
       for (String stopDomain : namesOfDefinedDomain) {
           Domain domain = connect.domainLookupByName(stopDomain);
           if (names.contains(domain.getName())) {
               map.put(domain.getName(), initVmResponse(domain));
           }
       }
       for (GuestInfoRequest request : batchRequest) {
           list.add(map.get(request.getName()));
       }
       return list;
   }

    @Override
    public void shutdown(Connect connect, GuestShutdownRequest request) throws Exception {
        while (true) {
            Domain domain = this.findDomainByName(connect, request.getName());
            if (domain == null) {
                break;
            }
            switch (domain.getInfo().state) {
                case VIR_DOMAIN_SHUTDOWN:
                case VIR_DOMAIN_SHUTOFF:
                    domain.destroy();
                default:
                    ThreadUtil.sleep(1, TimeUnit.SECONDS);
                    break;
            }
        }

    }

    @Override
    public void reboot(Connect connect, GuestRebootRequest request) throws Exception {
        Domain domain = this.findDomainByName(connect, request.getName());
        if (domain == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机没有运行:" + request.getName());
        }
        switch (domain.getInfo().state) {
            case VIR_DOMAIN_SHUTDOWN:
            case VIR_DOMAIN_SHUTOFF:
                domain.create();
                break;
            default:
                domain.reboot(1);
                break;
        }
    }

    @Override
    public VmInfoModel start(Connect connect, GuestStartRequest request) throws Exception {
        Domain domain = this.findDomainByName(connect, request.getName());
        if (domain != null) {
            if (domain.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
                return this.initVmResponse(domain);
            }
            domain.destroy();
        }
        String cpuXml = "";
        String cdRoomXml = "";
        String diskXml = "";
        String nicXml = "";
        if (request.getOsCpu().getShare() > 0) {
            String xml = ResourceUtil.readUtf8Str("xml/cpu/cputune.xml");
            cpuXml += String.format(xml, request.getOsCpu().getShare()) + "\r\n";
        }
        if (request.getOsCpu().getCore() > 0) {
            String xml = ResourceUtil.readUtf8Str("xml/cpu/cpu.xml");
            cpuXml += String.format(xml, request.getOsCpu().getSocket(), request.getOsCpu().getCore(), request.getOsCpu().getThread()) + "\r\n";
        }
        if (request.getOsCdRoom() != null) {
            OsCdRoom cd = request.getOsCdRoom();
            if (StringUtils.isEmpty(cd.getPath())) {
                String xml = ResourceUtil.readUtf8Str("xml/cd/DetachCdRoom.xml");
                cdRoomXml += xml + "\r\n";
            } else {
                String xml = ResourceUtil.readUtf8Str("xml/cd/AttachCdRoom.xml");
                cdRoomXml += String.format(xml, cd.getPath()) + "\r\n";
            }
        }
        for (int i = 0; i < request.getOsDisks().size(); i++) {
            OsDisk osDisk = request.getOsDisks().get(i);
            String xml = getDiskXml(osDisk, i == 0 ? request.getBus() : Constant.DiskBus.VIRTIO);
            int deviceId = osDisk.getDeviceId() + MIN_DISK_DEVICE_ID;

            String dev = "" + (char) ('a' + deviceId);
            xml = String.format(xml, dev, osDisk.getVolumeType(), osDisk.getVolume(), deviceId, deviceId);
            diskXml += xml + "\r\n";
        }
        for (OsNic osNic : request.getNetworkInterfaces()) {
            String xml = ResourceUtil.readUtf8Str("xml/network/Nic.xml");
            int deviceId = osNic.getDeviceId() + MIN_NIC_DEVICE_ID;
            xml = String.format(xml, osNic.getMac(), osNic.getDriveType(), osNic.getBridgeName(), deviceId);
            nicXml += xml + "\r\n";
        }
        String xml = ResourceUtil.readUtf8Str("xml/Domain.xml");
        xml = String.format(xml,
                request.getName(),
                request.getDescription(),
                request.getOsMemory().getMemory(),
                request.getOsCpu().getNumber(),
                cpuXml,
                request.getEmulator(),
                cdRoomXml,
                diskXml,
                nicXml,
                request.getName(),
                request.getVncPassword());
        domain=connect.domainCreateXML(xml, 0);
        return this.initVmResponse(domain);
    }

    @Override
    public void detachCdRoom(Connect connect, OsCdRoom request) throws Exception {
        Domain domain = connect.domainLookupByName(request.getName());
        if (domain == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机没有运行:" + request.getName());
        }
        String xml = ResourceUtil.readUtf8Str("xml/cd/DetachCdRoom.xml");
        domain.updateDeviceFlags(xml, 1);
    }

    @Override
    public void attachCdRoom(Connect connect, OsCdRoom request) throws Exception {
        Domain domain = connect.domainLookupByName(request.getName());
        if (domain == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机没有运行:" + request.getName());
        }
        String xml = ResourceUtil.readUtf8Str("xml/cd/AttachCdRoom.xml");
        xml = String.format(xml, request.getPath());
        domain.updateDeviceFlags(xml, 1);
    }

    @Override
    public void attachDisk(Connect connect, OsDisk request) throws Exception {
        Domain domain = connect.domainLookupByName(request.getName());
        if (domain == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机没有运行:" + request.getName());
        }
        if (request.getDeviceId() >= MAX_DEVICE_COUNT) {
            throw new CodeException(ErrorCode.SERVER_ERROR, "超过最大磁盘数量");
        }
        String xml = getDiskXml(request, Constant.DiskBus.VIRTIO);
        int deviceId = request.getDeviceId() + MIN_DISK_DEVICE_ID;

        String dev = "" + (char) ('a' + deviceId);
        xml = String.format(xml, dev, request.getVolumeType(), request.getVolume(), request.getDeviceId(), deviceId);
        domain.attachDevice(xml);
    }

    @Override
    public void detachDisk(Connect connect, OsDisk request) throws Exception {
        Domain domain = connect.domainLookupByName(request.getName());
        if (domain == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机没有运行:" + request.getName());
        }
        if (request.getDeviceId() >= MAX_DEVICE_COUNT) {
            throw new CodeException(ErrorCode.SERVER_ERROR, "超过最大磁盘数量");
        }
        String xml = getDiskXml(request, Constant.DiskBus.VIRTIO);
        int deviceId = request.getDeviceId() + MIN_DISK_DEVICE_ID;
        String dev = "" + (char) ('a' + deviceId);
        xml = String.format(xml, dev, request.getVolumeType(), request.getVolume(), request.getDeviceId(), deviceId);
        domain.detachDevice(xml);
    }

    @Override
    public void attachNic(Connect connect, OsNic request) throws Exception {
        Domain domain = connect.domainLookupByName(request.getName());
        if (domain == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机没有运行:" + request.getName());
        }
        if (request.getDeviceId() >= MAX_DEVICE_COUNT) {
            throw new CodeException(ErrorCode.SERVER_ERROR, "超过最大网卡数量");
        }
        String xml = ResourceUtil.readUtf8Str("xml/network/Nic.xml");
        int deviceId = request.getDeviceId() + MIN_NIC_DEVICE_ID;
        xml = String.format(xml, request.getMac(), request.getDriveType(), request.getBridgeName(), deviceId);
        domain.attachDevice(xml);
    }

    @Override
    public void detachNic(Connect connect, OsNic request) throws Exception {
        Domain domain = connect.domainLookupByName(request.getName());
        if (domain == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机没有运行:" + request.getName());
        }
        if (request.getDeviceId() >= MAX_DEVICE_COUNT) {
            throw new CodeException(ErrorCode.SERVER_ERROR, "超过最大网卡数量");
        }
        String xml = ResourceUtil.readUtf8Str("xml/network/Nic.xml");
        int deviceId = request.getDeviceId() + MIN_NIC_DEVICE_ID;
        xml = String.format(xml, request.getMac(), request.getDriveType(), request.getBridgeName(), deviceId);
        domain.detachDevice(xml);
    }

    @Override
    public void qma(Connect connect, GuestQmaRequest request) throws Exception {
        Domain domain = connect.domainLookupByName(request.getName());
        if (domain == null) {
            throw new CodeException(ErrorCode.VM_NOT_FOUND, "虚拟机没有运行:" + request.getName());
        }
        List<GuestQmaRequest.QmaBody> commands=request.getCommands();
        for (GuestQmaRequest.QmaBody command : commands) {
            switch (command.getCommand()){
                case GuestQmaRequest.QmaType.WRITE_FILE:
                    GuestQmaRequest.WriteFile writeFile=new Gson().fromJson(command.getData(), GuestQmaRequest.WriteFile.class);
                    int handler = qmaOpenFile(writeFile.getFileName(), domain);
                    qmaWriteFile(writeFile.getFileBody(), domain,handler);
                    qmaCloseFile(domain,handler);
                    break;
                case GuestQmaRequest.QmaType.EXECUTE:
                    GuestQmaRequest.Execute execute=new Gson().fromJson(command.getData(), GuestQmaRequest.Execute.class);

                    qmaExecuteShell(request, domain, command, execute);
                    break;
                default:
                    throw new CodeException(ErrorCode.VM_NOT_FOUND, "不支持的QMA操作:" + command.getCommand());
            }
        }
    }



    @Override
    public void destroy(Connect connect, GuestDestroyRequest request) throws Exception {
        Domain domain = this.findDomainByName(connect, request.getName());
        if (domain != null) {
            domain.destroy();
            domain.undefine();
        }
    }

    private Domain findDomainByName(Connect connect, String name) throws Exception {
        int[] ids = connect.listDomains();

        for (int id : ids) {
            Domain domain = connect.domainLookupByID(id);
            if (Objects.equals(domain.getName(), name)) {
                return domain;
            }
        }
        String[] namesOfDefinedDomain = connect.listDefinedDomains();
        for (String stopDomain : namesOfDefinedDomain) {
            Domain domain = connect.domainLookupByName(stopDomain);
            if (Objects.equals(domain.getName(), name)) {
                return domain;
            }
        }
        return null;
    }
    private static void qmaExecuteShell(GuestQmaRequest request, Domain domain, GuestQmaRequest.QmaBody command, GuestQmaRequest.Execute execute) throws LibvirtException {
        Gson gson = new Gson();
        Map<String, Object> map = new HashMap<>(2);
        map.put("execute", "guest-exec");
        Map<String, Object> arguments = new HashMap<>(3);
        map.put("arguments", arguments);
        map.put("path", execute.getCommand());
        map.put("arg", execute.getArgs());
        if(null== domain.qemuAgentCommand( gson.toJson(command), request.getTimeout(), 0)){
            throw new CodeException(ErrorCode.SERVER_ERROR,"执行命令失败");
        }
    }
    private void qmaCloseFile(Domain domain, int handler) throws LibvirtException {
        Map<String, Object> command = new HashMap<>(2);
        command.put("execute", "guest-file-close");
        Map<String, Object> arguments = new HashMap<>(2);
        arguments.put("handle", handler);
        command.put("arguments", arguments);
        String response = domain.qemuAgentCommand(new Gson().toJson(command), 10, 0);
        if (StringUtils.isEmpty(response)) {
            throw new CodeException(ErrorCode.VM_COMMAND_ERROR, "执行失败");
        }
    }

    private void qmaWriteFile(String body, Domain domain, int handler) throws LibvirtException {
        Map<String, Object> command = new HashMap<>(2);
        command.put("execute", "guest-file-write");
        Map<String, Object> arguments = new HashMap<>(2);
        arguments.put("handle", handler);
        arguments.put("buf-b64", cn.hutool.core.codec.Base64.encode(body.getBytes(StandardCharsets.UTF_8)));
        command.put("arguments", arguments);
        String response = domain.qemuAgentCommand(new Gson().toJson(command), 10, 0);
        if (StringUtils.isEmpty(response)) {
            throw new CodeException(ErrorCode.VM_COMMAND_ERROR, "执行失败");
        }
    }

    private int qmaOpenFile(String path, Domain domain) throws LibvirtException {
        int handler;
        Map<String, Object> command = new HashMap<>(2);
        command.put("execute", "guest-file-open");
        Map<String, Object> arguments = new HashMap<>(2);
        arguments.put("path", path);
        arguments.put("mode", "w+");
        command.put("arguments", arguments);
        String response = domain.qemuAgentCommand(new Gson().toJson(command), 10, 0);
        if (StringUtils.isEmpty(response)) {
            throw new CodeException(ErrorCode.VM_COMMAND_ERROR, "执行失败");
        }
        Map<String, Object> map = new Gson().fromJson(response, new TypeToken<Map<String, Object>>() {
        }.getType());
        handler = NumberUtil.parseInt(map.get("return").toString());
        return handler;
    }
    private static String getDiskXml(OsDisk request, String bus) {
        String xml;
        switch (bus) {
            case Constant.DiskBus.VIRTIO:
                xml = ResourceUtil.readUtf8Str("xml/disk/VirtioDisk.xml");
                break;
            case Constant.DiskBus.IDE:
                xml = ResourceUtil.readUtf8Str("xml/disk/IdeDisk.xml");
                break;
            case Constant.DiskBus.SCSI:
                xml = ResourceUtil.readUtf8Str("xml/disk/ScsiDisk.xml");
                break;
            default:
                throw new CodeException(ErrorCode.SERVER_ERROR, "未知的总线模式:" + bus);
        }
        return xml;
    }
    private VmInfoModel initVmResponse(Domain domain) throws LibvirtException, SAXException, DocumentException {
        DomainInfo domainInfo = domain.getInfo();
        VmInfoModel info = VmInfoModel.builder().name(domain.getName())
                .uuid(domain.getUUIDString())
                .state(domainInfo.state)
                .maxMem(domainInfo.maxMem)
                .memory(domainInfo.memory)
                .cpuTime(domainInfo.cpuTime)
                .cpu(domainInfo.nrVirtCpu)
                .build();
        if (domainInfo.state == DomainInfo.DomainState.VIR_DOMAIN_RUNNING) {
            String xml = domain.getXMLDesc(0);
            info.setVnc(XmlUtil.getVnc(xml));
            info.setPassword(XmlUtil.getVncPassword(xml));
        }
        return info;
    }
}
