package com.bjunicom.scservice.shiro;

import com.bjunicom.scservice.utils.JsonConvertUtil;
import com.bjunicom.scservice.utils.ResultTools;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JWTFilter extends BasicHttpAuthenticationFilter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * 这里我们详细说明下为什么最终返回的都是true，即允许访问
     * 例如我们提供一个地址 GET /article
     * 登入用户和游客看到的内容是不同的
     * 如果在这里返回了false，请求会被直接拦截，用户看不到任何东西
     * 所以我们在这里返回true，Controller中可以通过 subject.isAuthenticated() 来判断用户是否登入
     * 如果有些资源只有登入用户才能访问，我们只需要在方法上面加上 @RequiresAuthentication 注解即可
     * 但是这样做有一个缺点，就是不能够对GET,POST等请求进行分别过滤鉴权(因为我们重写了官方的方法)，但实际上对应用影响不大
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        if (this.isLoginAttempt(request, response)) {
            try {
                this.executeLogin(request, response);
            } catch (Exception e) {
                try {
                    this.response401(request,response,e.getMessage());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * 判断用户是否想要登入。
     * 检测header里面是否包含Authorization字段即可
     */
    @Override
    protected boolean isLoginAttempt(ServletRequest request, ServletResponse response) {
        String token = this.getAuthzHeader(request);
        return token != null;
    }

    @Override
    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {
        // 拿到当前Header中Authorization的AccessToken(Shiro中getAuthzHeader方法已经实现)
        JWTToken token = new JWTToken(this.getAuthzHeader(request));
        // 提交给UserRealm进行认证，如果错误他会抛出异常并被捕获
        this.getSubject(request, response).login(token);
        // 如果没有抛出异常则代表登入成功，返回true
        return true;
    }

    /**
     * 无需转发，直接返回Response信息
     */
    private void response401(ServletRequest req, ServletResponse resp, String msg) throws IOException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) resp;
        httpServletResponse.setStatus(401);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json; charset=utf-8");
        ServletOutputStream out = null;

            out = httpServletResponse.getOutputStream();
            String data = JsonConvertUtil.objectToJson(ResultTools.result(401, "无权访问:" + msg, null));
            out.write(data.getBytes("UTF-8"));

            if (out != null) {
                out.close();
            }
    }


    /**
     * 对跨域提供支持
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", httpServletRequest.getHeader("Access-Control-Request-Headers"));
        // 跨域时会首先发送一个option请求，这里我们给option请求直接返回正常状态
        if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            return false;
        }
        return super.preHandle(request, response);
    }


//    private void response401(ServletRequest req, ServletResponse resp, String msg) {
//        HttpServletResponse httpServletResponse = (HttpServletResponse) resp;
//        httpServletResponse.setStatus(401);
//        httpServletResponse.setCharacterEncoding("UTF-8");
//        httpServletResponse.setContentType("application/json; charset=utf-8");
//        PrintWriter out = null;
//        try {
//            out = httpServletResponse.getWriter();
//            String data = JsonConvertUtil.objectToJson(ResultTools.result(401, "无权访问:" + msg, null));
//            out.append(data);
//        } catch (IOException e) {
//            logger.error(e.getMessage());
//        } finally {
//            if (out != null) {
//                out.flush();
//                out.close();
//            }
//        }
//    }
//    /**
//     * 将非法请求跳转到 /401
//     */
//    private void response401(ServletRequest req, ServletResponse resp) {
//        try {
//            HttpServletResponse httpServletResponse = (HttpServletResponse) resp;
//            httpServletResponse.sendRedirect("/401");
//        } catch (IOException e) {
//            logger.error(e.getMessage());
//        }
//    }


}
