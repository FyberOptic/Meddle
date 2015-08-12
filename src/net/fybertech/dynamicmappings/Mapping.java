package net.fybertech.dynamicmappings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Mapping
{
	String[] provides() default {};
	String[] depends() default {};
	
	String[] providesFields() default {};
	String[] dependsFields() default {};
	
	String[] providesMethods() default {};
	String[] dependsMethods() default {};
}