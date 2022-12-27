package cn.roamblue.cloud.management.servcie;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.DigestUtil;
import cn.roamblue.cloud.common.bean.ResultUtil;
import cn.roamblue.cloud.common.error.CodeException;
import cn.roamblue.cloud.common.gson.GsonBuilderUtil;
import cn.roamblue.cloud.common.util.ErrorCode;
import cn.roamblue.cloud.management.config.ApplicationConfig;
import cn.roamblue.cloud.management.config.Oauth2Config;
import cn.roamblue.cloud.management.data.entity.UserInfoEntity;
import cn.roamblue.cloud.management.data.mapper.UserInfoMapper;
import cn.roamblue.cloud.management.model.LoginSignatureModel;
import cn.roamblue.cloud.management.model.LoginUserModel;
import cn.roamblue.cloud.management.model.TokenModel;
import cn.roamblue.cloud.management.model.UserInfoModel;
import cn.roamblue.cloud.management.util.Constant;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chenjun
 */
@Service
public class UserService extends AbstractService {
    private static final int HOUR = 1000 * 60 * 60;
    @Autowired
    private UserInfoMapper loginInfoMapper;

    @Autowired
    private Oauth2Config oauth2Config;
    @Autowired
    private ApplicationConfig applicaionconfig;


    public ResultUtil<TokenModel> login(String loginName, String password, String nonce) {

        UserInfoEntity loginInfoEntity = loginInfoMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("login_name", loginName));
        if (loginInfoEntity == null) {
            throw new CodeException(ErrorCode.USER_LOGIN_NAME_OR_PASSWORD_ERROR, "用户名或密码错误");
        }
        String realPassword = DigestUtil.sha256Hex(loginInfoEntity.getLoginPassword() + ":" + nonce);
        if (!realPassword.equals(password)) {
            throw new CodeException(ErrorCode.USER_LOGIN_NAME_OR_PASSWORD_ERROR, "用户名或密码错误");
        }
        if (loginInfoEntity.getLoginState() != Constant.UserState.ABLE) {
            throw new CodeException(ErrorCode.USER_FORBID_ERROR, "用户已禁用");
        }
        return ResultUtil.success(getToken(Constant.UserType.LOCAL, loginInfoEntity.getUserId()));
    }

    public ResultUtil<TokenModel> loginOauth2(Object id) {
        return ResultUtil.success(this.getToken(Constant.UserType.OAUTH2, id));

    }

    public ResultUtil<UserInfoModel> findUserById(int userId) {
        return ResultUtil.success(this.initLoginInfoBO(loginInfoMapper.selectById(userId)));
    }


    public ResultUtil<UserInfoModel> findUserByLoginName(String loginName) {
        UserInfoEntity user = loginInfoMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("login_name", loginName));
        return ResultUtil.success(this.initLoginInfoBO(user));
    }

    public ResultUtil<LoginUserModel> getUserIdByToken(String token) {
        if (StringUtils.isEmpty(token)) {
            throw new CodeException(ErrorCode.SERVER_ERROR, "token不能为空");
        }
        try {
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(this.applicaionconfig.getJwtPassword())).withIssuer(this.applicaionconfig.getJwtIssuer()).build();
            DecodedJWT jwt = jwtVerifier.verify(token);
            LoginUserModel loginUser = GsonBuilderUtil.create().fromJson(jwt.getClaim("User").asString(), LoginUserModel.class);
            if (this.oauth2Config.isEnable() && !Constant.UserType.OAUTH2.equals(loginUser.getType())) {
                throw new CodeException(ErrorCode.NO_LOGIN_ERROR, "Token过期");
            } else if (!this.oauth2Config.isEnable() && !Constant.UserType.LOCAL.equals(loginUser.getType())) {
                throw new CodeException(ErrorCode.NO_LOGIN_ERROR, "Token过期");
            }
            return ResultUtil.success(loginUser);
        } catch (Exception j) {
            throw new CodeException(ErrorCode.NO_LOGIN_ERROR, "Token过期");
        }
    }


    public ResultUtil<TokenModel> updatePassword(Integer userId, String oldPassword, String newPassword, String nonce) {

        UserInfoEntity loginInfoEntity = this.loginInfoMapper.selectById(userId);
        if (loginInfoEntity == null) {
            throw new CodeException(ErrorCode.NO_LOGIN_ERROR, "登陆用户不存在");
        }
        String realPassword = DigestUtil.sha256Hex(loginInfoEntity.getLoginPassword() + ":" + nonce);
        if (!realPassword.equalsIgnoreCase(oldPassword)) {
            throw new CodeException(ErrorCode.PARAM_ERROR, "旧密码错误");
        }
        if (StringUtils.isEmpty(newPassword)) {
            throw new CodeException(ErrorCode.PARAM_ERROR, "新密码不能为空");
        }
        loginInfoEntity.setLoginPassword(newPassword);
        loginInfoMapper.updateById(loginInfoEntity);
        return ResultUtil.success(getToken(Constant.UserType.LOCAL, loginInfoEntity.getUserId()));

    }


    public ResultUtil<TokenModel> refreshToken(LoginUserModel loginUser) {
        return ResultUtil.success(getToken(loginUser.getType(), loginUser.getId()));
    }


    public ResultUtil<UserInfoModel> register(String loginName, String password) {


        UserInfoEntity entity = loginInfoMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("login_name", loginName));
        if (entity != null) {
            throw new CodeException(ErrorCode.SERVER_ERROR, "用户已经存在");
        }
        String salt = "CRY:" + RandomStringUtils.randomAlphanumeric(16);
        String pwd = DigestUtil.sha256Hex(password + ":" + salt);
        entity = UserInfoEntity.builder().loginState(Constant.UserState.ABLE).loginName(loginName).loginPasswordSalt(salt).loginPassword(pwd).createTime(new Date()).build();
        loginInfoMapper.insert(entity);
        return ResultUtil.success(this.initUserInfoBO(entity));
    }

    public ResultUtil<UserInfoModel> updateUserState(int userId, short state) {

        UserInfoEntity loginInfoEntity = this.loginInfoMapper.selectById(userId);
        if (loginInfoEntity == null) {
            throw new CodeException(ErrorCode.NO_LOGIN_ERROR, "登陆用户不存在");
        }
        loginInfoEntity.setLoginState(state);
        this.loginInfoMapper.updateById(loginInfoEntity);
        return ResultUtil.success(this.initUserInfoBO(loginInfoEntity));
    }


    public ResultUtil<List<UserInfoModel>> listUsers() {

        List<UserInfoEntity> list = this.loginInfoMapper.selectList(new QueryWrapper<>());
        return ResultUtil.success(list.stream().map(this::initUserInfoBO).collect(Collectors.toList()));
    }


    public ResultUtil<UserInfoModel> resetPassword(int userId, String password) {
        UserInfoEntity loginInfoEntity = this.loginInfoMapper.selectById(userId);
        if (loginInfoEntity == null) {
            throw new CodeException(ErrorCode.NO_LOGIN_ERROR, "登陆用户不存在");
        }
        String salt = "CRY:" + RandomStringUtils.randomAlphanumeric(16);
        String pwd = DigestUtil.sha256Hex(password + ":" + salt);
        loginInfoEntity.setLoginPassword(pwd);
        loginInfoEntity.setLoginPasswordSalt(salt);
        this.loginInfoMapper.updateById(loginInfoEntity);
        return ResultUtil.success(this.initUserInfoBO(loginInfoEntity));
    }


    public ResultUtil<Void> destroyUser(int userId) {
        this.loginInfoMapper.deleteById(userId);
        return ResultUtil.success();
    }

    public ResultUtil<LoginSignatureModel> getSignature(String loginName) {
        if (StringUtils.isEmpty(loginName)) {
            return ResultUtil.error(ErrorCode.PARAM_ERROR, "用户名不能为空");
        }

        UserInfoEntity loginInfoBean = this.loginInfoMapper.selectOne(new QueryWrapper<UserInfoEntity>().eq("login_name", loginName));
        LoginSignatureModel model = LoginSignatureModel.builder().signature(loginInfoBean == null ? UUID.randomUUID().toString() : loginInfoBean.getLoginPasswordSalt()).nonce(String.valueOf(System.currentTimeMillis())).build();
        return ResultUtil.success(model);
    }

    public ResultUtil<LoginSignatureModel> getLoginSignature(Integer userId) {
        UserInfoEntity loginInfoBean = this.loginInfoMapper.selectById(userId);
        LoginSignatureModel model = LoginSignatureModel.builder().signature(loginInfoBean == null ? UUID.randomUUID().toString() : loginInfoBean.getLoginPasswordSalt()).nonce(String.valueOf(System.currentTimeMillis())).build();
        return ResultUtil.success(model);
    }

    private TokenModel getToken(String userType, Object userId) {
        Date expire = new Date(System.currentTimeMillis() + HOUR);

        LoginUserModel user = LoginUserModel.builder().id(userId).type(userType).build();
        String token = JWT.create()
                .withIssuer(this.applicaionconfig.getJwtIssuer())
                .withIssuedAt(new Date())
                .withClaim("User", GsonBuilderUtil.create().toJson(user))
                .withExpiresAt(expire)
                .sign(Algorithm.HMAC256(this.applicaionconfig.getJwtPassword()));


        return TokenModel.builder().expire(expire).token(token).build();
    }

    private UserInfoModel initUserInfoBO(UserInfoEntity loginInfoEntity) {
        if (loginInfoEntity == null) {
            return null;
        }
        UserInfoModel userModel = new UserInfoModel();
        userModel.setUserId(loginInfoEntity.getUserId());
        userModel.setLoginName(loginInfoEntity.getLoginName());
        userModel.setPasswordSalt(loginInfoEntity.getLoginPasswordSalt());
        userModel.setState(loginInfoEntity.getLoginState());
        userModel.setRegisterTime(loginInfoEntity.getCreateTime());
        return userModel;
    }

    private UserInfoModel initLoginInfoBO(UserInfoEntity loginInfoEntity) {
        if (loginInfoEntity == null) {
            return null;
        }
        UserInfoModel loginInfoBean = new UserInfoModel();
        loginInfoBean.setUserId(loginInfoEntity.getUserId());
        loginInfoBean.setLoginName(loginInfoEntity.getLoginName());
        loginInfoBean.setPasswordSalt(loginInfoEntity.getLoginPasswordSalt());
        return loginInfoBean;
    }
}
