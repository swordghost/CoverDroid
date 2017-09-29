package cn.edu.pku.apiminier.web.trace.bean;

import java.util.ArrayList;
import java.util.List;

public class SummaryCoverageBean {
	private String name;
	private ClassInfoBean overall;

	private int totalPkgs;
	private int totalClzs;
	private int totalMethods;
	private int totalLines;

	private List<ClassInfoBean> pkgSummary;

	private List<PackageCoverageBean> content;

	public SummaryCoverageBean() {
		this.pkgSummary = new ArrayList<ClassInfoBean>();
		this.content = new ArrayList<PackageCoverageBean>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ClassInfoBean getOverall() {
		return overall;
	}

	public void setOverall(ClassInfoBean overall) {
		this.overall = overall;
	}

	public int getTotalPkgs() {
		return totalPkgs;
	}

	public void setTotalPkgs(int totalPkgs) {
		this.totalPkgs = totalPkgs;
	}

	public int getTotalClzs() {
		return totalClzs;
	}

	public void setTotalClzs(int totalClzs) {
		this.totalClzs = totalClzs;
	}

	public int getTotalMethods() {
		return totalMethods;
	}

	public void setTotalMethods(int totalMethods) {
		this.totalMethods = totalMethods;
	}

	public int getTotalLines() {
		return totalLines;
	}

	public void setTotalLines(int totalLines) {
		this.totalLines = totalLines;
	}

	public List<ClassInfoBean> getPkgSummary() {
		return pkgSummary;
	}

	public void setPkgSummary(List<ClassInfoBean> pkgSummary) {
		this.pkgSummary = pkgSummary;
	}

	public void addPkgSummary(ClassInfoBean bean) {
		this.pkgSummary.add(bean);
	}

	public List<PackageCoverageBean> getContent() {
		return content;
	}

	public void setContent(List<PackageCoverageBean> content) {
		this.content = content;
	}

	public void addContent(PackageCoverageBean bean) {
		this.content.add(bean);
	}

}
