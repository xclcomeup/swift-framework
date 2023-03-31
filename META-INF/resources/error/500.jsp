<%@ page import="java.io.PrintWriter"%>
<%@ page import="java.util.Date"%>
<%@ page contentType="text/html;charset=utf-8"%>
<%
Date timestamp=(Date) request.getAttribute("swift_timestamp");
Integer status=(Integer)request.getAttribute("swift_status");
String url=(String)request.getAttribute("swift_url");
String stackTrace=(String)request.getAttribute("swfit_trace");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<title>临时后台</title>
</head>
<body bgcolor="#F0F0F0">
<div>
    <div>
        <h1>系统异常统一处理</h1>
        <h3>时间：<%=timestamp%></h3>
        <h3>状态：<%=status%></h3>
        <h3>请求地址：<%=url%></h3>
        <h2>异常堆栈跟踪日志StackTrace</h2>
        <h3><%=stackTrace%></h3>
    </div>
</div>
</body>
</html>