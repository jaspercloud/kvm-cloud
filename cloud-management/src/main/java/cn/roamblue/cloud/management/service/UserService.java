package cn.roamblue.cloud.management.service;


import cn.roamblue.cloud.management.bean.LoginUser;
import cn.roamblue.cloud.management.bean.LoginUserInfo;
import cn.roamblue.cloud.management.bean.LoginUserTokenInfo;
import cn.roamblue.cloud.management.bean.UserInfo;

import java.util.Collection;
import java.util.List;

/**
 * @author chenjun
 */
public interface UserService {
    /**
     * 登陆
     *
     * @param loginName 用户名
     * @param password  密码
     * @param nonce     nonce
     * @return
     */
    LoginUserTokenInfo login(String loginName, String password, String nonce);

    /**
     * Oauth2协议登录
     * @param id
     * @param authorities
     * @return
     */
    LoginUserTokenInfo loginOauth2(Object id, Collection<String> authorities);
    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return
     */
    LoginUserInfo findUserById(int userId);

    /**
     * 根据登陆名获取用户信息
     *
     * @param loginName 登录名
     * @return
     */
    LoginUserInfo findUserByLoginName(String loginName);

    /**
     * 根据token获取用户ID
     *
     * @param token token
     * @return
     */
    LoginUser getUserIdByToken(String token);


    /**
     * 更新密码
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @param nonce       noce
     * @return
     */
    LoginUserTokenInfo updatePassword(Integer userId, String oldPassword, String newPassword, String nonce);

    /**
     * 刷新token
     *
     * @param user
     * @return
     */
    LoginUserTokenInfo refreshToken(LoginUser user);

    /**
     * 注册用户
     *
     * @param loginName
     * @param password
     * @param rule
     * @return
     */
    UserInfo register(String loginName, String password, int rule);

    /**
     * 更新用户状态
     *
     * @param userId
     * @param state
     * @return
     */
    UserInfo updateUserState(int userId, short state);

    /**
     * 更新用户权限
     *
     * @param userId
     * @param rule
     * @return
     */
    UserInfo updateUserRule(int userId, int rule);

    /**
     * 获取用户列表
     *
     * @return
     */
    List<UserInfo> listUsers();

    /**
     * 重置密码
     *
     * @param userId
     * @param password
     * @return
     */
    UserInfo resetPassword(int userId, String password);

    /**
     * 销毁用户
     *
     * @param userId
     */
    void destroyUser(int userId);
}
