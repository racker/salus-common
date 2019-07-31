package com.rackspace.salus.common.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Import;

/**
 * When enabled on an application's {@link org.springframework.context.annotation.Configuration} bean,
 * this will register an {@link ErrorAttributes} bean that augments the standard response content with:
 * <ul>
 *   <li><code>app</code> : the value of the <code>spring.application.name</code> property</li>
 *   <li><code>host</code> : the value of the <code>localhost.name</code> property</li>
 * </ul>
 * <p>
 *   <em>NOTE:</em> this requires <code>spring.application.name</code> to be configured in
 *   <code>application.yml</code> (or similar), which is why this needs to be explicitly enabled.
 * </p>
 * @see com.rackspace.salus.common.env.LocalhostPropertySourceProcessor
 * @see ExtendedErrorAttributesConfig
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ExtendedErrorAttributesConfig.class)
public @interface EnableExtendedErrorAttributes {

}
