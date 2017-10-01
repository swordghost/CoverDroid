package cn.edu.pku.apiminier.web;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.edu.pku.apiminier.debug.TraceConf;
import cn.edu.pku.apiminier.debug.TraceType;
import cn.edu.pku.apiminier.web.trace.CodeMapFile;
import cn.edu.pku.apiminier.web.trace.DexZipReader;
import cn.edu.pku.coverdroid.cmd.PhoneHelper;

import com.google.iasgson.Gson;
import com.google.iasgson.GsonBuilder;

public class CMDHandler {
	public static String lastException;
	private static String confPath = "./trace2dex.conf";
	private static Map<String, String> trace2dex = new HashMap<String, String>();
	static {
		File conf = new File(confPath);
		try {
			if (!conf.exists()) {
				conf.createNewFile();
			} else {
				FileInputStream in = new FileInputStream(conf);
				byte[] b = new byte[in.available()];
				in.read(b);
				in.close();
				for (String tmp : new String(b).split("\n")) {
					String[] line = tmp.split("\t");
					if (line.length < 2) {
						continue;
					}
					trace2dex.put(line[0], line[1]);
				}
			}
			confPath = conf.getAbsolutePath();
			System.out.println("[CMDHandler] Conf path = " + confPath);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static Method get(String name) {
		try {
			Method m = CMDHandler.class.getDeclaredMethod(name,
					HttpServletRequest.class, HttpServletResponse.class);
			return m;
		} catch (Throwable e) {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(bo));
			lastException = bo.toString();
			return get("exception");
		}
	}

	public static String exception(HttpServletRequest request,
			HttpServletResponse response) {
		return lastException;
	}

	public static String lsDir(HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("text/json");
		String succ = request.getParameter("succ");
		return new Gson().toJson(DexZipReader.lsDir(succ));
	}

	public static String listFiles(HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("text/json");
		File dir = new File(DexZipReader.TraceDir);
		Map<String, List<String>> files = new HashMap<>();
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				List<String> list = new ArrayList<String>();
				for (String subFile : f.list()) {
					list.add(subFile);
				}
			}
		}
		return new Gson().toJson(files);
	}

	public static String queryClassCode(HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("text/json");
		String clz = request.getParameter("clzName");
		String fileName = request.getParameter("fileName");
		String coverage = request.getParameter("coverage");

		CodeMapFile temp = new CodeMapFile(new File(DexZipReader.TraceDir
				+ coverage).getAbsolutePath());

		if (!trace2dex.containsKey(coverage)) {
			trace2dex.put(coverage, fileName);
			appendToTrace2Dir(coverage, fileName);
		} else {
			fileName = trace2dex.get(coverage);
		}

		System.out.println("[DexZipReader] dexFile: " + fileName
				+ ", traceFile: " + coverage);
		return DexZipReader.searchClz(fileName, clz, temp);
	}

	private static void appendToTrace2Dir(String coverage, String fileName) {
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(confPath, true);
			PrintStream ps = new PrintStream(fout);
			ps.println();
			ps.print(coverage);
			ps.print("\t");
			ps.print(fileName);
			ps.close();
			fout.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String queryPackages(HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("text/json");
		String ip = request.getParameter("ip");

		return new PhoneHelper(ip).listPackages();
	}

	public static String dumpDex(HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("text/json");
		String ip = request.getParameter("ip");
		String pkgName = request.getParameter("pkg_name");
		try {
			PhoneHelper ph = new PhoneHelper(ip);
			System.out.println(ph.dumpDex(pkgName));
			List<String> dexes = ph.autoPull("zip");
			String dexFile = dexes.get(0);
			return "{\"status\":\"success\",\"dexFile\":\"" + dexFile + "\"}";

		} catch (Exception e) {
			e.printStackTrace();
			return "{\"status\":\"error\"}";
		}
	}

	public static String autoTrace(HttpServletRequest request,
			HttpServletResponse response) {
		response.setContentType("text/json");
		String ip = request.getParameter("ip");
		String pkgName = request.getParameter("pkg_name");

		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			PhoneHelper ph = new PhoneHelper(ip);
			PrintStream ps = new PrintStream(bo);
			TraceConf conf = new TraceConf(TraceType.Coverage, null, 0, 0,
					1000, new ArrayList<String>());
			String toPrint = new GsonBuilder().setPrettyPrinting().create()
					.toJson(conf);

			System.out.println(toPrint);
			String ret = ph.autoTrace(pkgName, conf);
			System.out.println(ret);

			if (ret.contains("stop")) {
				List<String> traceFiles = ph.autoPull("trace");
				ps.println(traceFiles);
				return "{\"traceFile\":\"" + traceFiles.get(0) + "\"}";
			} else {
				return "{\"status\":\"start\"}";
			}
		} catch (Exception e) {
			e.printStackTrace();
			return "{\"status\":\"error\"}";
		}

	}
}
