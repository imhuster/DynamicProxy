package com.iqts.test.target.testcase;

import org.junit.Test;

import com.iqts.MyProxy;
import com.iqts.test.target.ConcreteSubject;
import com.iqts.test.target.FinalSubject;
import com.iqts.test.target.RealSubject;
import com.iqts.test.target.Subject;
import com.iqts.test.target.SubjectInvocationHandler;

/**
 * 测试
 */
public class DynamicClassTest {

	@Test
	public void testInterface() {
		// 输出代理类到文件中
//		System.setProperty("DebuggingClassWriter.DEBUG_LOCATION_PROPERTY", "E:/tmp/");
		try {
			Subject proxy = (Subject) MyProxy.newProxyInstance(RealSubject.class.getClassLoader(),
					RealSubject.class.getInterfaces(), new SubjectInvocationHandler(new RealSubject()));
			proxy.doOperation();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testClass() {
		// 输出代理类到文件中
//		System.setProperty("DebuggingClassWriter.DEBUG_LOCATION_PROPERTY", "E:/tmp/");
		try {
			ConcreteSubject proxy = (ConcreteSubject) MyProxy.newProxyInstance(ConcreteSubject.class.getClassLoader(),
					ConcreteSubject.class, new SubjectInvocationHandler(new ConcreteSubject()));
			proxy.doOperation();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testFinalClass() {
		// 输出代理类到文件中
//		System.setProperty("DebuggingClassWriter.DEBUG_LOCATION_PROPERTY", "E:/tmp/");
		try {
			MyProxy finalProxy = (MyProxy) MyProxy.newProxyInstance(FinalSubject.class.getClassLoader(),
					FinalSubject.class, new SubjectInvocationHandler(new FinalSubject()));
			finalProxy.invoke("doOperation");

			finalProxy.invoke("doOperation", "method with args.");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
