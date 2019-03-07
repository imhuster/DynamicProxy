package com.iqts;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * 在内存中编译动态生成class文件
 */
public class DynamicClassUtils {

	private static class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

		private final Map<String, byte[]> classBytes = new HashMap<String, byte[]>();

		MemoryJavaFileManager(JavaFileManager fileManager) {
			super(fileManager);
		}

		public Map<String, byte[]> getClassBytes() {
			return new HashMap<String, byte[]>(this.classBytes);
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
			classBytes.clear();
		}

		@Override
		public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, Kind kind,
				FileObject sibling) throws IOException {
			if (kind == Kind.CLASS) {
				return new MemoryOutputJavaFileObject(className);
			} else {
				return super.getJavaFileForOutput(location, className, kind, sibling);
			}
		}

		JavaFileObject makeStringSource(String name, String code) {
			return new MemoryInputJavaFileObject(name, code);
		}

		static class MemoryInputJavaFileObject extends SimpleJavaFileObject {

			final String code;

			MemoryInputJavaFileObject(String name, String code) {
				super(URI.create("string:///" + name), Kind.SOURCE);
				this.code = code;
			}

			@Override
			public CharBuffer getCharContent(boolean ignoreEncodingErrors) {
				return CharBuffer.wrap(code);
			}
		}

		class MemoryOutputJavaFileObject extends SimpleJavaFileObject {
			final String name;

			MemoryOutputJavaFileObject(String name) {
				super(URI.create("string:///" + name), Kind.CLASS);
				this.name = name;
			}

			@Override
			public OutputStream openOutputStream() {
				return new FilterOutputStream(new ByteArrayOutputStream()) {
					@Override
					public void close() throws IOException {
						out.close();
						ByteArrayOutputStream bos = (ByteArrayOutputStream) out;
						classBytes.put(name, bos.toByteArray());
					}
				};
			}

		}
	}

	private static class MyClassLoader extends URLClassLoader {

		Map<String, byte[]> classBytes = new HashMap<String, byte[]>();

		public MyClassLoader(Map<String, byte[]> classBytes) {
			super(new URL[0], MyClassLoader.class.getClassLoader());
			this.classBytes.putAll(classBytes);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] buf = classBytes.get(name);
			if (buf == null) {
				return super.findClass(name);
			}
			classBytes.remove(name);
			return defineClass(name, buf, 0, buf.length);
		}
	}

	private static class JavaStringCompiler {
		JavaCompiler compiler;
		StandardJavaFileManager stdManager;

		public JavaStringCompiler() {
			this.compiler = ToolProvider.getSystemJavaCompiler();
			this.stdManager = compiler.getStandardFileManager(null, null, null);
		}

		public Map<String, byte[]> compile(String fileName, String source) throws IOException {
			try (MemoryJavaFileManager manager = new MemoryJavaFileManager(stdManager)) {
				JavaFileObject javaFileObject = manager.makeStringSource(fileName, source);
				CompilationTask task = compiler.getTask(null, manager, null, null, null, Arrays.asList(javaFileObject));
				Boolean result = task.call();
				if (result == null || !result.booleanValue()) {
					throw new RuntimeException("Compilation failed.");
				}
				return manager.getClassBytes();
			}
		}

		public Class<?> loadClass(String name, Map<String, byte[]> classBytes)
				throws ClassNotFoundException, IOException {
			try (MyClassLoader classLoader = new MyClassLoader(classBytes)) {
				return classLoader.loadClass(name);
			}
		}
	}

	public static Class<?> generatorDynamicClass(String javaFileName, String fullClassName, String source) {
		Class<?> clz = null;
		JavaStringCompiler compiler = new JavaStringCompiler();
		try {
			Map<String, byte[]> results = compiler.compile(javaFileName, source);
			clz = compiler.loadClass(fullClassName, results);
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return clz;
	}
}