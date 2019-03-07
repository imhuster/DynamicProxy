package com.iqts.test.target;

/**
 * 被代理类
 */
public final class RealSubject implements Subject {
	@Override
	public void doOperation() {
		System.out.println("RealSubject doOperation...");
	}
}
