package cn.edu.pku.apiminier.web;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CoverServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8694040559114663465L;

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		handle(request, response);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		handle(request, response);
	}

	private void handle(HttpServletRequest request, HttpServletResponse response) {
		String ret = null;
		String type = request.getParameter("type");
		response.setCharacterEncoding("UTF-8");
		Method m = CMDHandler.get(type);
		try {
			ret = (String) m.invoke(null, request, response);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		OutputStream w = null;
		try {
			w = response.getOutputStream();
			if (ret != null && ret.length() > 500)
				System.out.println("[DexPrinterServlet]"
						+ ret.substring(0, 500));
			else
				System.out.println("[DexPrinterServlet]" + ret);
			if (ret == null)
				ret = "{\"errorCode\":-1,{\"reason\":\"network_unreachable or time limit exceeds.\"}}";
			w.write(ret.getBytes("UTF-8"));
			w.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
