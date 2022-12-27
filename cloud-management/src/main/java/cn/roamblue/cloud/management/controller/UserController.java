package cn.roamblue.cloud.management.controller;

import cn.hutool.core.util.NumberUtil;
import cn.roamblue.cloud.common.bean.ResultUtil;
import cn.roamblue.cloud.common.util.ErrorCode;
import cn.roamblue.cloud.management.annotation.Login;
import cn.roamblue.cloud.management.annotation.NoLogin;
import cn.roamblue.cloud.management.model.LoginSignatureModel;
import cn.roamblue.cloud.management.model.LoginUserModel;
import cn.roamblue.cloud.management.model.TokenModel;
import cn.roamblue.cloud.management.model.UserInfoModel;
import cn.roamblue.cloud.management.servcie.UserService;
import cn.roamblue.cloud.management.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理
 *
 * @author chenjun
 */
@Login
@RestController
@ResponseBody
public class UserController {
    @Autowired
    private UserService userUiService;
    @NoLogin
    @PostMapping("/api/user/login")
    public ResultUtil<TokenModel> login(@RequestParam("loginName") String loginName, @RequestParam("password") String password, @RequestParam("nonce") String nonce) {

        return userUiService.login(loginName, password, nonce);

    }

    @PostMapping("/api/user/password/modify")
    public ResultUtil<TokenModel> updatePassword(@RequestAttribute(Constant.HttpHeaderNames.LOGIN_USER_INFO_ATTRIBUTE) LoginUserModel model, @RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, @RequestParam("nonce") String nonce) {
        if(!Constant.UserType.LOCAL.equals(model.getType())){
            return ResultUtil.error(ErrorCode.SERVER_ERROR,"Oauth2方式不支持修改密码");
        }
        return userUiService.updatePassword(NumberUtil.parseInt(model.getId().toString()), oldPassword, newPassword, nonce);
    }

    @PostMapping("/api/user/token/refresh")
    public ResultUtil<TokenModel> refreshToken(@RequestAttribute(Constant.HttpHeaderNames.LOGIN_USER_INFO_ATTRIBUTE) LoginUserModel loginUser) {
        return userUiService.refreshToken(loginUser);
    }

    @NoLogin
    @GetMapping("/api/user/login/signature")
    public ResultUtil<LoginSignatureModel> getSignature(@RequestParam("loginName") String loginName) {
        return userUiService.getSignature(loginName);
    }
    @GetMapping("/api/user/signature")
    public ResultUtil<LoginSignatureModel> getLoginSignature(@RequestAttribute(Constant.HttpHeaderNames.LOGIN_USER_INFO_ATTRIBUTE) LoginUserModel model) {
        if(!Constant.UserType.LOCAL.equals(model.getType())){
            return ResultUtil.error(ErrorCode.SERVER_ERROR,"Oauth2方式不支持获取签名");
        }
        return userUiService.getLoginSignature(NumberUtil.parseInt(model.getId().toString()));
    }


    @PutMapping("/api/user/register")
    public ResultUtil<UserInfoModel> register(@RequestParam("loginName") String loginName, @RequestParam("password") String password) {

        return userUiService.register(loginName, password);

    }


    @PostMapping("/api/user/state/update")
    @Login
    public ResultUtil<UserInfoModel> updateUserState(@RequestParam("userId") int userId, @RequestParam("state") short state) {

        return userUiService.updateUserState(userId, state);
    }


    @DeleteMapping("/api/user/destroy")
    public ResultUtil<Void> destroyUser(@RequestParam("userId") int userId) {

        return userUiService.destroyUser(userId);
    }

    @PostMapping("/api/user/password/reset")
    @Login
    public ResultUtil<UserInfoModel> resetPassword(@RequestParam("userId") int userId, @RequestParam("password") String password) {
        return userUiService.resetPassword(userId, password);
    }

    @GetMapping("/api/user/list")
    public ResultUtil<List<UserInfoModel>> listUsers() {
        return userUiService.listUsers();
    }
}
