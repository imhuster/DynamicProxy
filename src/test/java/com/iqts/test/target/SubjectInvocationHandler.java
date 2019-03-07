package com.iqts.test.target;

import java.lang.reflect.Method;

import com.iqts.MyInvocationHandler;

/**
 * MyInvocationHandler 实例对象
 */
public class SubjectInvocationHandler implements MyInvocationHandler {
	private Object object;// 被代理的对象

	public SubjectInvocationHandler(Object object) {
		this.object = object;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("before:");
		Object ret = method.invoke(object, args);
		System.out.println("after:");
		return ret;
	}
}