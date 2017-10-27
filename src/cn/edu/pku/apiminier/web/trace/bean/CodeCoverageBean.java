package cn.edu.pku.apiminier.web.trace.bean;

import java.util.ArrayList;
import java.util.List;

public class CodeCoverageBean {
	public int totalReg;
	public List<Line> content;
	public long calls;

	public static class Line {
		public int offset;
		public int lineNumber;
		public String coverInfo;
		public String content;

		public Line() {
			lineNumber = -1;
		}
	}

	public CodeCoverageBean() {
		totalReg = 0;
		calls = 0;
		content = new ArrayList<Line>();
	}

}
