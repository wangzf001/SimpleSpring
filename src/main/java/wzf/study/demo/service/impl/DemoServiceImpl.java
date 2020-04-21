package wzf.study.demo.service.impl;

import wzf.study.demo.annotation.ZfService;
import wzf.study.demo.service.IDemoService;

/**
 * @author 王振方
 * @date 2020/4/21
 */
@ZfService("demoService")
public class DemoServiceImpl implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is " + name;
    }
}
