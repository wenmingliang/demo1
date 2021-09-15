package com.example.demo.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//@Component
//@Aspect
public class AlphaAspect {

    @Pointcut("execution(* com.example.demo.service.*.*(..))")
    public void pointcut(){


    }

    @Before("pointcut()")
    public void before(){
        for(int i=1;i<10;i++) {
            System.out.println("before***************************************************");
        }
    }

    @After("pointcut()")
    public void after(){
        for(int i=1;i<10;i++) {
            System.out.println("after***************************************************");
        }
    }

    @AfterThrowing("pointcut()")
    public void afterThrowing(){
        for(int i=1;i<10;i++) {
            System.out.println("afterThrowing***************************************************");
        }
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{
        System.out.println("around before");
        Object obj = joinPoint.proceed();
        System.out.println("around after");
        return obj;

    }

}
