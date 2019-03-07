package com.iqts;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Proxy 类
 */
public class MyProxy {

	private static AtomicInteger counter = new AtomicInteger(0);
	protected MyInvocationHandler invocationHandler;

	public Object invoke(String methodName, Object... args) throws Exception {// for final class
		Object ret = null;
		Class<?>[] parameterTypes = null;
		if (args != null && args.length > 0) {
			parameterTypes = new Class<?>[args.length];
			for (int i = 0; i < args.length; ++i) {
				parameterTypes[i] = args[i].getClass();
			}
		} else {
			parameterTypes = new Class<?>[0];
		}
		try {
			Method method = this.getClass().getDeclaredMethod(methodName, parameterTypes);
			try {
				ret = method.invoke(this, args);
			} catch (Throwable e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			throw e;
		}
		return ret;
	}

	public MyProxy(MyInvocationHandler invocationHandler) {
		super();
		this.invocationHandler = invocationHandler;
	}

	public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces,
			MyInvocationHandler invocationHandler) throws Exception {

		final int numOfProxy = counter.getAndIncrement();

		StringBuilder sb = new StringBuilder();
		sb.append(generatorHead(interfaces[0].getPackage().getName()));

		sb.append("public final class $Proxy");
		sb.append(numOfProxy);
		sb.append(" extends MyProxy implements ");

		Set<Method> methodSet = new HashSet<>();

		for (int i = 0, len = interfaces.length - 1; i < len; ++i) {
			sb.append(interfaces[i].getName() + ",");
			methodSet.addAll(Arrays.asList(interfaces[i].getDeclaredMethods()));
		}

		sb.append(interfaces[interfaces.length - 1].getName());
		sb.append("{\r\n");
		methodSet.addAll(Arrays.asList(interfaces[interfaces.length - 1].getDeclaredMethods()));

		// 过滤掉static、private方法
		methodSet = methodSet.stream()
				.filter((m) -> (m.getModifiers() & Modifier.STATIC) == 0 && (m.getModifiers() & Modifier.PRIVATE) == 0)
				.collect(Collectors.toSet());

		// 排除重载后重复的方法
		// 虽然实际的类不可能实现有方法重载冲突的两个接口，但是考虑到interfaces可以手动填入，故做此过滤！
		methodSet = methodSet.parallelStream().filter(distinctByKey((Method method) -> {
			StringBuilder temp = new StringBuilder();
			if (method != null) {
				temp.append(method.getName());
				temp.append(Arrays.toString(method.getParameters()));
			}
			return temp.toString();
		})).collect(Collectors.toSet());

		sb.append("public $Proxy");
		sb.append(numOfProxy);
		sb.append("(MyInvocationHandler paramMyInvocationHandler) {\r\n");
		sb.append("super(paramMyInvocationHandler);\r\n");
		sb.append("}\r\n");

		// 拼接被代理类的所有接口的方法
		sb.append(generatorMethods(methodSet));

		sb.append("}");

		FileUtils.saveResourceCode(sb.toString(), "$Proxy" + numOfProxy + ".java");

		// 动态编译并加载字节码
		Class<?> dynamicClass = DynamicClassUtils.generatorDynamicClass("$Proxy" + numOfProxy + ".java",
				interfaces[0].getPackage().getName() + "." + "$Proxy" + numOfProxy, sb.toString());

		try {
			// 反射调用代理类的构造器，实例化代理类
			return dynamicClass.getConstructor(MyInvocationHandler.class).newInstance(invocationHandler);
		} catch (Exception e) {
			throw e;
		}
	}

	private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Set<Object> seen = ConcurrentHashMap.newKeySet();
		return t -> seen.add(keyExtractor.apply(t));
	}

	public static Object newProxyInstance(ClassLoader loader, Class<?> clz, MyInvocationHandler invocationHandler)
			throws Exception {

		final int numOfProxy = counter.getAndIncrement();

		StringBuilder sb = new StringBuilder();
		sb.append(generatorHead(clz.getPackage().getName()));

		// 类声明
		sb.append("public final class $Proxy");
		sb.append(numOfProxy);

		if (clz.isInterface()) {// clz is a interface
			sb.append(" implements ");
			sb.append(clz.getName());
		} else {
			sb.append(" extends ");
			if ((clz.getModifiers() & Modifier.FINAL) == 0) {// a non-final Class
				sb.append(clz.getName());
			} else {// final class
				sb.append(" MyProxy ");
			}
		}
		sb.append("{\r\n");

		// 过滤掉static、private方法
		Set<Method> methodSet = Arrays.asList(clz.getDeclaredMethods()).stream()
				.filter((m) -> (m.getModifiers() & Modifier.STATIC) == 0 && (m.getModifiers() & Modifier.PRIVATE) == 0)
				.collect(Collectors.toSet());

		// 构造器声明
		if ((clz.getModifiers() & Modifier.FINAL) != 0) {// final class
			sb.append("public $Proxy");
			sb.append(numOfProxy);
			sb.append("(MyInvocationHandler paramMyInvocationHandler) {\r\n");
			sb.append("super(paramMyInvocationHandler);\r\n");
			sb.append("}\r\n");
		} else {// interface or class
			sb.append("private MyInvocationHandler invocationHandler;");
			sb.append("public $Proxy");
			sb.append(numOfProxy);
			sb.append("(MyInvocationHandler paramMyInvocationHandler) {\r\n");
			sb.append("this.invocationHandler = paramMyInvocationHandler;\r\n");
			sb.append("}\r\n");
		}

		// 拼接被代理类的方法
		sb.append(generatorMethods(methodSet));

		sb.append("}");

		FileUtils.saveResourceCode(sb.toString(), "$Proxy" + numOfProxy + ".java");

		// 动态编译并加载字节码
		Class<?> dynamicClass = DynamicClassUtils.generatorDynamicClass("$Proxy" + numOfProxy + ".java",
				clz.getPackage().getName() + "." + "$Proxy" + numOfProxy, sb.toString());
		try {
			// 反射调用代理类的构造器，实例化代理类
			return dynamicClass.getConstructor(MyInvocationHandler.class).newInstance(invocationHandler);
		} catch (Exception e) {
			throw e;
		}
	}

	// 生成所有方法，包括Method field的声明
	private static String generatorMethods(Set<Method> methodSet) {
		StringBuilder sb = new StringBuilder();

		StringBuilder loadMethod = new StringBuilder();
		if (methodSet != null && methodSet.size() > 0) {
			String methodName = null;
			Map<String, List<Method>> methodMap = new HashMap<>();
			for (Method m : methodSet) {
				methodName = m.getName();
				List<Method> methodList = methodMap.getOrDefault(methodName, new ArrayList<>());
				methodList.add(m);
				methodMap.put(methodName, methodList);
			}

			for (String key : methodMap.keySet()) {
				List<Method> methodList = methodMap.get(key);
				for (int i = 0, len = methodList.size(); i < len; ++i) {
					Method m = methodList.get(i);
					sb.append(generatorMethod(m, i));

					loadMethod.append(m.getName());
					loadMethod.append(i);
					loadMethod.append(" = ");
					loadMethod.append("Class.forName(\"");
					loadMethod.append(m.getDeclaringClass().getName());
					loadMethod.append("\").getMethod(\"");
					loadMethod.append(m.getName());
					loadMethod.append("\",");
					Parameter[] parameters = m.getParameters();
					if (parameters == null || parameters.length < 1) {
						loadMethod.append(" new Class[0]);\r\n");
					} else {
						loadMethod.append(" new Class[]{");
						for (int j = 0, size = parameters.length - 1; j < size; ++j) {
							loadMethod.append("Class.forName(\"" + parameters[j].getType().getName() + "\")");
						}
						loadMethod.append(
								"Class.forName(\"" + parameters[parameters.length - 1].getType().getName() + "\")");
						loadMethod.append(" });\r\n");
					}
				}
			}
		}

		sb.append("static {");
		sb.append("try {");

		sb.append(loadMethod);

		sb.append("} catch (NoSuchMethodException localNoSuchMethodException) {\r\n"
				+ "			throw new NoSuchMethodError(localNoSuchMethodException.getMessage());\r\n"
				+ "		} catch (ClassNotFoundException localClassNotFoundException) {\r\n"
				+ "			throw new NoClassDefFoundError(localClassNotFoundException.getMessage());\r\n"
				+ "		}\r\n");

		sb.append("}\r\n");
		return sb.toString();
	}

	private static String generatorHead(String packageName) {
		StringBuilder sb = new StringBuilder();
		sb.append("package ");
		sb.append(packageName);
		sb.append(";\r\n");
		sb.append("import ");
		sb.append(MyProxy.class.getName());
		sb.append(";\r\n");
		sb.append("import ");
		sb.append(MyInvocationHandler.class.getName());
		sb.append(";\r\n");
		sb.append("import java.lang.reflect.Method;\r\n");
		sb.append("import java.lang.reflect.UndeclaredThrowableException;\r\n");
		return sb.toString();
	}

	private static String generatorMethod(Method method, int idx) {// idx 应对重载的方法
		int modifiers = method.getModifiers();

		if ((modifiers & Modifier.STATIC) != 0 || (modifiers & Modifier.PRIVATE) != 0) {
			return "";
		}

		method.setAccessible(true);
		StringBuilder sb = new StringBuilder();

		sb.append(" private static Method ");

		String name = method.getName();

		sb.append(name);
		sb.append(idx);// 应对重载的方法
		sb.append(";\r\n");

		sb.append("public final ");
		String returnType = method.getReturnType().getName();
		sb.append(returnType);
		sb.append(" ");
		sb.append(name);
		sb.append("(");

		StringBuilder params = new StringBuilder();

		Parameter[] parameters = method.getParameters();
		if (parameters != null && parameters.length > 0) {
			for (int i = 0; i < parameters.length - 1; ++i) {
				sb.append(parameters[i].getType().getName());
				sb.append(" param");
				sb.append(i);
				sb.append(",");

				params.append(" param");
				params.append(i);
				params.append(",");
			}
			sb.append(parameters[parameters.length - 1].getType().getName());
			sb.append(" param");
			sb.append(parameters.length - 1);

			params.append(" param");
			params.append(parameters.length - 1);

		}

		sb.append("){\r\n");
		sb.append("try {\r\n");
		if (!returnType.trim().equals("void")) {
			sb.append("return (");
			sb.append(returnType);
			sb.append(") ");
		}
		sb.append("this.invocationHandler.invoke(this,");
		sb.append(name);
		sb.append(idx);// 应对重载的方法
		sb.append(",");
		sb.append(" new Object[] { ");
		sb.append(params);
		sb.append("});\r\n");
		sb.append("} catch (Error | RuntimeException localError) {\r\n");
		sb.append("throw localError;\r\n");
		sb.append("} catch (Throwable localThrowable) {\r\n");
		sb.append("throw new UndeclaredThrowableException(localThrowable);\r\n");
		sb.append("}\r\n}\r\n");
		return sb.toString();
	}
}