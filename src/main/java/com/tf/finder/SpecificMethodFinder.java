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
	static int methodTotal;

	public static void main(String[] args) throws Exception {
		String methodName = setPackageName(args[1]);
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
				+ SpecificMethodFinder.methodTotal);
	}

	static String setPackageName(String methodName) {
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		int front = methodName.indexOf("(") + 1;
		int rear = methodName.indexOf(")");
		String paramNames = methodName.substring(front, rear);
		if (paramNames.length() != 0) {
			while (true) {
				rear = paramNames.indexOf(",");
				if (rear > 0) {
					sb.append(changePrimitiveVar(paramNames.substring(0, rear)));
					sb.append(",");
					rear = paramNames.indexOf(",");
					paramNames = paramNames.substring(rear + 1);
				} else {
					sb.append(changePrimitiveVar(paramNames));
					break;
				}
			}
		}
		sb.append(")");
		methodName = methodName.substring(0,front-1)+sb.toString();
		return methodName;
	}

	static String changePrimitiveVar(String var) {
		if (!var.contains(".")) {
			if (!var.equals("int") && !var.equals("long")
					&& !var.equals("double") && !var.equals("float")
					&& !var.equals("short") && !var.equals("char")
					&& !var.equals("byte")&&!var.equals("boolean")) {
				var = "java.lang."+var;
			}
		}
		return var;
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
				methodTotal++;
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
			FileInputStream fis = new FileInputStream(path);
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze;
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
		String parseResult = null;
		int stringIndexTemp = codeTxt.indexOf("\t") + 1;
		codeTxt = codeTxt.substring(stringIndexTemp);
		stringIndexTemp = codeTxt.indexOf("\n");
		codeTxt = codeTxt.substring(0, stringIndexTemp);
		stringIndexTemp = codeTxt.indexOf("(");
		if (stringIndexTemp > 0) {
			String methodName = codeTxt.substring(0, stringIndexTemp).trim();
			methodName = changeInitName(methodName);
			codeTxt = codeTxt.substring(stringIndexTemp);
			stringIndexTemp = codeTxt.indexOf(")") + 1;
			String ParametersName = codeTxt.substring(0, stringIndexTemp);
			parseResult = methodName + parseParameters(ParametersName);
		}
		return parseResult;
	}

	private static String changeInitName(String methodName) {
		int stringIndexTemp = methodName.lastIndexOf(".") + 1;
		if (methodName.substring(stringIndexTemp).equals("<init>")) {
			String lastStr = methodName.substring(0, stringIndexTemp - 1);
			stringIndexTemp = lastStr.lastIndexOf(".") + 1;
			methodName = lastStr + "." + lastStr.substring(stringIndexTemp);
		}
		return methodName;
	}

	private static String parseParameters(String paramName) {
		String rearStr = paramName.substring(1);
		StringBuffer sb = new StringBuffer();
		sb.append("(");
		while (true) {
			if (rearStr.length() > 1) {
				if (rearStr.startsWith("L")) {
					int semiColon = rearStr.indexOf(";");
					sb.append(rearStr.substring(1, semiColon).replace("/", "."));
					sb.append(",");
					rearStr = rearStr.substring(semiColon + 1);
				} else if (!rearStr.startsWith(")")) {
					sb.append(changeTypeName(rearStr.substring(0, 1)));
					rearStr = rearStr.substring(1);
				}
			} else {
				if (sb.lastIndexOf(",") != -1) {
					sb.deleteCharAt(sb.lastIndexOf(","));
				}
				break;
			}
		}
		sb.append(")");
		return sb.toString();
	}

	private static String changeTypeName(String paramName) {
		paramName = paramName.replaceAll("I", "int,");
		paramName = paramName.replaceAll("B", "byte,");
		paramName = paramName.replaceAll("C", "char,");
		paramName = paramName.replaceAll("D", "double,");
		paramName = paramName.replaceAll("F", "float,");
		paramName = paramName.replaceAll("J", "long,");
		paramName = paramName.replaceAll("S", "short,");
		paramName = paramName.replaceAll("Z", "boolean,");
		return paramName;
	}

	private static boolean isSpecificMethod(Method m, String methodName) {
		Code c = m.getCode();
		boolean result = false;
		if (c != null) {
			String codeTxt = c.toString();
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
		}
		return result;
	}
}
