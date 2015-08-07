package net.fybertech.meddle;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface MeddleMod
{
	String id();
	String name() default "";
	String author() default "";
	String version() default "";
	String[] depends() default {};
}
