package cn.edu.pku.coverdroid.cmd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import cn.edu.pku.apiminier.debug.TraceConf;
import cn.edu.pku.apiminier.web.trace.DexZipReader;

import com.google.iasgson.Gson;
import com.yancloud.android.reflection.get.GetMessage;

public class PhoneHelper {
	private static final String END = "\r\n";
	private static final String TWOHYPHENS = "--";
	private static final String BOUNDARY = "******";
	private static Gson gson = new Gson();

	String url;

	public static enum Handler {
		fileHelper("filehelper"), CMDManager("CMDManager");
		String name;

		Handler(String str) {
			name = str;
		}

		public String getName() {
			return name;
		}
	}

	public PhoneHelper(String ip) {
		url = "http://" + ip + ":6161/";
	}

	public String dumpDex(String pkgName) {
		return forward(pkgName, "dumpAllDex", "");
	}

	public String autoTrace(String pkgName, TraceConf conf) {
		return forward(pkgName, "autoTrace", gson.toJson(conf));
	}

	public String listPackages() {
		return forward("CMDManager", "listPackages", null);
	}

	public List<String> autoPull(String endWith) {
		String phoneDir = "/sdcard/dumpDex/";
		String listSh = this.lsDir(phoneDir);
		String[] ff = listSh.split("\n");
		List<String> files = new ArrayList<String>();
		for (String str : ff) {
			if (str.endsWith(endWith)) {
				System.out.println("AutoPull:" + str);
				this.getFile("/sdcard/dumpDex/" + str, DexZipReader.TraceDir);
				files.add(str);
				this.rmFile(phoneDir + str);
			}
		}
		return files;
	}

	public String lsDir(String path) {
		GetMessage getMessage = new GetMessage();
		getMessage.method = "lsDir";
		getMessage.arg = path;
		return postGetMessage(Handler.fileHelper, getMessage);
	}

	public void getFile(String file, String targetDir) {
		try {
			GetMessage getMessage = new GetMessage();
			getMessage.arg = file;
			File f = new File(file);
			System.out.println("Download from Phone:" + file);
			URL url = new URL(this.url + "fileprovider?getMessage="
					+ gson.toJson(getMessage));
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(3 * 1000);
			conn.setRequestProperty("User-Agent",
					"Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
			InputStream inputStream = conn.getInputStream();
			FileOutputStream fout = new FileOutputStream(targetDir + "/"
					+ f.getName(), false);
			byte[] buff = new byte[8196];
			int size = 1024;
			int total = 0;
			for (int i = 0; (i = inputStream.read(buff)) > 0;) {
				fout.write(buff, 0, i);
				total += i;
				if (total > size) {
					System.out.print(byteToDisplay(total) + "...");
					if (size < 16 * 1024 * 1024)
						size *= 2;
					else
						size += 16 * 1024 * 1024;
				}
			}
			fout.close();
			inputStream.close();
			System.out.println("\nDownload success:" + byteToDisplay(total));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String rmFile(String path) {
		GetMessage getMessage = new GetMessage();
		getMessage.method = "rmFile";
		getMessage.arg = path;
		return postGetMessage(Handler.fileHelper, getMessage);
	}

	public String forward(String pkgName, String name, String input) {
		GetMessage message = new GetMessage();
		message.pkgName = pkgName;
		message.method = name;
		message.arg = input;
		return postGetMessage(Handler.CMDManager, message);
	}

	public String postGetMessage(Handler handler, GetMessage msg) {
		return sendHttpGet(url + handler.getName() + "?getMessage="
				+ gson.toJson(msg));
	}

	private String byteToDisplay(int total) {
		String str = "byte";
		if (total > 1024) {
			total /= 1024;
			str = "kb";
		}
		if (total > 1024) {
			total /= 1024;
			str = "mb";
		}
		return total + str;
	}

	private String sendHttpGet(String str) {
		try {
			System.out.println("[PhoneHelper] sendHttp:" + str);
			URL url = new URL(str);
			URLConnection connection = url.openConnection();
			connection.setReadTimeout(1000 * 1000);
			InputStream input = connection.getInputStream();
			Scanner sc = new Scanner(input);
			StringBuilder sb = new StringBuilder();
			for (; sc.hasNextLine();) {
				sb.append(sc.nextLine()).append("\n");
			}
			sc.close();
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

}
