package cn.edu.pku.apiminier.web;

import java.util.List;

public abstract class BasicConf {
	public static ServerType osType = isMacOS();

	private static ServerType isMacOS() {
		String osName = System.getProperty("os.name") + " "
				+ System.getProperty("os.version");
		System.out.println("[BasicConf] OS Name:" + osName);
		if (osName.startsWith("Mac"))
			return ServerType.Mac;
		else if (osName.startsWith("Linux 3.5.0")) {
			return ServerType.NewTech1626;
		} else if (osName.startsWith("Linux 2.6.32")) {
			return ServerType.School;
		} else if (osName.startsWith("Linux 3.19.0")) {
			return ServerType.Lugu;
		}
		else if (osName.startsWith("Windows")){
			return ServerType.Windows;
		}
		return ServerType.Unknown;

	}

	public abstract List<String> getAdminEmails();
}
