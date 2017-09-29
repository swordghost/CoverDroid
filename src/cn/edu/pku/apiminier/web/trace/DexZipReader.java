package cn.edu.pku.apiminier.web.trace;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cn.edu.pku.apiminier.web.trace.CodeMapFile.CodeMethod;
import cn.edu.pku.apiminier.web.trace.bean.ClassInfoBean;
import cn.edu.pku.apiminier.web.trace.bean.CodeCoverageBean;
import cn.edu.pku.apiminier.web.trace.bean.CodeCoverageBean.Line;
import cn.edu.pku.apiminier.web.trace.bean.SummaryCoverageBean;

import com.google.iasgson.Gson;
import com.googlecode.d2j.DexConstants;
import com.googlecode.d2j.node.DexClassNode;
import com.googlecode.d2j.node.DexCodeNode;
import com.googlecode.d2j.node.DexFileNode;
import com.googlecode.d2j.node.DexMethodNode;
import com.googlecode.d2j.node.insn.DexLabelStmtNode;
import com.googlecode.d2j.node.insn.DexStmtNode;
import com.googlecode.d2j.node.insn.JumpStmtNode;
import com.googlecode.d2j.node.insn.PackedSwitchStmtNode;
import com.googlecode.d2j.node.insn.SparseSwitchStmtNode;
import com.googlecode.d2j.reader.DexFileReader;
import com.googlecode.d2j.visitors.DexDebugVisitor;

public class DexZipReader {
	public static String TraceDir = "./trace";
	public static TraceCache<DexZipReader> global = getTraceCache();
	Map<String, DexClassNode> dexCache;
	Map<String, String> clzName2File;
	static {
		File dir = new File(TraceDir);
		if (!dir.exists()) {
			dir.mkdir();
		}
		TraceDir = dir.getAbsolutePath() + File.separator;
		System.out.println("[DexZipReader] trace dir = " + TraceDir);
	}

	public static class PlainClass {
		public PlainClass(String clz) {
			className = clz;
			classSummary = new ClassInfoBean();
			methodReports = new ArrayList<ClassInfoBean>();
			methods = new ArrayList<PlainMethod>();
		}

		String className;
		ClassInfoBean classSummary;
		List<ClassInfoBean> methodReports;
		List<PlainMethod> methods;

		public void addMethod(PlainMethod pm) {
			methods.add(pm);
		}

		public ClassInfoBean getClassSummary() {
			return this.classSummary;
		}

		public void addMethodReport(ClassInfoBean methodReport) {
			this.methodReports.add(methodReport);
		}
	}

	public static class PlainMethod {
		String methodName;
		CodeCoverageBean code;

		// String code;
		// transient ByteArrayOutputStream bout;

		PlainMethod(String methodName) {
			this.methodName = methodName;
			code = new CodeCoverageBean();
			// bout = new ByteArrayOutputStream();
		}

	}

	public static List<String> lsDir(String succ) {
		File[] list = new File(TraceDir).listFiles();

		List<File> ret = new ArrayList<File>();
		for (File f : list) {
			if (f.getName().endsWith(succ)) {
				ret.add(f);
			}
		}
		Collections.sort(ret, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return (int) ((o2.lastModified() - o1.lastModified()) > 0 ? 1 : -1);
			}
		});
		List<String> strs = new ArrayList<String>();
		for (File f : ret)
			strs.add(f.getName());
		return strs;
	}

	public DexZipReader(String path) {
		dexCache = new HashMap<String, DexClassNode>();
		clzName2File = new HashMap<String, String>();
		try {
			ZipInputStream zin = new ZipInputStream(new FileInputStream(path));
			for (ZipEntry ze; (ze = zin.getNextEntry()) != null;) {
				if (!ze.getName().endsWith(".dex"))
					continue;
				DexFileReader dfr = new DexFileReader(zin);
				DexFileNode dfn = new DexFileNode();
				dfr.accept(dfn);
				for (DexClassNode dcn : dfn.clzs) {
					dexCache.put(dcn.className, dcn);
					clzName2File.put(dcn.className, ze.getName());
				}
			}
			zin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String queryMethod(String clz, CodeMapFile codeMapFile) {
		clz = clz.replaceAll("\\.", "/");
		if (clz.isEmpty() || !clz.substring(0, 1).equals("L")) {
			clz = "L" + clz;
		}
		if (!clz.endsWith(";")) {
			clz = clz + ";";
		}
		DexClassNode dcn = dexCache.get(clz);
		if (dcn != null) {
			return new Gson().toJson(parseClassNode(dcn, codeMapFile));
		} else {
			return new Gson().toJson(getCoverageSummary(codeMapFile));
		}
	}

	private SummaryCoverageBean getCoverageSummary(CodeMapFile codeMapFile) {
		SummaryCoverageBean summary = new SummaryCoverageBean();
		codeMapFile.printAll(this, summary);
		return summary;
	}

	// TODO
	private PlainClass getPackageCoverage(String pkgName, CodeMapFile codeMapFile) {
		pkgName = pkgName.replace(";", "");
		PlainClass pc = new PlainClass(pkgName.substring(1).replaceAll("/", "."));
		for (String key : dexCache.keySet()) {
			if (key.startsWith(pkgName) && codeMapFile.coverClz(key)) {
				key = "Covered " + key;
				PlainMethod pm = new PlainMethod(key);
				pc.addMethod(pm);
			}
		}
		return pc;
	}

	private PlainClass parseClassNode(DexClassNode dcn, CodeMapFile codeMapFile) {
		PlainClass pc = new PlainClass(dcn.className.substring(1, dcn.className.length() - 1).replace("/", "."));
		// + "@" + clzName2File.get(dcn.className)
		codeMapFile.printClz(dcn, pc);

		if (dcn.methods != null) {
			for (DexMethodNode dmn : dcn.methods) {
				CodeMethod cover = codeMapFile.codes.get(dmn.method);
				String pre = cover == null ? "" : "Covered ";
				// PlainMethod pm = new PlainMethod(pre +
				// getMethodAccess(dmn.access) + dmn.method.toString());
				PlainMethod pm = new PlainMethod(pre + getMethodAccess(dmn.access) + dmn.method.getName());

				System.out.println("[DexZipReader] parse method:" + dmn.method.toString() + " codeisNULL:"
						+ codeMapFile.codes.containsKey(dmn.method));
				DexCodePrinter dcp = new DexCodePrinter(true, pm.code, cover);
				if (dmn.codeNode != null) {
					acceptCodeNode(dmn.codeNode, dcp, cover);
					pc.addMethod(pm);
				}
			}
		}
		return pc;
	}

	private void acceptCodeNode(DexCodeNode codeNode, DexCodePrinter dcp, CodeMethod cover) {
		if (codeNode.debugNode != null) {
			DexDebugVisitor ddv = dcp.visitDebug();
			if (ddv != null) {
				codeNode.debugNode.accept(ddv);
				ddv.visitEnd();
			}
		}
		if (codeNode.totalRegister >= 0) {
			dcp.visitRegister(codeNode.totalRegister);
		}
		for (int j = 0; j < codeNode.stmts.size(); j++) {
			DexStmtNode n = codeNode.stmts.get(j);
			n.accept(dcp);
			if (cover == null)
				continue;
			if (n instanceof DexLabelStmtNode) {
				DexStmtNode next = null;
				if (j + 1 < codeNode.stmts.size()) {
					next = codeNode.stmts.get(j + 1);
				}
				if (next != null && next instanceof JumpStmtNode && next.op.name().contains("IF")) {
					System.out.println("[Debug]DexZipReader owner:" + cover.getMethod().getOwner());
					Line line = dcp.bean.content.get(dcp.bean.content.size() - 1);
					line.coverInfo = (Integer.valueOf(line.coverInfo) + cover.getCover(dcp.offset + 1)) + "";
				}
			}
			if (n instanceof PackedSwitchStmtNode) {
				PackedSwitchStmtNode switchStmt = (PackedSwitchStmtNode) n;
				List<Integer> covers = cover.getCover(switchStmt.offset, switchStmt.labels.length + 1);
				Line line = dcp.bean.content.get(dcp.bean.content.size() - 1);
				for (Integer i : covers)
					line.coverInfo += " " + i;
			}
			if (n instanceof SparseSwitchStmtNode) {
				SparseSwitchStmtNode switchStmt = (SparseSwitchStmtNode) n;
				List<Integer> covers = cover.getCover(switchStmt.offset, switchStmt.labels.length + 1);
				Line line = dcp.bean.content.get(dcp.bean.content.size() - 1);
				for (Integer i : covers)
					line.coverInfo += " " + i;
			}
		}
	}

	private String getFieldAccess(int access) {
		String ret = "";
		if ((DexConstants.ACC_PUBLIC & access) != 0)
			ret += "public ";
		if ((DexConstants.ACC_PROTECTED & access) != 0)
			ret += "protected ";
		if ((DexConstants.ACC_PRIVATE & access) != 0)
			ret += "privated ";
		if ((DexConstants.ACC_STATIC & access) != 0)
			ret += "static ";
		if ((DexConstants.ACC_FINAL & access) != 0)
			ret += "final ";
		if ((DexConstants.ACC_TRANSIENT & access) != 0)
			ret += "transient ";
		if ((DexConstants.ACC_VOLATILE & access) != 0)
			ret += "volatile ";
		return ret;
	}

	public static String getMethodAccess(int access) {
		String ret = "";
		if ((DexConstants.ACC_PUBLIC & access) != 0)
			ret += "public ";
		if ((DexConstants.ACC_PROTECTED & access) != 0)
			ret += "protected ";
		if ((DexConstants.ACC_PRIVATE & access) != 0)
			ret += "privated ";
		if ((DexConstants.ACC_ABSTRACT & access) != 0)
			ret += "abstract ";
		if ((DexConstants.ACC_STATIC & access) != 0)
			ret += "static ";
		if ((DexConstants.ACC_SYNCHRONIZED & access) != 0)
			ret += "synchronized ";
		if ((DexConstants.ACC_NATIVE & access) != 0)
			ret += "native ";
		return ret;
	}

	private static TraceCache<DexZipReader> getTraceCache() {
		return new TraceCache<DexZipReader>(60 * 60 * 1000L) {
			@Override
			public DexZipReader getInstance(String key) {
				File f = new File(TraceDir, key);
				System.out.println("file:" + f.getAbsolutePath() + " is Exist?" + f.exists());
				if (!f.exists())
					return null;
				try {
					return new DexZipReader(f.getAbsolutePath());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}
		};
	}

	public static String searchClz(String fileName, String clz, CodeMapFile temp) {
		if (global.loadIfNotExist(fileName)) {
			DexZipReader reader = global.get(fileName);
			return reader.queryMethod(clz, temp);
		}
		return "No such File";
	}

}
