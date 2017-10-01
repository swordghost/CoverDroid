package cn.edu.pku.apiminier.web;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

// Servlet 文件上传  
public class UploadServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4430516772397859452L;
	private String filePath; // 文件存放目录
	private String tempPath; // 临时文件目录

	// 初始化
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		filePath = UploadConf.filePath;
		tempPath = UploadConf.tempFilePath;
		System.out.println("文件存放目录、临时文件目录准备完毕 ...");
	}

	// doPost
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		res.setContentType("text/plain;charset=utf-8");
		PrintWriter pw = res.getWriter();
		try {
			DiskFileItemFactory diskFactory = new DiskFileItemFactory();
			// threshold 极限、临界值，即硬盘缓存 1M
			diskFactory.setSizeThreshold(4 * 1024);
			// repository 贮藏室，即临时文件目录
			diskFactory.setRepository(new File(tempPath));

			ServletFileUpload upload = new ServletFileUpload(diskFactory);
			// 设置允许上传的最大文件大小 4M
			upload.setSizeMax(4000 * 1024 * 1024);
			// 解析HTTP请求消息头
			List<FileItem> fileItems = upload.parseRequest(req);
			Iterator<FileItem> iter = fileItems.iterator();
			while (iter.hasNext()) {
				FileItem item = (FileItem) iter.next();
				if (item.isFormField()) {
					System.out.println("处理表单内容 ...");
					processFormField(item, pw);
				} else {
					processUploadFile(item, pw);
				}
			}
			pw.close();
		} catch (Exception e) {
			System.out.println("使用 fileupload 包时发生异常 ...");
			e.printStackTrace();
		}
	}

	private void processFormField(FileItem item, PrintWriter pw) throws Exception {
		String name = item.getFieldName();
		String value = item.getString();
		pw.println(name + " : " + value + "\r\n");
	}

	private void processUploadFile(FileItem item, PrintWriter pw) throws Exception {
		String filename = item.getName();
		System.out.println("处理上传的文件:+" + filename + " ...");
		int index = filename.lastIndexOf("/");
		String dir = filename.substring(0, index);
		dir = dir.substring(dir.lastIndexOf("/"));
		filename = filename.substring(index + 1, filename.length());
		long fileSize = item.getSize();
		File realDir = new File(filePath, dir);
		if (!realDir.exists())
			realDir.mkdirs();
		File uploadFile = new File(realDir, filename);
		if (!uploadFile.exists())
			uploadFile.createNewFile();
		item.write(uploadFile);
		pw.println(filename + " upload done!");
		pw.println("fileSiz ：" + fileSize + "\r\n");
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
		doPost(req, res);
	}

	@SuppressWarnings("unused")
	private static void uploadFile(String path) {
		String end = "\r\n";
		String twoHyphens = "--";
		String boundary = "******";
		try {
			URL url = new URL("");
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setDoInput(true);
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setUseCaches(false);
			httpURLConnection.setRequestMethod("POST");
			httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
			httpURLConnection.setRequestProperty("Charset", "UTF-8");
			httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			DataOutputStream dos = new DataOutputStream(httpURLConnection.getOutputStream());
			dos.writeBytes(twoHyphens + boundary + end);
			dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"");
			dos.write(path.getBytes("utf-8"));
			dos.writeBytes("\"" + end);
			dos.writeBytes(end);
			// 文件通过输入流读到Java代码中-++++++++++++++++++++++++++++++`````````````````````````
			FileInputStream fis = new FileInputStream(path);
			byte[] buffer = new byte[8192]; // 8k
			int count = 0;
			while ((count = fis.read(buffer)) != -1) {
				dos.write(buffer, 0, count);

			}
			fis.close();
			System.out.println("file send to server............");
			dos.writeBytes(end);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
			dos.flush();

			// 读取服务器返回结果
			InputStream is = httpURLConnection.getInputStream();
			InputStreamReader isr = new InputStreamReader(is, "utf-8");
			BufferedReader br = new BufferedReader(isr);
			String result = br.readLine();
			System.out.println(result);
			dos.close();
			is.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}