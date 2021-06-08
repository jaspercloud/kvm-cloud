package com.roamblue.cloud.management.ui.impl;

import cn.hutool.core.lang.UUID;
import com.roamblue.cloud.common.bean.ResultUtil;
import com.roamblue.cloud.common.util.ErrorCode;
import com.roamblue.cloud.management.annotation.Rule;
import com.roamblue.cloud.management.bean.LoginSignatureInfo;
import com.roamblue.cloud.management.bean.LoginUserInfo;
import com.roamblue.cloud.management.bean.LoginUserTokenInfo;
import com.roamblue.cloud.management.bean.UserInfo;
import com.roamblue.cloud.management.service.UserService;
import com.roamblue.cloud.management.ui.UserUiService;
import com.roamblue.cloud.management.util.RuleType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author chenjun
 */
@Service
public class UserUiServiceImpl extends AbstractUiService implements UserUiService {
    @Autowired
    private UserService userService;

    @Override
    public ResultUtil<LoginUserTokenInfo> login(String loginName, String password, String nonce) {
        if (StringUtils.isEmpty(loginName)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        if (StringUtils.isEmpty(password)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        return super.call(() -> userService.login(loginName, password, nonce));

    }

    @Override
    public ResultUtil<LoginUserTokenInfo> updatePassword(Integer userId, String oldPassword, String newPassword, String nonce) {
        if (StringUtils.isEmpty(oldPassword)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "旧密码不能为空");
        }

        if (StringUtils.isEmpty(newPassword)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }
        return super.call(() -> userService.updatePassword(userId, oldPassword, newPassword, nonce));
    }

    @Override
    public ResultUtil<LoginUserTokenInfo> refreshToken(Integer userId) {
        return super.call(() -> userService.refreshToken(userId));
    }

    @Override
    public ResultUtil<LoginSignatureInfo> getSignature(String loginName) {
        if (StringUtils.isEmpty(loginName)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        return super.call(() -> {
            LoginUserInfo loginInfoBean = userService.findUserByLoginName(loginName);
            return LoginSignatureInfo.builder().signature(loginInfoBean == null ? UUID.randomUUID().toString() : loginInfoBean.getPasswordSalt()).nonce(String.valueOf(System.currentTimeMillis())).build();
        });
    }

    @Override
    public ResultUtil<LoginSignatureInfo> getLoginSignature(Integer userId) {
        return super.call(() -> {
            LoginUserInfo loginInfoBean = userService.findUserById(userId);
            return LoginSignatureInfo.builder().signature(loginInfoBean == null ? UUID.randomUUID().toString() : loginInfoBean.getPasswordSalt()).nonce(String.valueOf(System.currentTimeMillis())).build();
        });
    }

    @Override
    @Rule(min = RuleType.SUPER_ADMIN)
    public ResultUtil<UserInfo> register(String loginName, String password, int rule) {
        if (StringUtils.isEmpty(loginName)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }
        if (StringUtils.isEmpty(password)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        return super.call(() -> userService.register(loginName, password, rule));

    }

    @Override
    @Rule(min = RuleType.SUPER_ADMIN)
    public ResultUtil<UserInfo> updateUserState(int currentUserId, int userId, short state) {
        if (currentUserId == userId) {
            return ResultUtil.error(ErrorCode.PERMISSION_ERROR, "不能更改自己状态");
        }
        return super.call(() -> userService.updateUserState(userId, state));
    }

    @Override
    @Rule(min = RuleType.SUPER_ADMIN)
    public ResultUtil<UserInfo> updateUserRule(int currentUserId, int userId, int rule) {
        if (currentUserId == userId) {
            return ResultUtil.error(ErrorCode.PERMISSION_ERROR, "不能更改自己权限");
        }
        return super.call(() -> userService.updateUserRule(userId, rule));
    }

    @Override
    @Rule(min = RuleType.SUPER_ADMIN)
    public ResultUtil<Void> destroyUser(int currentUserId, int userId) {
        if (currentUserId == userId) {
            return ResultUtil.error(ErrorCode.PERMISSION_ERROR, "不能删除自己账号");
        }
        return super.call(() -> userService.destroyUser(userId));
    }


    @Override
    @Rule(min = RuleType.SUPER_ADMIN)
    public ResultUtil<UserInfo> resetPassword(int currentUserId, int userId, String password) {
        if (StringUtils.isEmpty(password)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "密码不能为空");
        }
        return super.call(() -> userService.resetPassword(userId, password));
    }

    @Override
    @Rule(min = RuleType.SUPER_ADMIN)
    public ResultUtil<List<UserInfo>> listUsers() {
        return super.call(() -> userService.listUsers());
    }
}