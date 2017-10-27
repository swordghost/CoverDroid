package cn.edu.pku.apiminier.web.trace;

import java.util.HashMap;
import java.util.Map;

import cn.edu.pku.apiminier.web.trace.CodeMapFile.CodeMethod;
import cn.edu.pku.apiminier.web.trace.bean.CodeCoverageBean;
import cn.edu.pku.apiminier.web.trace.bean.CodeCoverageBean.Line;

import com.googlecode.d2j.DexLabel;
import com.googlecode.d2j.Field;
import com.googlecode.d2j.Method;
import com.googlecode.d2j.reader.Op;
import com.googlecode.d2j.visitors.DexCodeVisitor;
import com.googlecode.d2j.visitors.DexDebugVisitor;

public class DexCodePrinter extends DexCodeVisitor {

	private boolean printCode;
	private String indent = "    ";
	public CodeCoverageBean bean;
	int offset;
	private static final boolean verbose = true;
	private Map<DexLabel, Integer> label2Line = new HashMap<DexLabel, Integer>();
	private CodeMethod coverage;

	public DexDebugVisitor visitDebug() {
		return new DexDebugVisitor() {
			public void visitLineNumber(int line, DexLabel label) {
				label2Line.put(label, line);
			}
		};
	}

	public DexCodePrinter(boolean b, CodeCoverageBean bean,
			CodeMethod codeMethod) {
		this.printCode = b;
		this.bean = bean;
		this.coverage = codeMethod;
	}

	public void visitTryCatch(DexLabel start, DexLabel end, DexLabel handler[],
			String type[]) {
		// Just ignore try catch
		// TODO
		// out.println("====Try Catch Start=====");
		// out.println("Start Label:" + start);
		// out.println("End Label:" + end);
		// for (int i = 0; i < handler.length; i++) {
		// out.println("Handler:" + handler[i] + " type:"
		// + sigToDisplay(type[i]));
		// }
		// if (visitor != null) {
		// visitor.visitTryCatch(start, end, handler, type);
		// }
		//
		// out.println("====Try Catch Finish=====");
	}

	public void visitRegister(int total) {
		bean.totalReg = total;
		if (coverage != null)
			bean.calls = coverage.calls;
		offset = 0;
	}

	public void visitStmt2R1N(Op op, int distReg, int srcReg, int content) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		line.content = op.displayName + " " + distReg + " <-- " + srcReg + " "
				+ content;
	}

	private Line printOffset(Op op) {
		CodeCoverageBean.Line line = new CodeCoverageBean.Line();
		bean.content.add(line);
		line.offset = offset;
		if (coverage != null) {
			line.coverInfo = (coverage.getCover(offset) + "");
		} else
			line.coverInfo = "0";
		if (op != null)
			offset += op.format.size;
		return line;
	}

	public void visitStmt3R(Op op, int a, int b, int c) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		line.content = op.displayName + " " + a + " <-- " + b + ", " + c;
	}

	public void visitTypeStmt(Op op, int a, int b, String type) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		line.content = op.displayName + " " + a + " " + b + " "
				+ sigToDisplay(type);
	}

	public void visitConstStmt(Op op, int ra, Object value) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		if (op.displayName.contains("const-class")) {
			line.content = op.displayName + " " + ra + " "
					+ sigToDisplay(value.toString());
		} else
			line.content = op.displayName + " " + ra + " " + value;
	}

	public void visitFillArrayDataStmt(Op op, int ra, Object array) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		line.content = "FillArrayDataStmt: " + op.displayName + " " + ra + " "
				+ array;
	}

	public void visitFieldStmt(Op op, int a, int b, Field field) {
		if (printCode || verbose) {
			CodeCoverageBean.Line line = printOffset(op);
			line.content = op.displayName + " " + a + " " + b + " "
					+ field2Display(field);
		}
	}

	public void visitFilledNewArrayStmt(Op op, int[] args, String type) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		line.content = "FilledNewArrayStmt: " + op.displayName + " argSize:"
				+ args + " " + type;
	}

	public void visitJumpStmt(Op op, int a, int b, DexLabel label) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		if (coverage != null) {
			if (op != null && op.name().contains("IF")) {
				int left = Integer.valueOf(line.coverInfo);
				int right = coverage.getCover(offset + 1 - op.format.size);
				line.coverInfo = (left + right) + " " + left + " " + right;
			}
		}
		line.content = op.displayName + " " + a + " " + b + " label:" + label;
	}

	public void visitLabel(DexLabel label) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(null);
		if (label2Line.containsKey(label)) {
			line.lineNumber = label2Line.get(label);
		}
		line.content = "label:" + label;
	}

	public void visitSparseSwitchStmt(Op op, int ra, int[] cases,
			DexLabel[] labels) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		StringBuilder sb = new StringBuilder();
		sb.append(op.displayName).append(" ").append(ra);
		if (labels != null) {
			for (DexLabel label : labels) {
				sb.append(" ").append(label);
			}
		}
		line.content = sb.toString();
	}

	public void visitMethodStmt(Op op, int[] args, Method method) {
		if (printCode || verbose) {
			CodeCoverageBean.Line line = printOffset(op);
			line.content = op.displayName + listArg(args) + " desc:"
					+ method2Display(method);
		}
	}

	public static String field2Display(Field f) {
		StringBuilder sb = new StringBuilder();
		sb.append(sigToDisplay(f.getType())).append(" ");
		sb.append(sigToDisplay(f.getOwner())).append(".");
		sb.append(f.getName());

		return sb.toString();
	}

	public static String method2DisplayhideOwner(Method method) {
		StringBuilder sb = new StringBuilder();
		if (method.getName().equals("<init>")) {
			sb.append(sigToDisplay(method.getOwner()));
		} else {
			sb.append(sigToDisplay(method.getReturnType())).append(" ");
			sb.append(method.getName());
		}
		sb.append("(");
		boolean isFirst = true;
		for (String str : method.getParameterTypes()) {
			if (isFirst) {
				isFirst = false;
			} else
				sb.append(", ");
			sb.append(sigToDisplay(str));
		}
		sb.append(")");
		return sb.toString();
	}

	public static String method2Display(Method method) {
		StringBuilder sb = new StringBuilder();
		sb.append(sigToDisplay(method.getReturnType())).append(" ");
		sb.append(sigToDisplay(method.getOwner())).append(".");
		sb.append(method.getName());
		sb.append("(");
		boolean isFirst = true;
		for (String str : method.getParameterTypes()) {
			if (isFirst) {
				isFirst = false;
			} else
				sb.append(", ");
			sb.append(sigToDisplay(str));
		}
		sb.append(")");
		return sb.toString();

	}

	private static String sigToDisplay(String sig) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		if (sig == null)
			return "null";
		for (; i < sig.length() && sig.charAt(i) == '['; i++)
			sb.append("[]");
		switch (sig.charAt(i)) {
		case 'Z':
			return "boolean" + sb.toString();
		case 'I':
			return "int" + sb.toString();
		case 'S':
			return "short" + sb.toString();
		case 'C':
			return "char" + sb.toString();
		case 'B':
			return "byte" + sb.toString();
		case 'J':
			return "long" + sb.toString();
		case 'F':
			return "float" + sb.toString();
		case 'D':
			return "double" + sb.toString();
		case 'V':
			return "void";
		case 'L':
			return sig.substring(i + 1).replace("/", ".").replace(";", "")
					+ sb.toString();
		}
		return sig;
	}

	private String listArg(int[] args) {
		if (args == null)
			return null;
		StringBuilder sb = new StringBuilder("");
		for (int i : args)
			sb.append(" ").append(i);
		return sb.toString();
	}

	public void visitStmt2R(Op op, int a, int b) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		line.content = op.displayName + " " + a + " <-- " + b;
	}

	public void visitStmt0R(Op op) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		line.content = op.displayName;
	}

	public void visitStmt1R(Op op, int reg) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);
		line.content = op.displayName + " " + reg;
	}

	public void visitPackedSwitchStmt(Op op, int aA, int first_case,
			DexLabel[] labels) {
		if (!verbose)
			return;
		CodeCoverageBean.Line line = printOffset(op);

		StringBuilder sb = new StringBuilder();
		sb.append(indent).append(op.displayName).append(" ").append(aA);
		if (labels != null) {
			for (DexLabel label : labels) {
				sb.append(" ").append(label);
			}
		}
		line.content = sb.toString();
	}
}
