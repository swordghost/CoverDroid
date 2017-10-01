package cn.edu.pku.apiminier.web.trace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.edu.pku.apiminier.web.trace.CodeMapFile.CodeMethod;
import cn.edu.pku.apiminier.web.trace.CodeMapFile.CoverReport;
import cn.edu.pku.apiminier.web.trace.CodeMapFile.InsnCover;

import com.googlecode.d2j.DexConstants;
import com.googlecode.d2j.DexLabel;
import com.googlecode.d2j.Field;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.reader.Op;
import com.googlecode.d2j.visitors.DexClassVisitor;
import com.googlecode.d2j.visitors.DexCodeVisitor;
import com.googlecode.d2j.visitors.DexDebugVisitor;
import com.googlecode.d2j.visitors.DexFileVisitor;
import com.googlecode.d2j.visitors.DexMethodVisitor;

public class ReportCollector extends DexFileVisitor {

	private class ClassCollector extends DexClassVisitor {
		CoverReport report;
		Set<Method> methods;
		String clzName;

		public ClassCollector(String className) {
			clzName = className;
			report = new CoverReport();
			clzReport.put(className, report);
			methods = new HashSet<Method>();
			clz2Method.put(className, methods);
		}

		@Override
		public DexMethodVisitor visitMethod(int accessFlags, Method method) {
			return new MethodCollector(this, method);
		}

		public void visitEnd() {
			int clzCover = 0;
			for (Method method : methods) {
				report.method++;
				if (codeMapFile.hasCover(method)) {
					clzCover = 1;
					report.methodCover++;
					InsnCover cover = methodReport.get(method);
					report.insn += cover.insn;
					report.insnCover += cover.insnCover;
					report.branch += cover.branch;
					report.branchCover += cover.branchCover;
					if (cover.maxLine >= 0) {
						report.line += cover.line;
						report.lineCover += cover.lineCover;
					}
				}
			}
			report.clz = 1;
			report.clzCover += clzCover;
		}
	}

	private class MethodCollector extends DexMethodVisitor {
		Method m;
		ClassCollector cc;
		CodeCollector codeCollector;

		public MethodCollector(ClassCollector classCollector, Method method) {
			m = method;
			cc = classCollector;
			codeCollector = new CodeCollector(cc, m);

		}

		@Override
		public DexCodeVisitor visitCode() {
			if (codeCollector.codeMethod == null)
				return null;
			return codeCollector;
		}
	}

	private class CodeCollector extends DexCodeVisitor {
		Method m;
		ClassCollector cc;
		InsnCover cover;
		int offset;
		CodeMethod codeMethod;
		Map<DexLabel, Integer> label2Integer;
		Set<Integer> coveredLines;

		public CodeCollector(ClassCollector classCollector, Method method) {
			m = method;
			cc = classCollector;
			cover = new InsnCover();
			cover.insnCover = cover.insn = cover.branch = cover.branchCover = 0;
			methodReport.put(m, cover);
			offset = 0;
			label2Integer = new HashMap<DexLabel, Integer>();
			codeMethod = ReportCollector.this.codeMapFile.codes.get(m);
			coveredLines = new HashSet<Integer>();
			cc.methods.add(m);
		}

		public void visitRegister(int total) {
			cover.branch = cover.branchCover = 0;
		}

		public void visitStmt2R1N(Op op, int distReg, int srcReg, int content) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitStmt3R(Op op, int a, int b, int d) {
			if (visitor != null) {
				visitor.visitStmt3R(op, a, b, d);
			}
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitTypeStmt(Op op, int a, int b, String type) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitConstStmt(Op op, int ra, Object value) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitFillArrayDataStmt(Op op, int ra, Object array) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitEnd() {
			cover.maxLine = -1;
			cover.minLine = -1;
			for (Integer linNu : label2Integer.values()) {
				if (cover.maxLine == -1 || cover.maxLine < linNu) {
					cover.maxLine = linNu;
				}
				if (cover.minLine == -1 || cover.minLine > linNu) {
					cover.minLine = linNu;
				}
			}
			cover.line = cover.maxLine - cover.minLine + 1;
			cover.lineCover = coveredLines.size();
			if (cover.branch == 0) {
				cover.branch = cover.branchCover = 1;
			}
		}

		public void visitFieldStmt(Op op, int a, int b, Field field) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitFilledNewArrayStmt(Op op, int[] args, String type) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		/**
		 * <pre>
		 * OP_IF_EQ
		 * OP_IF_NE
		 * OP_IF_LT
		 * OP_IF_GE
		 * OP_IF_GT
		 * OP_IF_LE
		 * OP_GOTO
		 * OP_IF_EQZ
		 * OP_IF_NEZ
		 * OP_IF_LTZ
		 * OP_IF_GEZ
		 * OP_IF_GTZ
		 * OP_IF_LEZ
		 * </pre>
		 * 
		 * @param op
		 * @param a
		 * @param b
		 * @param label
		 */
		public void visitJumpStmt(Op op, int a, int b, DexLabel label) {
			int c = codeMethod.getCover(offset);
			if (op.name().startsWith("IF")) {
				cover.branch += 2;
			}
			cover.insn++;
			if (c != 0) {
				cover.insnCover++;
				int cover2 = codeMethod.getCover(offset + 1);
				cover.branchCover++;
				if (c != cover2 && cover2 != 0)
					cover.branchCover++;
			}
			offset += op.format.size;
		}

		public void visitLabel(DexLabel label) {
			int c = codeMethod.getCover(offset);
			if (c != 0 && label2Integer.containsKey(label)) {
				coveredLines.add(label2Integer.get(label));
			}
		}

		public void visitSparseSwitchStmt(Op op, int ra, int[] cases, DexLabel[] labels, int offset) {
			cover.branch += cases.length + 1;
			List<Integer> covers = codeMethod.getCover(offset, cases.length + 1);
			cover.branchCover += countNZero(covers);
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			this.offset += op.format.size;
		}

		private int countNZero(List<Integer> cover) {
			int ret = 0;
			for (int i = 0; i < cover.size(); i++) {
				if (cover.get(i) != 0)
					ret++;
			}
			return ret;
		}

		public void visitMethodStmt(Op op, int[] args, Method method) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitStmt2R(Op op, int a, int b) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitStmt0R(Op op) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			if (op != null && op.format != null)
				offset += op.format.size;
		}

		public void visitStmt1R(Op op, int reg) {
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			offset += op.format.size;
		}

		public void visitPackedSwitchStmt(Op op, int aA, int first_case, DexLabel[] labels, int offset) {
			cover.branch += labels.length;
			List<Integer> covers = codeMethod.getCover(offset, labels.length);
			cover.branchCover += countNZero(covers);
			int c = codeMethod.getCover(offset);
			cover.insn++;
			if (c != 0)
				cover.insnCover++;
			this.offset += op.format.size;
		}

		public DexDebugVisitor visitDebug() {
			return new DebugCollector(this);
		}

	}

	class DebugCollector extends DexDebugVisitor {
		CodeCollector cc;

		public DebugCollector(CodeCollector codeCollector) {
			cc = codeCollector;
		}

		public void visitLineNumber(int line, DexLabel label) {
			cc.label2Integer.put(label, line);
		}
	}

	Map<String, CoverReport> clzReport;
	Map<String, Set<Method>> clz2Method;
	Map<Method, InsnCover> methodReport;
	CodeMapFile codeMapFile;

	public ReportCollector(CodeMapFile c) {
		clzReport = new HashMap<String, CodeMapFile.CoverReport>();
		clz2Method = new HashMap<String, Set<Method>>();
		methodReport = new HashMap<Method, CodeMapFile.InsnCover>();
		codeMapFile = c;
	}

	@Override
	public DexClassVisitor visit(int access_flags, String className, String superClass, String[] interfaceNames) {
		// We ignore interfaces
		if ((access_flags & DexConstants.ACC_INTERFACE) == 0 && !clzReport.containsKey(className)) {
			ClassCollector cc = new ClassCollector(className);
			return cc;
		} else
			return null;
	}

	public Map<String, CoverReport> getReport() {
		return clzReport;
	}

	public Set<Method> getMethods(String clzName) {
		return clz2Method.get(clzName);
	}

	public InsnCover getInsnCover(Method method) {
		return methodReport.get(method);
	}

}
