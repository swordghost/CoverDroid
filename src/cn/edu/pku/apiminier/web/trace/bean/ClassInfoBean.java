package cn.edu.pku.apiminier.web.trace.bean;

public class ClassInfoBean {
	private String name;
	private String clzInfo;
	private String methodInfo;
	private String branchInfo;
	private String lineInfo;
	private String insnInfo;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClzInfo() {
		return clzInfo;
	}

	public void setClzInfo(String clzInfo) {
		this.clzInfo = clzInfo;
	}

	public String getMethodInfo() {
		return methodInfo;
	}

	public void setMethodInfo(String methodInfo) {
		this.methodInfo = methodInfo;
	}

	public String getBranchInfo() {
		return branchInfo;
	}

	public void setBranchInfo(String branchInfo) {
		this.branchInfo = branchInfo;
	}

	public String getLineInfo() {
		return lineInfo;
	}

	public void setLineInfo(String lineInfo) {
		this.lineInfo = lineInfo;
	}

	public String getInsnInfo() {
		return insnInfo;
	}

	public void setInsnInfo(String insnInfo) {
		this.insnInfo = insnInfo;
	}
}
