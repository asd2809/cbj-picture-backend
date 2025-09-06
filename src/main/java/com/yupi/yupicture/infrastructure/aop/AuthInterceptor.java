package com.yupi.yupicture.infrastructure.aop;


import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.infrastructure.annotation.AuthCheck;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.user.valueobject.UserRoleEnum;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

//什么是切面
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
//    @annotation这个叫切点
//    只会作用在有authCheck注解的方法上
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
//        获取的是AuthCheck()里写的值
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//        获取当前用户
        User loginUser = userApplicationService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
//        如果不需要权限，放行
        if (mustRoleEnum == null) {
            log.info("不需要权限：" + authCheck.mustRole());
            return joinPoint.proceed();
        }
//        必须有权限才能通过
//        获取用户的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if (userRoleEnum == null) {
            log.info("用户的字段里没有权限" + mustRole);
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        通过权限校验，放行
        return joinPoint.proceed();

    }

}
