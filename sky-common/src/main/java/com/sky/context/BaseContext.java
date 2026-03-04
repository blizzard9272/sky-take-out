package com.sky.context;

public class  BaseContext {

    // ThreadLocal为每个线程提供单独一份存储空间，具有线程隔离的效果，只有在线程内才能获取到线程的值，线程外则不能访问
    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 设置当前线程的线程局部变量的值
    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    // 返回当前线程所对应的线程局部变量的值
    public static Long getCurrentId() {
        return threadLocal.get();
    }

    // 移除当前现成的线程局部变量
    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
