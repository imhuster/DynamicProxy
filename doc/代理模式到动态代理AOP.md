##  彻底理解动态代理

为了理解动态代理，我们需要先了解代理模式是怎么回事。

###  代理模式

代理模式给某一个对象提供一个代理对象，并由代理对象控制对原对象的引用。可以将代理模式理解为生活中常见的中介，UML 图如下。



![OverrideTrap](proxy.png)

```java
public interface Subject {
	public void doOperation();
}
public class RealSubject implements Subject {
	@Override
	public void doOperation() {
		System.out.println("RealSubject doOperation...");
	}
}
public class Proxy implements Subject {
	private Subject subject;
	public Proxy(Subject subject) {
		this.subject = subject;
	}
	@Override
	public void doOperation() {
		System.out.println("Proxy before RealSubject doOperation...");
		subject.doOperation();
		System.out.println("Proxy after RealSubject doOperation...");
	}
}
public class Client {
	public static void main(String[] args) {
		Subject subject = new Proxy(new RealSubject());
		subject.doOperation();
	}
}
```

上述代码输出如下：

```java
Proxy before RealSubject doOperation...
RealSubject doOperation...
Proxy after RealSubject doOperation...
```

通过代理模式，我们可以做到在不修改目标对象的前提下,对目标对象进行功能扩展。但是上述静态代理的不足之处在于需要事先写好相应的代理类，而且在接口发生变化时需要对被代理类及代理类进行修改，对此 Java 引入了动态代理的概念。

### 动态代理

与静态代理需要事先构建不同，动态代理是动态地在内存中生成的。一般而言，动态代理可以由 JDK 动态代理及CGLib 动态代理实现。

#### JDK动态代理

使用JDK动态代理只需要3部即可完成。

1、创建被代理的对象 RealSubject
2、创建被代理对象的处理对象，持有目标（被代理）对象 JDKInvocationHandler
3、使用Proxy的静态方法 newProxyInstance 创建代理对象

```
public class JDKInvocationHandler implements java.lang.reflect.InvocationHandler {
	private Subject subject;
	public JDKInvocationHandler(Subject subject) {
		this.subject = subject;
	}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("JDKProxy before RealSubject doOperation...");
		Object ret = method.invoke(subject, args);
		System.out.println("JDKProxy after RealSubject doOperation...");
		return ret;
	}
}
public class Client {
	public static void main(String[] args) {
	    //创建被代理的对象 realSubject
		RealSubject realSubject = new RealSubject();
		//创建被代理对象的处理对象
		InvocationHandler handler = new JDKInvocationHandler(realSubject);
		//创建代理对象
		Subject proxy = (Subject) Proxy.newProxyInstance(
			RealSubject.class.getClassLoader(),
			RealSubject.class.getInterfaces(),
			handler);
		//执行相应的方法
		proxy.doOperation();
	}
}
```

上述代码输出如下：

```java
JDKProxy before RealSubject doOperation...
RealSubject doOperation...
JDKProxy after RealSubject doOperation...
```

其中，InvocationHandler 是 Java 自带的接口，其定义如下：

```java
public interface InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Throwable;
}
```

Proxy 静态方法的定义如下：

```
public static Object newProxyInstance(ClassLoader loader,
                                      Class<?>[] interfaces,
                                      InvocationHandler h)
```

其中，loader 为类加载器，interfaces 为被代理对象需要实现的接口，h为方法调用的实际处理者。

但是，等一下，为什么需要这三个参数呢？是不是任意一个 Java 类都可以动态代理呢？我们不妨深入看看 Proxy类到底做了什么？Proxy.newProxyInstance 方法的主要内容（删除了安全检测等内容）如下：

```java
public static Object newProxyInstance(ClassLoader loader,Class<?>[] interfaces,
                   InvocationHandler h) throws IllegalArgumentException{
    final Class<?>[] intfs = interfaces.clone();
    //根据ClassLoader及接口获取指定的代理类的Class信息
    Class<?> cl = getProxyClass0(loader, intfs);
    try {
        //在Proxy中 constructorParams被硬编码为{InvocationHandler.class};
        //获取代理类的参数为 InvocationHandler 的构造器
        final Constructor<?> cons = cl.getConstructor(constructorParams);
        //生成代理类
        return cons.newInstance(new Object[]{h});
    } catch (XXException e) {
        throw new XXException(e.toString(), e);
    }
}
//从proxyClassCache中获取代理类的Class信息，如果没有则根据classLoader、ingerfaces生成加载并缓存
private static Class<?> getProxyClass0(ClassLoader loader,Class<?>... interfaces) {
        return proxyClassCache.get(loader, interfaces);
}
```

动态编译生成代理类的代码如下：

```java
//通过反射动态生成、编译代理类，得到代理类的字节码数据
byte[] proxyClassFile = ProxyGenerator.generateProxyClass(
    								proxyName,interfaces, accessFlags);
try {
    //动态加载代理类
    return defineClass0(loader,proxyName,proxyClassFile,0,proxyClassFile.length);
} catch (ClassFormatError e) {
    throw new IllegalArgumentException(e.toString());
}
```

至此，我们知道了 Proxy 的运行逻辑，为了进一步了解动态生成的代理类的内容，我们不妨输出并使用反编译工具查看动态生成的代理类的信息：

通过在程序运行时设置：

```java
System.getProperties().put("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
```

即可将生成的代理类保存在项目根目录下，路径为：com/sun/proxy/$ProxyNum.class

使用Java Decompiler工具查看，结果如下：

```java
package com.sun.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

public final class $Proxy0 extends Proxy implements Subject {
	private static Method m1;
	private static Method m2;
	private static Method m3;
	private static Method m0;

	public $Proxy0(InvocationHandler paramInvocationHandler) {
		super(paramInvocationHandler);
	}

	@Override
	public final boolean equals(Object paramObject) {
		try {
			return ((Boolean) this.h.invoke(this, m1, 
                            new Object[] { paramObject })).booleanValue();
		} catch (Error | RuntimeException localError) {
			throw localError;
		} catch (Throwable localThrowable) {
			throw new UndeclaredThrowableException(localThrowable);
		}
	}

	@Override
	public final String toString() {
		try {
			return (String) this.h.invoke(this, m2, null);
		} catch (Error | RuntimeException localError) {
			throw localError;
		} catch (Throwable localThrowable) {
			throw new UndeclaredThrowableException(localThrowable);
		}
	}

	@Override
	public final void doOperation() {
		try {
			this.h.invoke(this, m3, null);
			return;
		} catch (Error | RuntimeException localError) {
			throw localError;
		} catch (Throwable localThrowable) {
			throw new UndeclaredThrowableException(localThrowable);
		}
	}

	@Override
	public final int hashCode() {
		try {
			return ((Integer) this.h.invoke(this, m0, null)).intValue();
		} catch (Error | RuntimeException localError) {
			throw localError;
		} catch (Throwable localThrowable) {
			throw new UndeclaredThrowableException(localThrowable);
		}
	}

	static {
		try {
			m1 = Class.forName("java.lang.Object").getMethod("equals",
					new Class[] { Class.forName("java.lang.Object") });
			m2 = Class.forName("java.lang.Object").getMethod("toString", new Class[0]);
			m3 = Class.forName("com.iqts.proxy.Subject").getMethod("doOperation", new Class[0]);
			m0 = Class.forName("java.lang.Object").getMethod("hashCode", new Class[0]);
			return;
		} catch (NoSuchMethodException localNoSuchMethodException) {
			throw new NoSuchMethodError(localNoSuchMethodException.getMessage());
		} catch (ClassNotFoundException localClassNotFoundException) {
			throw new NoClassDefFoundError(localClassNotFoundException.getMessage());
		}
	}
}
```

到这里可以大概推测出，JDK 动态代理是通过继承 Proxy 类，实现被代理类的所有接口生成动态代理类。这也解释了采用 JDK 动态代理时为什么只能使用接口引用指向代理，而不能使用被代理的具体类引用指向代理。

```java
Subject proxy = (Subject) Proxy.newProxyInstance(RealSubject.class.getClassLoader(),
				RealSubject.class.getInterfaces(), handler);//ok

//java.lang.ClassCastException: com.sun.proxy.$Proxy0 cannot be cast to RealSubject
RealSubject proxy = (RealSubject) Proxy.newProxyInstance(
    			RealSubject.class.getClassLoader(),
    			RealSubject.class.getInterfaces(), handler);
```

此外，除了实现了被代理所有接口中的方法外，JDK 动态代理还重写了 Object 类中的 hashCode、equals、toString 三个方法。

为了更好地看出类之间的依赖关系，上述代码可以简化如下：

```java
public interface Subject {
	public void doOperation();
}
public class RealSubject implements Subject {
	@Override
	public void doOperation() {
		System.out.println("RealSubject doOperation...");
	}
}
public interface InvocationHandler {
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable;
}
public class Proxy implements java.io.Serializable {
    protected InvocationHandler h;
    protected Proxy(InvocationHandler h) {
        this.h = h;
    }
    public static Object newProxyInstance(ClassLoader loader,
                                          Class<?>[] interfaces,
                                          InvocationHandler h){
        Class<?> cl = getProxyClass0(loader, intfs);
        final Constructor<?> cons = cl.getConstructor(InvocationHandler.class);
        return cons.newInstance(new Object[]{h});
    }
}
public final class $Proxy0 extends Proxy implements Subject {
    private static Method m3;
    public $Proxy0(InvocationHandler paramInvocationHandler) {
		super(paramInvocationHandler);
	}
    @Override
	public final void doOperation() {
		try {
			this.h.invoke(this, m3, null);
		} catch (Error | RuntimeException localError) {
			throw localError;
		} catch (Throwable localThrowable) {
			throw new UndeclaredThrowableException(localThrowable);
		}
	}
}
```

到这里我们就不难理解，为什么 JDK 实现动态代理必须要求被代理类实现接口，这是由于动态代理动态生成的代理类需要继承 Proxy 类，而 Java 中只能单继承的限制使得被代理类必须实现接口才能实现动态代理。

JDK 能够很好地实现动态代理，但是如果被代理的类没有实现接口就无法实现动态代理，这时候我们就需要使用第三方工具来帮忙了。

### CGLib

CGLib 是一个强大的高性能的代码生成包，它可以在运行期扩展 Java 类及实现Java接口、提供方法的拦截，因此被众多 AOP 框架使用。CGLib 包的底层是通过使用字节码处理框架 ASM 来转换字节码并生成新的类。

使用 CGLib 实现动态代理也很简单，首先

1. 创建Enhancer对象
2. 设置被代理类
3. 回调对象（回调类实现 MethodInterceptor或InvocationHandler接口）
4. 创建并设置回调对象
5. 创建代理对象

```JAVA
public class CGLib {
	public static void main(String[] args) {
		// 创建Enhancer对象
		Enhancer enhancer = new Enhancer();
		// 设置被代理类
		enhancer.setSuperclass(ConcreteSubject.class);
        
		// 创建回调对象
        //实现 MethodInterceptor 接口
		Callback callback = new CGLibMethodInterceptor();
        //实现 InvocationHandler 接口
        Callback callback = new CGLibInvocationHandler(new ConcreteSubject());
		
        // 设置回调对象
		enhancer.setCallback(callback);
		// 创建代理对象
		ConcreteSubject subject = (ConcreteSubject) enhancer.create();
		subject.doOperation();
	}
}
//回调对象 实现 MethodInterceptor
public class CGLibMethodInterceptor implements MethodInterceptor {
	@Override
	public Object intercept(Object obj,Method method,Object[] args,MethodProxy proxy) 
        throws Throwable {
		Object ret = null;
		System.out.println("CGLib before ConcreteSubject doOperation...");
		ret = proxy.invokeSuper(obj, args);
		System.out.println("CGLib after ConcreteSubject doOperation...");
		return ret;
	}
}
//回调对象 实现 InvocationHandler
public class CGLibInvocationHandler implements InvocationHandler {
    
		private Object realSubject;

		public CGLibInvocationHandler(Object realSubject) {
			super();
			this.realSubject = realSubject;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) 
            throws Throwable {
			System.out.println("CGLib before ConcreteSubject doOperation...");
			Object ret = method.invoke(realSubject, args);
			System.out.println("CGLib after ConcreteSubject doOperation...");
			return ret;
		}
	}
//被代理类
public class ConcreteSubject {
	public void doOperation() {
		System.out.println("ConcreteSubject doOperation...");
	}
}
```


输出如下：

```java
CGLib before ConcreteSubject doOperation...
ConcreteSubject doOperation...
CGLib after ConcreteSubject doOperation...
```
为了进一步理解 CGLib 动态代理的生成机制，我们不妨将生成的动态代理类保存到文件中，可以通过设置：

```java
System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "E://CGLib//proxy//");
```

来导出动态代理类，使用Java Decompiler工具查看，结果如下：

```java
public class ConcreteSubject$$EnhancerByCGLib$$7e8b8caf
    extends ConcreteSubject implements Factory {
    
	private MethodInterceptor CGLib$CALLBACK_0;

	final void CGLib$doOperation$0() {
		super.doOperation();
	}
    
	@Override
	public final void doOperation(){
    	MethodInterceptor tmp4_1 = this.CGLib$CALLBACK_0;
        if (tmp4_1 == null){
          tmp4_1;
          CGLib$BIND_CALLBACKS(this);
        }
        if (this.CGLib$CALLBACK_0 != null) {
          return;
        }
        super.doOperation();
	}
    @Override
    public final boolean equals(Object paramObject)...
    @Override
    public final String toString()...
    @Override
    public final int hashCode()...
    @Override
    public final Object clone()...
}
```

通过反编译得到代码可以看出，CGLib 是通过继承被代理类 ConcreteSubject 实现动态代理的，这也就要求被代理的类不能是 final 类。此外，与 JDK 动态代理相比，CGLib 不仅重写了 Object 类的 hashCode、equals、toString方法，还重写了 clone 方法。

此外，值得注意的是创建回调对象时，采用实现 MethodInterceptor 接口与 InvocationHandler 接口这两种方式除了是否需要额外创建被代理对象以及方法调用的差异外，还有一个小细节：采用实现 InvocationHandler 接口的方式生成的代理类在调用的方法内部如果还调用该代理类的其他成员方法时，会对被调用的其他方法进行代理，而采用 MethodInterceptor 接口方式不会。(ps:  JDK 动态代理也不会对非目标方法进行代理。)

为被代理类添加两个方法：

```java
public class ConcreteSubject {

	public static void fn() {
		System.out.println("fn");
	}

	public void doOperation() {
		System.out.println("ConcreteSubject doOperation...");
		other();// InvocationHandler 同时会代理非目标方法
		fn();//静态方法不代理
	}

	public void other() {
		System.out.println("ConcreteSubject other...");
	}
}
```
相应的输出如下：
```
invocationHandler
CGLib before RealSubject doOperation...
ConcreteSubject doOperation...
ConcreteSubject other...
fn
CGLib after RealSubject doOperation...
==================
methodInterceptor
Cglib before ConcreteSubject doOperation...
ConcreteSubject doOperation...
Cglib before ConcreteSubject doOperation...
ConcreteSubject other...
Cglib after ConcreteSubject doOperation...
fn
Cglib after ConcreteSubject doOperation...

```

CGLib 生成动态代理的两种方式的区别总结如下：

| 项目 |                 MethodInterceptor                 |      InvocationHandler      |
| :------: | :------: | :------: |
| 是否依赖被代理对象实例 | 不依赖 |依赖 |
| 目标方法执行方式 | method.invokeSuper(proxy, args) |method.invoke(realSubject, args); |
|   非目标方法是否进行代理   | 不代理 |代理 |

###  JDK 与 CGLib 动态代理的区别

结合 JDK 动态代理的实现，可以得出下列区别：

| 项目 |                 JDK                 |      CGLib      |
| :------: | :------: | :------: |
| 被代理对象的要求 | 必须实现接口(可为 final 类) |非final类 |
| 代理类生成方式 | 继承 Proxy，实现被代理类的所有接口 |继承被代理类，实现 Factory 接口 |
|   非目标方法是否进行代理   | 不进行代理 |可通过 InvocationHandler 进行代理 |

至此，动态代理两种实现方式及其原理已经介绍完毕，在理解了相关原理后，我们完全可以通过反射及Java动态编译技术实现动态代理。

既然JDK动态代理要求被代理类必须实现接口，而CGLib要求被代理类不能是final类，那么能不能为没有实现接口的final类进行动态代理呢？

答案是不能，但是可以通过反射来实现类似动态代理的功能，只需要将Proxy进行改造即可，如：

```java
public class MyProxy {
    protected InvocationHandler invocationHandler;
    public MyProxy(InvocationHandler invocationHandler) {
		super();
		this.invocationHandler = invocationHandler;
	}
    public static Object newProxyInstance(ClassLoader loader, 
                                          Class<?> clz, InvocationHandler h){
        ...
    }
    public Object invoke(String methodName, Object... args){
        Class<?>[] parameterTypes = getParameterTypes(args);
        Method method = this.getClass().getDeclaredMethod(methodName,parameterTypes);
        return method.invoke(this, args);
    }
}
```

使用时通过 MyProxy 的 invoke 方法实现被代理对象方法的调用：

```java
MyProxy proxy = (MyProxy) MyProxy.newProxyInstance(
    									FinalSubject.class.getClassLoader(),
                                        FinalSubject.class,
										new FinalInvocationHandler(new FinalSubject()));
proxy.invoke("doOperation");
proxy.invoke("other",args);
```

完整项目可以参考本人的 github 小项目  [DynamicProxy ](https://github.com/imhuster/DynamicProxy )