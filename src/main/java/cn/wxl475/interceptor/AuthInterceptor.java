package cn.wxl475.interceptor;

import cn.hutool.json.JSONUtil;
import cn.wxl475.pojo.Result;
import cn.wxl475.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Value("${jwt.signKey}")
    private String signKey;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response,@NonNull Object handler) throws Exception {
        Claims claims = JwtUtils.parseJWT(request.getHeader("Authorization"), signKey);
        if (claims == null) {
            response.getWriter().write(JSONUtil.toJsonStr(Result.error("令牌无效")));
            return false;
        }
        String uri = request.getRequestURI();
        if(uri.contains("search")){
            return true;
        }else if(!(Boolean) claims.get("userType")){
            response.getWriter().write(JSONUtil.toJsonStr(Result.error("权限不足")));
            return false;
        }
        return true;
    }
}
