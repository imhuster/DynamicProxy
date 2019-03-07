package com.iqts;

import java.lang.reflect.Method;

public interface MyInvocationHandler {
	/**
	 * @param proxy  代理类
	 * @param method 需要执行的方法
	 * @param args   method的参数
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable;
}