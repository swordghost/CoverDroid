package cn.edu.pku.apiminier.web.trace;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import cn.edu.pku.apiminier.web.trace.bean.ClassInfoBean;
import cn.edu.pku.apiminier.web.trace.bean.CodeCoverageBean;
import cn.edu.pku.apiminier.web.trace.bean.PackageCoverageBean;
import cn.edu.pku.apiminier.web.trace.bean.SummaryCoverageBean;

import com.google.iasgson.Gson;
import com.googlecode.d2j.DexConstants;
import com.googlecode.d2j.DexLabel;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexDebugNode;
import com.googlecode.d2j.node.DexDebugNode.DexDebugOpNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.node.insn.DexLabelStmtNode;
import com.googlecode.d2j.node.insn.DexStmtNode;
import com.googlecode.d2j.node.insn.JumpStmtNode;
import com.googlecode.d2j.node.insn.PackedSwitchStmtNode;
import com.googlecode.d2j.node.insn.SparseSwitchStmtNode;

public class CodeMapFile {
	public Map<Method, CodeMethod> codes;
	private Set<String> clzs;

	public static class InsnCover {
		int insn;
		int insnCover;
		int branch;
		int branchCover;
		int line;
		int lineCover;
		int maxLine;
		int minLine;
	}

	public static class CodeMethod {
		String m;
		long codeSize;
		long dexCodeInsnUnits;
		long calls;
		Method method;
		List<Integer> coverage;

		public void init() {
			// System.out.println("m is:" + m);
			String owner = m.replaceAll(";.*$", ";");
			// System.out.println("owner is:" + owner);
			m = m.replaceFirst("[^;]*;", "");
			String name = m.replaceAll("\\(.*$", "");
			// System.out.println("name is:" + name);
			m = m.replace(name, "");
			String retType = m.replaceAll(".*\\)", "");
			List<String> args = parse(m.replaceAll("\\(", "").replaceAll("\\).*$", ""));
			String[] argsStr = new String[args.size()];
			args.toArray(argsStr);
			method = new Method(owner, name, argsStr, retType);
		}

		private static List<String> parse(String s) {
			// System.out.println("Parse: " + s);
			List<String> ret = new ArrayList<String>();
			for (int i = 0; i < s.length();) {
				int j = i;
				for (; s.charAt(j) == '['; j++)
					;
				if (s.charAt(j) == 'L') {
					for (; s.charAt(j) != ';'; j++) {
					}
				}
				ret.add(s.substring(i, j + 1));
				i = j + 1;
			}
			return ret;
		}

		Method getMethod() {
			if (method == null)
				init();
			return method;
		}

		// public void setMap(String str) {
		// init();
		// map = str;
		// }

		public int getCover(int offset) {
			if (offset >= coverage.size()) {
				System.err.println("method:" + method.toString() + " map:" + coverage.size() + " offset:" + offset);
				return 0;
			}
			return coverage.get(offset);
		}

		public List<Integer> getCover(int offset, int len) {
			return coverage.subList(offset, offset + len);
		}

		public Map<DexLabel, Integer> getLabel2Integer(DexDebugNode debugNode) {
			Map<DexLabel, Integer> ret = new HashMap<DexLabel, Integer>();
			if (debugNode != null) {
				for (DexDebugOpNode node : debugNode.debugNodes) {
					if (node instanceof DexDebugOpNode.LineNumber) {
						DexDebugOpNode.LineNumber line = (DexDebugOpNode.LineNumber) node;
						ret.put(line.label, Integer.valueOf(line.line));
					}
				}
			}
			return ret;
		}

		public InsnCover getInsnCover(DexMethodNode dmn) {
			if (!method.toString().equals(dmn.method.toString()))
				throw new IllegalStateException("Method is not the same!");
			InsnCover ret = new InsnCover();
			ret.insnCover = ret.insn = ret.branch = ret.branchCover = 0;
			if (dmn.codeNode == null || dmn.codeNode.stmts == null || dmn.codeNode.stmts.size() == 0) {
				return ret;
			}

			Map<DexLabel, Integer> label2Integer = getLabel2Integer(dmn.codeNode.debugNode);
			Set<Integer> lines = new HashSet<Integer>();
			int offset = 0;
			ret.branch = ret.branchCover = 1;
			for (int i = 0; i < dmn.codeNode.stmts.size(); i++) {
				DexStmtNode stmt = dmn.codeNode.stmts.get(i);
				int c = getCover(offset);
				if (c != 0 && (stmt instanceof DexLabelStmtNode)
						&& label2Integer.containsKey(((DexLabelStmtNode) stmt).label)) {
					lines.add(label2Integer.get(((DexLabelStmtNode) stmt).label));
				}
				if (stmt != null && stmt.op != null && c != 0) {
					ret.insnCover++;
					if (stmt instanceof PackedSwitchStmtNode) {
						PackedSwitchStmtNode pssn = (PackedSwitchStmtNode) stmt;
						ret.branch += pssn.labels.length;
						List<Integer> cover = getCover(pssn.offset, pssn.labels.length);
						ret.branchCover += countNZero(cover);
					} else if (stmt instanceof SparseSwitchStmtNode) {
						SparseSwitchStmtNode sssn = (SparseSwitchStmtNode) stmt;
						ret.branch += sssn.cases.length + 1;
						List<Integer> cover = getCover(sssn.offset, sssn.cases.length + 1);
						ret.branchCover += countNZero(cover);
					} else if (stmt instanceof JumpStmtNode) {
						if (stmt.op.name().contains("IF")) {
							ret.branch += 2;
							int cover2 = getCover(offset + 1);
							ret.branchCover++;
							if (c != cover2 && cover2 != 0)
								ret.branchCover++;
						}
					}
				}
				if (stmt != null && stmt.op != null) {
					ret.insn++;
					offset += stmt.op.format.size;
				}
			}
			ret.line = label2Integer.values().size();
			ret.lineCover = lines.size();
			ret.maxLine = -1;
			ret.minLine = -1;
			for (Integer i : label2Integer.values()) {
				if (ret.maxLine == -1 || ret.maxLine < i)
					ret.maxLine = i;
				if (ret.minLine == -1 || ret.minLine > i)
					ret.minLine = i;
			}
			return ret;
		}

		private int countNZero(List<Integer> cover) {
			int ret = 0;
			for (int i = 0; i < cover.size(); i++) {
				if (cover.get(i) != 0)
					ret++;
			}
			return ret;
		}
	}

	public CodeMapFile(String file) {
		try {
			clzs = new HashSet<String>();
			codes = new HashMap<Method, CodeMethod>();
			FileInputStream fin = new FileInputStream(file);
			Scanner sc = new Scanner(fin);
			Gson gson = new Gson();
			for (; sc.hasNextLine();) {
				CodeMethod temp = gson.fromJson(sc.nextLine(), CodeMethod.class);
				codes.put(temp.getMethod(), temp);
				clzs.add(temp.getMethod().getOwner());
			}
			sc.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String str = "{\"m\": \"Lcom/tencent/qqmusicplayerprocess/audio/playlist/w;<init>(Lcom/tencent/qqmusicplayerprocess/audio/playlist/u;)V\", \"dexCodeInsnUnits\":4, \"calls\":1, \"coverage\": [1,0,0,1,0]}";
		CodeMethod temp = new Gson().fromJson(str, CodeMethod.class);
		temp.init();
		System.out.println(temp.getMethod().toString());
	}

	public boolean hasCover(DexMethodNode dmn) {
		CodeMethod cm = codes.get(dmn.method);
		if (cm == null) {
			return false;
		}
		return true;
	}

	public void printCover(DexMethodNode dmn) {
		if (!hasCover(dmn)) {
			System.out.println("No cover");
			return;
		}
		CodeMethod cm = codes.get(dmn.method);
		System.out.println("Map:" + cm.coverage.size());
		CodeCoverageBean bean = new CodeCoverageBean();
		DexCodePrinter dcp = new DexCodePrinter(true, bean, cm);
		if (cm != null && dmn.codeNode != null) {
			int offset = 0;
			for (int i = 0; i < dmn.codeNode.stmts.size(); i++) {
				DexStmtNode stmt = dmn.codeNode.stmts.get(i);
				ByteArrayOutputStream bo = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(bo);
				ps.print(String.format("0x%04x", offset));
				ps.print(" cover:");
				if (stmt instanceof PackedSwitchStmtNode) {
					PackedSwitchStmtNode pssn = (PackedSwitchStmtNode) stmt;
					ps.print(cm.getCover(offset) + " branch:" + cm.getCover(pssn.offset, pssn.labels.length));
				} else if (stmt instanceof SparseSwitchStmtNode) {
					SparseSwitchStmtNode sssn = (SparseSwitchStmtNode) stmt;
					ps.print(cm.getCover(offset) + " branch:" + cm.getCover(sssn.offset, sssn.cases.length + 1));
				} else if (stmt instanceof JumpStmtNode) {
					ps.print(cm.getCover(offset, 2));
				} else
					ps.print(cm.getCover(offset));
				ps.print(" ");
				if (stmt != null && stmt.op != null)
					offset += stmt.op.format.size;
				stmt.accept(dcp);
				System.out.print(bo.toString());
			}
		}
	}

	public static void main2(String[] args) {
		String path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1478428460_5735_0.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493715752_3452_0.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493715752_3452_0.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493799383_27627_1.trace";
		// path =
		// "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493714760_1309_5.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493828895_4739_0.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493831691_5047_1.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493831780_5047_2.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1500455253_12702_2.trace";
		// path =
		// "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493834202_4855_1.trace";
		CodeMapFile temp = new CodeMapFile(path);
		String apkPath = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/com.android.calculator2_5.1.1-eng.root.20170407.155435.zip";
		apkPath = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/com.google.android.apps.calculator_.zip";
		DexZipReader dzp = new DexZipReader(apkPath);
		temp.printAll(dzp, System.out);

		System.exit(0);
	}

	private void printAll(DexZipReader dzp, PrintStream ps) {
		Map<String, CoverReport> reports = calculateReport(dzp);
		CoverReport total = mergeCoverReport(reports);
		ps.println("OVERALL COVERAGE SUMMARY:\n");
		ps.println("[name]\t[class,%]\t[method,%]\t[branch,%]\t[line,%]\t[insn,%]");
		ps.println("all classes\t" + total.prettyString());
		ps.println("\nOVERALL STATS SUMMARY:\n");
		ps.println("total packages:" + reports.keySet().size());
		ps.println("total classes:" + total.clz);
		ps.println("total method:" + total.method);
		ps.println("total executable lines:" + total.line);
		printCoverageByPackage(reports, ps);
	}

	public void printClz(DexClassNode dcn, DexZipReader.PlainClass pc) {
		CoverReport clzReport = new CoverReport();
		if (dcn.methods != null) {
			for (DexMethodNode dmn : dcn.methods) {
				if (dmn.codeNode != null) {
					CoverReport methodReport = new CoverReport();
					clzReport.method++;
					methodReport.method = 1;
					if (hasCover(dmn)) {
						clzReport.methodCover++;
						methodReport.methodCover = 1;
						InsnCover cover = codes.get(dmn.method).getInsnCover(dmn);
						clzReport.insn += cover.insn;
						methodReport.insn = cover.insn;
						clzReport.insnCover += cover.insnCover;
						methodReport.insnCover = cover.insnCover;
						clzReport.branch += cover.branch;
						methodReport.branch = cover.branch;
						clzReport.branchCover += cover.branchCover;
						methodReport.branchCover = cover.branchCover;
						if (cover.maxLine >= 0) {
							clzReport.line += cover.maxLine - cover.minLine + 1;
							methodReport.line = cover.maxLine - cover.minLine + 1;
							clzReport.lineCover += cover.lineCover;
							methodReport.lineCover = cover.lineCover;
						}
					}

					ClassInfoBean bean = new ClassInfoBean();
					methodReport.fillClassInfoBean(bean,
							DexZipReader.getMethodAccess(dmn.access) + dmn.method.getName());
					pc.addMethodReport(bean);
				}
			}
		}
		clzReport.fillClassInfoBean(pc.getClassSummary(), pc.className);
	}

	private Map<String, CoverReport> calculateReport(DexZipReader dzp) {
		Map<String, CoverReport> reports = new HashMap<String, CoverReport>();
		for (DexClassNode dcn : dzp.dexCache.values()) {
			if ((dcn.access & DexConstants.ACC_INTERFACE) != 0)
				continue;
			String pkg = dcn.className.replaceAll("[^//]*;", "");
			if (!reports.containsKey(pkg))
				reports.put(pkg, new CoverReport());
			CoverReport report = reports.get(pkg);
			report.clz++;
			int clzCover = 0;
			if (dcn.methods != null)
				for (DexMethodNode dmn : dcn.methods) {
					if (dmn.codeNode != null) {
						report.method++;
						if (hasCover(dmn)) {
							clzCover = 1;
							report.methodCover++;
							InsnCover cover = codes.get(dmn.method).getInsnCover(dmn);
							report.insn += cover.insn;
							report.insnCover += cover.insnCover;
							report.branch += cover.branch;
							report.branchCover += cover.branchCover;
							if (cover.maxLine >= 0) {
								report.line += cover.maxLine - cover.minLine + 1;
								report.lineCover += cover.lineCover;
							}
						}
					}
				}
			report.clzCover += clzCover;
		}
		return reports;
	}

	public void printAll(DexZipReader dzp, SummaryCoverageBean summary) {
		final Map<String, Map<String, CoverReport>> pkg2clzInfo = calculateReportForPkg(dzp);
		summary.setName("Summary");
		CoverReport summaryReport = mergeSummaryReport(pkg2clzInfo);
		ClassInfoBean overallBean = new ClassInfoBean();
		summaryReport.fillClassInfoBean(overallBean, "all classes");
		summary.setOverall(overallBean);

		summary.setTotalPkgs(pkg2clzInfo.keySet().size());
		summary.setTotalClzs(summaryReport.clz);
		summary.setTotalMethods(summaryReport.method);
		summary.setTotalLines(summaryReport.line);

		List<String> arr = new ArrayList<String>();
		arr.addAll(pkg2clzInfo.keySet());
		Collections.sort(arr, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if (pkg2clzInfo.get(o1).get("summary").clzCover != 0
						^ pkg2clzInfo.get(o2).get("summary").clzCover != 0) {
					return pkg2clzInfo.get(o1).get("summary").clzCover > 0 ? -1 : 1;
				} else {
					return o1.compareTo(o2);
				}
			}
		});

		for (String pkgName : arr) {
			final Map<String, CoverReport> clzMp = pkg2clzInfo.get(pkgName);
			PackageCoverageBean pkgBean = new PackageCoverageBean();
			pkgBean.setName(formatPkgName(pkgName));
			ClassInfoBean pkgSummaryBean = new ClassInfoBean();
			clzMp.get("summary").fillClassInfoBean(pkgSummaryBean, formatPkgName(pkgName));
			summary.addPkgSummary(pkgSummaryBean);
			pkgBean.setPkgInfo(pkgSummaryBean);
			clzMp.remove("summary");

			List<String> arr2 = new ArrayList<String>();
			arr2.addAll(clzMp.keySet());
			Collections.sort(arr2, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					if (clzMp.get(o1).clzCover != 0 ^ clzMp.get(o2).clzCover != 0) {
						return clzMp.get(o1).clzCover > 0 ? -1 : 1;
					} else {
						return o1.compareTo(o2);
					}
				}
			});
			for (String clzName : arr2) {
				ClassInfoBean clzBean = new ClassInfoBean();
				clzMp.get(clzName).fillClassInfoBean(clzBean, formatPkgName(clzName));
				pkgBean.addContent(clzBean);
			}

			summary.addContent(pkgBean);
		}

	}

	private Map<String, Map<String, CoverReport>> calculateReportForPkg(DexZipReader dzp) {
		Map<String, Map<String, CoverReport>> reports = new HashMap<String, Map<String, CoverReport>>();
		for (DexClassNode dcn : dzp.dexCache.values()) {
			if ((dcn.access & DexConstants.ACC_INTERFACE) != 0)
				continue;
			String pkg = dcn.className.replaceAll("[^//]*;", "");
			if (!reports.containsKey(pkg)) {
				reports.put(pkg, new HashMap<String, CoverReport>());
			}
			Map<String, CoverReport> clzReports = reports.get(pkg);
			if (!clzReports.containsKey("summary")) {
				clzReports.put("summary", new CoverReport());
			}
			CoverReport pkgReport = clzReports.get("summary");
			String clzName = dcn.className;
			if (!clzReports.containsKey(clzName)) {
				clzReports.put(clzName, new CoverReport());
			}
			CoverReport clzReport = clzReports.get(clzName);
			pkgReport.clz++;
			clzReport.clz++;
			int clzCover = 0;
			if (dcn.methods != null) {
				for (DexMethodNode dmn : dcn.methods) {
					if (dmn.codeNode != null) {
						pkgReport.method++;
						clzReport.method++;
						if (hasCover(dmn)) {
							clzCover = 1;
							pkgReport.methodCover++;
							clzReport.methodCover++;
							InsnCover cover = codes.get(dmn.method).getInsnCover(dmn);
							pkgReport.insn += cover.insn;
							clzReport.insn += cover.insn;
							pkgReport.insnCover += cover.insnCover;
							clzReport.insnCover += cover.insnCover;
							pkgReport.branch += cover.branch;
							clzReport.branch += cover.branch;
							pkgReport.branchCover += cover.branchCover;
							clzReport.branchCover += cover.branchCover;
							if (cover.maxLine >= 0) {
								pkgReport.line += cover.maxLine - cover.minLine + 1;
								clzReport.line += cover.maxLine - cover.minLine + 1;
								pkgReport.lineCover += cover.lineCover;
								clzReport.lineCover += cover.lineCover;
							}
						}
					}
				}
			}
			pkgReport.clzCover += clzCover;
			clzReport.clzCover += clzCover;
		}
		return reports;
	}

	private static CoverReport mergeCoverReport(Map<String, CoverReport> reports) {
		CoverReport total = new CoverReport();
		for (CoverReport report : reports.values()) {
			total.clz += report.clz;
			total.clzCover += report.clzCover;
			total.branch += report.branch;
			total.branchCover += report.branchCover;
			total.insn += report.insn;
			total.insnCover += report.insnCover;
			total.line += report.line;
			total.lineCover += report.lineCover;
			total.method += report.method;
			total.methodCover += report.methodCover;
		}
		return total;
	}

	private static CoverReport mergeSummaryReport(Map<String, Map<String, CoverReport>> reports) {
		CoverReport total = new CoverReport();
		for (Map<String, CoverReport> pkgReport : reports.values()) {
			CoverReport report = pkgReport.get("summary");
			total.clz += report.clz;
			total.clzCover += report.clzCover;
			total.branch += report.branch;
			total.branchCover += report.branchCover;
			total.insn += report.insn;
			total.insnCover += report.insnCover;
			total.line += report.line;
			total.lineCover += report.lineCover;
			total.method += report.method;
			total.methodCover += report.methodCover;
		}
		return total;
	}

	private static void printCoverageByPackage(Map<String, CoverReport> reports, PrintStream ps) {
		ps.println("\nCOVERAGE BREAKDOWN BY PACKAGE:\n");
		ps.println("[name]\t[class,%]\t[method,%]\t[branch,%]\t[line,%]\t[insn,%]");
		List<String> arr = new ArrayList<String>();
		arr.addAll(reports.keySet());
		Collections.sort(arr, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return o1.compareTo(o2);
			}
		});
		for (String key : arr) {
			CoverReport report = reports.get(key);
			ps.println(formatPkgName(key) + "\t" + report.prettyString());
		}
		return;
	}

	private static String formatPkgName(String key) {
		if (key.length() == 0)
			return "default";
		key = key.substring(1, key.length() - 1);
		key = key.replace("/", ".");
		return key;
	}

	private static double div(int a, int b) {
		if (b == 0)
			return 0;
		return ((double) a * 100) / (double) b;
	}

	public static class CoverReport {
		int clz;
		int clzCover;
		int method;
		int methodCover;
		int insn;
		int insnCover;
		int branch;
		int branchCover;
		int line;
		int lineCover;

		public String prettyString() {
			return String.format("%.0f%% (%d/%d)	%.0f%% (%d/%d)	%.0f%% (%d/%d)	%.0f%% (%d/%d)	%.0f%% (%d/%d)",
					div(clzCover, clz), clzCover, clz, div(methodCover, method), methodCover, method,
					div(branchCover, branch), branchCover, branch, div(lineCover, line), lineCover, line,
					div(insnCover, insn), insnCover, insn);
		}

		public void fillClassInfoBean(ClassInfoBean bean, String name) {
			bean.setName(name);
			bean.setClzInfo(String.format("%.0f%% (%d/%d)", div(clzCover, clz), clzCover, clz));
			bean.setMethodInfo(String.format("%.0f%% (%d/%d)", div(methodCover, method), methodCover, method));
			bean.setBranchInfo(String.format("%.0f%% (%d/%d)", div(branchCover, branch), branchCover, branch));
			bean.setLineInfo(String.format("%.0f%% (%d/%d)", div(lineCover, line), lineCover, line));
			bean.setInsnInfo(String.format("%.0f%% (%d/%d)", div(insnCover, insn), insnCover, insn));
		}
	}

	public Set<String> getMethodStrSet() {
		Set<String> ret = new HashSet<String>();
		for (Method m : codes.keySet()) {
			ret.add(m.toString());
		}
		return ret;
	}

	public boolean coverClz(String key) {
		return clzs.contains(key);
	}
}