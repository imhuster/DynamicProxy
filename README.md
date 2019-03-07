# DynamicProxy

本项目通过使用反射及动态编译技术实现了动态代理，较常规的基于 interface 的 JDK 动态代理与基于类的 CGLib 动态代理，本项目对无接口的final类实现了类似动态代理的功能。

通过本项目，您能够对 Java 动态代理机制有更深的认识。

[介绍博客](doc/代理模式到动态代理AOP.md)

参考:

本项目动态编译部分参考了 [jscc](https://github.com/verhas/jscc)

反编译工具 [Java Decompiler](http://java-decompiler.github.io/)
