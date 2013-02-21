package com.tf.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class SpecificMethodFinder {
	static int classesTotal;
	static int methodsTotal;
	static int getsetterTotal;

	public static void main(String[] args) throws Exception {
		String methodName = args[1];
		List<JavaClass> classList = new ArrayList<JavaClass>();
		SpecificMethodFinder.findClasses(args[0], classList);
		for (JavaClass jc : classList) {
			if (jc.isAbstract() || jc.isInterface()) {
			} else {
				SpecificMethodFinder.printSpecificMethod(jc, methodName);
			}
		}
		System.out.println("Method Name : " + methodName);
		System.out.println("Total # of classes : "
				+ SpecificMethodFinder.classesTotal);
		System.out.println("Total # of Method call " + methodName + " : "
				+ SpecificMethodFinder.getsetterTotal);
	}

	static void printSpecificMethod(JavaClass jc, String methodName) {
		List<Method> getsetters = new ArrayList<Method>();
		Method[] methods = jc.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (isSpecificMethod(method, methodName)) {
				getsetters.add(method);
			}
		}
		if (getsetters.size() > 0) {
			System.out.println("Class : " + jc.getClassName());
			for (Method m : getsetters) {
				getsetterTotal++;
				System.out.println("\t" + m);
			}
		}
	}

	static void findClasses(String dirOrZipPath, List<JavaClass> methodList)
			throws Exception {
		File file = new File(dirOrZipPath);
		String fName = file.getName().toUpperCase();
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; i++) {
					File f = files[i];
					if (f.isDirectory()) {
						findClasses(f.getCanonicalPath(), methodList);
					} else if (f.isFile()) {
						if (f.getName().endsWith(".class")) {
							String filePath = f.getAbsolutePath();
							classesTotal++;
							parseClass(filePath, methodList);
						}
					}
				}
			}
		} else if (file.isFile()) {
			if (fName.endsWith(".ZIP") || fName.endsWith(".JAR")) {
				readZip(file.getAbsolutePath(), methodList);
			} else {
				throw new IllegalStateException("Not correct form file");
			}
		}
	}

	private static void readZip(String path, List<JavaClass> methodList) {
		try {
			// FileInputStream으로 파일을 읽은 후 ZipInputStream으로 변환
			FileInputStream fis = new FileInputStream(path);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze;
			// ZipEntry가 있는 동안 반복
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.getName().endsWith(".class")) {
					classesTotal++;
					parseClass(path, ze.getName(), methodList);
				}
				zis.closeEntry();
			}
			zis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void parseClass(String zipFilePath, String classFilePath,
			List<JavaClass> methodList) {
		try {
			JavaClass classz = new ClassParser(zipFilePath, classFilePath)
					.parse();
			methodList.add(classz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void parseClass(String classFilePath,
			List<JavaClass> methodList) {
		try {
			JavaClass classz = new ClassParser(classFilePath).parse();
			methodList.add(classz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static String parseGetCode(String codeTxt) {
		String parseResult = "";
		int stringIndexTemp = codeTxt.indexOf("\t") + 1;
		codeTxt = codeTxt.substring(stringIndexTemp);
		stringIndexTemp = codeTxt.indexOf("\n");
		codeTxt = codeTxt.substring(0, stringIndexTemp);
		stringIndexTemp = codeTxt.indexOf("(");
		if (stringIndexTemp > 0) {
			String methodName = codeTxt.substring(0, stringIndexTemp).trim();
			codeTxt = codeTxt.substring(stringIndexTemp);
			stringIndexTemp = codeTxt.indexOf(")") + 1;
			String ParametersName = codeTxt.substring(0, stringIndexTemp);
			parseResult = methodName + parseParameters(ParametersName);
		}
		return parseResult;
	}

	private static String parseParameters(String paramName) {
		String rearStr = paramName;
		List<String> paramList = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		while (true) {
			if (rearStr.contains(";")) {
				int firstSemicolon = rearStr.indexOf(";");
				String frontStr = rearStr.substring(0, firstSemicolon);
				frontStr = frontStr.substring(frontStr.lastIndexOf("/") + 1);
				paramList.add(frontStr);
				rearStr = rearStr.substring(firstSemicolon + 1);
			} else
				break;
		}
		for (int i = 0; i < paramList.size(); i++) {
			if (i == paramList.size() - 1) {
				sb.append(paramList.get(i));
			} else {
				sb.append(paramList.get(i) + ",");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	private static boolean isSpecificMethod(Method m, String methodName) {
		Code c = m.getCode();
		String codeTxt = c.toString();
		boolean result = false;
		while (true) {
			if (codeTxt.contains("invoke")) {
				int start = codeTxt.indexOf("invoke");
				codeTxt = codeTxt.substring(start);
				if (methodName.equals(parseGetCode(codeTxt))) {
					result = true;
				}
				int end = codeTxt.indexOf("(");
				if (end != -1 && start != -1) {
					codeTxt = codeTxt.substring(end);
				} else
					break;
			} else
				break;
		}
		return result;
	}
}
