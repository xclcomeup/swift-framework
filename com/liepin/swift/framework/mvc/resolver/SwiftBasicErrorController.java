//package com.liepin.swift.framework.mvc.resolver;
//
//import java.util.List;
//import java.util.Map;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//import org.springframework.boot.autoconfigure.web.ErrorProperties;
//import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
//import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
//import org.springframework.boot.web.servlet.error.ErrorAttributes;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.servlet.ModelAndView;
//
//@RequestMapping("${server.error.path:${error.path:/error}}")
//public class SwiftBasicErrorController extends BasicErrorController {
//
//    public SwiftBasicErrorController(ErrorAttributes errorAttributes, ErrorProperties errorProperties,
//            List<ErrorViewResolver> errorViewResolvers) {
//        super(errorAttributes, errorProperties, errorViewResolvers);
//    }
//    
//    @RequestMapping(produces = "text/html",value = "/404")
//    public ModelAndView errorHtml404(HttpServletRequest request,HttpServletResponse response) {
//        response.setStatus(getStatus(request).value());
//        Map<String, Object> model = getErrorAttributes(request,isIncludeStackTrace(request, MediaType.TEXT_HTML));
//        model.put("msg","自定义错误信息");
//        return new ModelAndView("jsp/error/swift404.jsp", model);
//    }
//
//}
