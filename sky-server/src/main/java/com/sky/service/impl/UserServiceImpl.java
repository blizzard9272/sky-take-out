package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    // 定义微信服务接口地址的常量
    public static final String WECHAT_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";


    // 注入微信相关的配置属性对象（WeChatProperties），从中获取appid和secret等相关信息
    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 调用微信接口服务，获得当前微信用户的openid
        // 根据封装的HttpClientUtil对象，调用doGet方法，传入微信登录接口地址和请求参数（用Map封装），获得响应结果
        String openid = this.getOpenIdFromWeChat(userLoginDTO.getCode());

        // 判断openid是否为空，如果为空表示登录失败，抛出业务异常
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 查询数据库判断当前用户是否为新用户
        User user = userMapper.getByOpenId(openid);

        // 如果是新用户， 自动完成注册（封装一个User对象，设置openid等相关属性，保存到数据库）
        if(user == null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        // 返回用户信息

        return user;
    }

    /**
     * 调用微信接口服务，获得当前微信用户的openid
     * @param code
     * @return
     */
    private String getOpenIdFromWeChat(String code){
        Map<String, String> map = new HashMap<>();
        map.put(JwtClaimsConstant.APPID,weChatProperties.getAppid());
        map.put(JwtClaimsConstant.SECRET,weChatProperties.getSecret());
        map.put(JwtClaimsConstant.JS_CODE,code);
        map.put(JwtClaimsConstant.GRANT_TYPE,"authorization_code");
        String json = HttpClientUtil.doGet(WECHAT_LOGIN_URL, map);
        log.info("WeChat jscode2session response: {}", json);
        if(json == null || json.isBlank()){
            log.warn("WeChat jscode2session returned empty body");
            return null;
        }
        JSONObject jsonObject = JSONObject.parseObject(json);
        if(jsonObject == null){
            log.warn("WeChat jscode2session response could not be parsed");
            return null;
        }
        if(jsonObject.containsKey("errcode")){
            log.warn("WeChat jscode2session error, errcode={}, errmsg={}", jsonObject.getString("errcode"), jsonObject.getString("errmsg"));
            return null;
        }
        String openid = jsonObject.getString(JwtClaimsConstant.OPENID);

        return openid;
    }
}
