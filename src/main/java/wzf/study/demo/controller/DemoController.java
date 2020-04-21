package wzf.study.demo.controller;

import wzf.study.demo.annotation.ZfAutowire;
import wzf.study.demo.annotation.ZfController;
import wzf.study.demo.annotation.ZfRequestMapping;
import wzf.study.demo.annotation.ZfRequestParam;
import wzf.study.demo.service.impl.DemoServiceImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author 王振方
 * @date 2020/4/21
 */
@ZfController("demoController")
@ZfRequestMapping("/demo")
public class DemoController {

    @ZfAutowire
    private DemoServiceImpl demoService;

    @ZfRequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @ZfRequestParam("name") String name) {
        String result = demoService.get(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @ZfRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @ZfRequestParam("a") Integer a, @ZfRequestParam("b") Integer b) {
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
