package cn.edu.pku.apiminier.web.trace;

import java.util.Set;

public class CodeMapComparator {

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		String path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1478428460_5735_0.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493715752_3452_0.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493715752_3452_0.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493799383_27627_1.trace";
		// path =
		// "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493714760_1309_5.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493828895_4739_0.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493831691_5047_1.trace";
		path = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493831780_5047_2.trace";
		String path1 = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493869534_5291_1.trace";
		String path2 = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493870284_4781_1.trace";
		// path =
		// "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/1493834202_4855_1.trace";
		CodeMapFile temp1 = new CodeMapFile(path1);
		CodeMapFile temp2 = new CodeMapFile(path2);
		Set<String> temp1Set = temp1.getMethodStrSet();
		Set<String> temp2Set = temp2.getMethodStrSet();
		for (String key : temp2Set) {
			if (!temp1Set.contains(key))
				System.out.println("ExtraMethod:" + key);
		}
		System.out.println("methodCount:" + temp1Set.size() + " "
				+ temp2Set.size());

		String apkPath = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/com.android.calculator2_5.1.1-eng.root.20170407.155435.zip";
		apkPath = "/Volumes/Workspace/DelayDroidWorkSpace/APIMinier/output/trace/me.ele_7.0.2.zip";
		@SuppressWarnings("unused")
		DexZipReader dzp = new DexZipReader(apkPath);
		// temp.printAll(dzp, System.out);

		System.exit(0);
	}

}