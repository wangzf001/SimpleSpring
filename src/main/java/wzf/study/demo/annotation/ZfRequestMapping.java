package wzf.study.demo.annotation;

import java.lang.annotation.*;

/**
 * @author 王振方
 * @date 2020/4/21
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZfRequestMapping {

    String value() default "";
}
