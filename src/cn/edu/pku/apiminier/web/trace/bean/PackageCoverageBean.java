package cn.edu.pku.apiminier.web.trace.bean;

import java.util.ArrayList;
import java.util.List;

public class PackageCoverageBean {
	private String name;
	private ClassInfoBean pkgInfo;
	private List<ClassInfoBean> content;

	public PackageCoverageBean() {
		this.content = new ArrayList<ClassInfoBean>();
	};

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ClassInfoBean getPkgInfo() {
		return pkgInfo;
	}

	public void setPkgInfo(ClassInfoBean pkgInfo) {
		this.pkgInfo = pkgInfo;
	}

	public List<ClassInfoBean> getContent() {
		return content;
	}

	public void setContent(List<ClassInfoBean> content) {
		this.content = content;
	}

	public void addContent(ClassInfoBean bean) {
		this.content.add(bean);
	}

}
