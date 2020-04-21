package wzf.study.demo.servlet;

import wzf.study.demo.annotation.ZfAutowire;
import wzf.study.demo.annotation.ZfController;
import wzf.study.demo.annotation.ZfRequestMapping;
import wzf.study.demo.annotation.ZfService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author 王振方
 * @date 2020/4/21
 */
public class ZfDispatcherServlet extends HttpServlet {

    //保存application.properties配置文件中的内容
    private Properties contextConfig = new Properties();

    //保存扫描的所有的类名
    private List<String> classNames = new ArrayList<String>();

    //IOC容器
    private Map<String, Object> ioc = new HashMap<String, Object>();

    //保存url和Method的对应关系
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2、扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3、初始化扫描到的类，并且将它们放入到ICO容器中
        doInstance();

        //4、完成依赖注入
        doAutowire();

        //5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("WZF Spring framwork is init.");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            doDispatch(req, resp);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        System.out.println("url:" + url + "  ;contextPath:" + contextPath);

        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 NOT FOUND !");
            return;
        }

        Method method = this.handlerMapping.get(url);

        Map<String,String[]> params = req.getParameterMap();
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),new Object[]{req,resp,params});
    }

    private void doLoadConfig(String contextConfigLocation) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initHandlerMapping() {

        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(ZfController.class)) {
                continue;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(ZfRequestMapping.class)) {
                ZfRequestMapping requestMapping = clazz.getAnnotation(ZfRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //默认获取所有的public方法
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(ZfRequestMapping.class)) {
                    continue;
                }

                ZfRequestMapping requestMapping = method.getAnnotation(ZfRequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                System.out.println("Mapped:" + url + "," + method);
            }
        }

    }

    private void doAutowire() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {

            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(ZfAutowire.class)) {
                    continue;
                }
                ZfAutowire autowired = field.getAnnotation(ZfAutowire.class);

                String beanName = autowired.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {

        if (classNames.isEmpty()) {
            return;
        }

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                //存在此注解的类  isAnnotationPresent用于判断是否有此注解
                if (clazz.isAnnotationPresent(ZfController.class)) {
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(ZfService.class)) {
                    ZfService zfService = clazz.getAnnotation(ZfService.class);
                    String beanName = zfService.value();
                    //默认为类名首字母小写
                    if ("".equals(beanName.trim())) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);

                    for (Class<?> i : clazz.getInterfaces()) {
                        if (ioc.containsKey(i.getName())) {
                            throw new Exception("The “" + i.getSimpleName() + "” is exists!!");
                        }
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void doScanner(String scanPackage) {

        URL url = this.getClass().getClassLoader()
                .getResource("/" + scanPackage.replaceAll("\\.", "/"));
        System.out.println("URL:" + url);

        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                System.out.println("className:" + className);
                classNames.add(className);
            }
        }
    }

    //首字母小写
    private static String toLowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] = (char) (chars[0] + 32);
        return String.valueOf(chars);
    }
}
