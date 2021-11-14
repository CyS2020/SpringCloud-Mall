package com.atguigu.gulimall.order.interceptor;

import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author: CyS2020
 * @date: 2021/11/14
 * 描述：拦截器判断用户是否登录
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute == null) {
            request.getSession().setAttribute("msg", "请先登录");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
        loginUser.set(attribute);
        return true;
    }
}
