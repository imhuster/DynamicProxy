package com.iqts.test.target;

/**
 * final且 无接口的被代理类
 */
public final class FinalSubject {
	public final void doOperation() {
		System.out.println("FinalSubject doOperation...");
	}

	public final void doOperation(String msg) {
		System.out.println(msg + "FinalSubject doOperation...");
	}

}