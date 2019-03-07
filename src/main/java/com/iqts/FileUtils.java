package com.iqts;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 文件工具类
 */
public class FileUtils {
	public static void saveResourceCode(String code, String fileName) {
		String fileDir = System.getProperty("DebuggingClassWriter.DEBUG_LOCATION_PROPERTY");
		if (fileDir != null && !fileDir.trim().equals("")) {
			File dir = new File(fileDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			try (FileWriter fileWriter = new FileWriter(new File(fileDir + File.separator + fileName));) {
				fileWriter.write(code);
				fileWriter.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
