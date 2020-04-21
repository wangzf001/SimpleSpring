package wzf.study.demo.annotation;

import java.lang.annotation.*;

/**
 * @author 王振方
 * @date 2020/4/21
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZfRequestParam {

    String value() default "";
}
