package cn.edu.pku.apiminier.web;

import java.io.File;
import java.util.List;

public class UploadConf extends BasicConf {
	public static String filePath;
	public static String tempFilePath;
	public static String picPath;
	public static String iconpath;
	@SuppressWarnings("unused")
	private static ServerType serverType = init(BasicConf.osType);

	@Override
	public List<String> getAdminEmails() {
		
		// TODO Auto-generated method stub
		return null;
	}

	private static ServerType init(ServerType type) {
		switch (type) {
		case Mac:
			filePath = "/Volumes/Workspace/arc-welder/upload/";
			tempFilePath = "/Volumes/Workspace/arc-welder/temp/";
			break;
		case NewTech1626:
			// serverIP = "172.16.31.141";
			// workerIp.add("192.168.3.135");
			// workerIp.add("192.168.1.93");
			break;
		case School:
			// serverIP = "";
			break;
		case Lugu:
			filePath = "/home/user/program/apache-tomcat-7.0.64/webapps/Upload/files/";
			tempFilePath = "/home/user/program/apache-tomcat-7.0.64/webapps/Upload/temp/";
			picPath = "/home/user/program/apache-tomcat-7.0.64/webapps/NativeAPI/images/";
			iconpath = "/home/user/program/apache-tomcat-7.0.64/webapps/NativeAPI/icons/";
			break;
		case Windows:
			picPath = "F:\\gitrepo\\NativeAPI\\WebContent\\images\\";
			String path = UploadConf.class.getClassLoader().getResource("")
					.getPath();
			File ff = new File(path);
			ff = ff.getParentFile().getParentFile();
			picPath = ff.getAbsolutePath()+"\\images\\";
			iconpath = ff.getAbsolutePath()+"\\icons\\";
			break;
		default:
			break;
		}

		return type;
	}
}
