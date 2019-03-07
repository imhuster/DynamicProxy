package com.iqts.test.target;

/**
 * 无接口的被代理类
 */
public class ConcreteSubject {

	public void doOperation() {
		System.out.println("ConcreteSubject doOperation...");
		other();
	}

	public void other() {
		System.out.println("ConcreteSubject other...");
	}
}