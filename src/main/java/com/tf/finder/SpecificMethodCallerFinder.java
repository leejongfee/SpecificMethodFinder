package com.tf.finder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;

public class SpecificMethodCallerFinder {
	static int classesTotal;
	static int methodTotal;

	public static void main(String[] args) throws Exception {
		String methodName = setPackageName(args[1]);
		List<JavaClass> classList = new ArrayList<JavaClass>();
		SpecificMethodCallerFinder.findClasses(args[0], classList);
		for (JavaClass jc : classList) {
			if (!jc.isInterface()) {
				SpecificMethodCallerFinder.printSpecificMethod(jc, methodName);
			}
		}
		System.out.println("Method Name : " + methodName);
		System.out.println("Total # of classes : "
				+ SpecificMethodCallerFinder.classesTotal);
		System.out.println("Total # of Method call " + methodName + " : "
				+ SpecificMethodCallerFinder.methodTotal);
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
		methodName = methodName.substring(0, front - 1) + sb.toString();
		return methodName;
	}

	static String changePrimitiveVar(String var) {
		if (!var.contains(".") && !var.contains("[")) {
			if (!var.equals("int") && !var.equals("long")
					&& !var.equals("double") && !var.equals("float")
					&& !var.equals("short") && !var.equals("char")
					&& !var.equals("byte") && !var.equals("boolean")) {
				var = "java.lang." + var;
			}
		}
		return var;
	}

	static void printSpecificMethod(JavaClass jc, String methodName) {
		List<parsingMethodInfo> getsetters = new ArrayList<parsingMethodInfo>();
		Method[] methods = jc.getMethods();
		int invokeIndex = 0;
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (!method.isAbstract()) {
				invokeIndex = isSpecificMethod(method, methodName);
				if (invokeIndex > 0) {
					parsingMethodInfo pi = new parsingMethodInfo();
					pi.invokeIndex = invokeIndex;
					pi.jc = jc;
					pi.method = method;
					getsetters.add(pi);
				}
			}
		}
		if (getsetters.size() > 0) {
			System.out.println("Class : " + jc.getClassName());
			for (parsingMethodInfo p : getsetters) {
				methodTotal++;
				System.out.println("\t" + p.method);
				editMethod(p);
			}
		}
	}

	static void editMethod(parsingMethodInfo pi) {
		JavaClass newJc;
		try {
			newJc = new ClassParser(
					"C:/Users/jhLee/workspace/Sample/bin/com/tf/android/GraphicsFactory.class")
					.parse();
			Method newMethod = newJc.getMethods()[1];
			JavaClass jc = pi.jc;
			Method method = pi.method;
			int invokIndex = pi.invokeIndex;
			int methodIndex = getMethodIndex(jc.getMethods(), method);
			ClassGen cg = new ClassGen(jc);
			ConstantPoolGen cp = new ConstantPoolGen(jc.getConstantPool());
			MethodGen mg = new MethodGen(method, jc.getClassName(), cp);
			InstructionList il = mg.getInstructionList();
			InstructionHandle ih = il.findHandle(invokIndex);
			InstructionFactory factory = new InstructionFactory(cg);
			ih.setInstruction(factory.createInvoke(newJc.getClassName(),
					newMethod.getName(), newMethod.getReturnType(),
					newMethod.getArgumentTypes(), Constants.INVOKESTATIC));
			mg.setInstructionList(il);
			cg.setMethodAt(mg.getMethod(), methodIndex);
			jc = cg.getJavaClass();
			jc.dump(classNameParse(jc.getClassName()) + ".class");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static int getMethodIndex(Method[] methods, Method m) {
		int methodIndex = 0;
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].equals(m)) {
				methodIndex = i;
			}
		}
		return methodIndex;
	}

	static String classNameParse(String className) {
		int lastDot = className.lastIndexOf(".") + 1;
		if (lastDot > 0) {
			className = className.substring(lastDot);
		}
		return className;
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
		// System.out.println(parseResult);
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
				} else if (rearStr.startsWith("[")) {
					sb.append(changeArrayTypeName(rearStr));
					sb.append(",");
					rearStr = rearStr.substring(2);
				} else if (!rearStr.startsWith(")")) {
					sb.append(changeTypeName(rearStr.substring(0, 1)));
					sb.append(",");
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

	private static String changeArrayTypeName(String paramName) {
		paramName = changeTypeName(paramName.substring(1, 2)) + "[]";
		return paramName;
	}

	private static String changeTypeName(String paramName) {
		paramName = paramName.replaceAll("I", "int");
		paramName = paramName.replaceAll("B", "byte");
		paramName = paramName.replaceAll("C", "char");
		paramName = paramName.replaceAll("D", "double");
		paramName = paramName.replaceAll("F", "float");
		paramName = paramName.replaceAll("J", "long");
		paramName = paramName.replaceAll("S", "short");
		paramName = paramName.replaceAll("Z", "boolean");
		return paramName;
	}

	private static int isSpecificMethod(Method m, String methodName) {
		Code c = m.getCode();
		int result = 0;
		if (c != null) {
			String codeTxt = c.toString();
			while (true) {
				if (codeTxt.contains("invoke")) {
					int start = codeTxt.indexOf("invoke");
					String codeTemp = codeTxt.substring(start - 6);
					codeTxt = codeTxt.substring(start);
					if (methodName.equals(parseGetCode(codeTxt))) {
						result = getInvokeIndex(codeTemp);
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

	private static int getInvokeIndex(String code) {

		int index = code.indexOf("invoke");
		code = code.substring(0, index);
		index = code.indexOf(":");
		if (index > 0) {
			index = Integer.parseInt(code.substring(0, index));
		}
		return index;
	}

	static class parsingMethodInfo {
		JavaClass jc;
		Method method;
		int invokeIndex;
	}
}
