package com.xuecheng.ucenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/28 18:09
 */
@Slf4j
@Service
public class UserServiceImpl implements UserDetailsService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    AuthService authService;
    /**
     * @description 查询用户信息组成用户身份信息
     * @param s AuthParamsDto 类型的 json 数据
     * @return
    org.springframework.security.core.userdetails.UserDetails
     * @author Mr.M
     * @date 2022/9/28 18:30
     */
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        AuthParamsDto authParamsDto = null;
        try {
            //将认证参数转为 AuthParamsDto 类型
            authParamsDto = JSON.parseObject(s,
                    AuthParamsDto.class);
        } catch (Exception e) {
            log.info("认证请求不符合项目要求:{}",s);
            throw new RuntimeException("认证请求数据格式不对");
        }
        //开始认证
        authService.execute(authParamsDto);


        //账号
        String username = authParamsDto.getUsername();
        XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if(user==null){
            //返回空表示用户不存在
            return null;
        }
        //取出数据库存储的正确密码
        String password =user.getPassword();
        //用户权限,如果不加报 Cannot pass a null GrantedAuthority collection
        String[] authorities = {"p1"};
        //将 user 对象转 json
        String userString = JSON.toJSONString(user);
        //创建 UserDetails 对象
        UserDetails userDetails = User.withUsername(userString).password(password).authorities(authorities).build();
        return userDetails;
    }
}
