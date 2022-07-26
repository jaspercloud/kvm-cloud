package cn.roamblue.cloud.management.ui.impl;

import cn.roamblue.cloud.common.bean.ResultUtil;
import cn.roamblue.cloud.common.util.ErrorCode;
import cn.roamblue.cloud.management.annotation.PreAuthority;
import cn.roamblue.cloud.management.bean.GroupInfo;
import cn.roamblue.cloud.management.service.GroupService;
import cn.roamblue.cloud.management.ui.GroupUiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author chenjun
 */
@Service
public class GroupUiServiceImpl extends AbstractUiService implements GroupUiService {
    @Autowired
    private GroupService groupService;

    @Override
    public ResultUtil<List<GroupInfo>> listGroup() {
        return super.call(groupService::listGroup);
    }

    @Override
    @PreAuthority(value = "hasAuthority('group.create')")
    public ResultUtil<GroupInfo> createGroup(String name) {
        if (StringUtils.isEmpty(name)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "名称不能为空");
        }
        return super.call(() -> groupService.createGroup(name));
    }

    @Override
    @PreAuthority(value = "hasAuthority('group.modify')")
    public ResultUtil<GroupInfo> modifyGroup(int id, String name) {

        if (StringUtils.isEmpty(name)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "名称不能为空");
        }
        return super.call(() -> groupService.modifyGroup(id, name));
    }

    @Override
    @PreAuthority(value = "hasAuthority('group.destroy')")
    public ResultUtil<Void> destroyGroupById(int id) {
        return super.call(() -> groupService.destroyGroupById(id));
    }
}
