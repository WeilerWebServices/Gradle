/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationprocessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

import org.springframework.boot.configurationprocessor.fieldvalues.FieldValuesParser;
import org.springframework.boot.configurationprocessor.fieldvalues.javac.JavaCompilerFieldValuesParser;
import org.springframework.boot.configurationprocessor.metadata.ItemDeprecation;

/**
 * Provide utilities to detect and validate configuration properties.
 *
 * @author Stephane Nicoll
 */
class MetadataGenerationEnvironment {

	private static final String NULLABLE_ANNOTATION = "org.springframework.lang.Nullable";

	private final Set<String> typeExcludes;

	private final TypeUtils typeUtils;

	private final Elements elements;

	private final FieldValuesParser fieldValuesParser;

	private final Map<TypeElement, Map<String, Object>> defaultValues = new HashMap<>();

	private final String configurationPropertiesAnnotation;

	private final String nestedConfigurationPropertyAnnotation;

	private final String deprecatedConfigurationPropertyAnnotation;

	private final String endpointAnnotation;

	private final String readOperationAnnotation;

	MetadataGenerationEnvironment(ProcessingEnvironment environment,
			String configurationPropertiesAnnotation,
			String nestedConfigurationPropertyAnnotation,
			String deprecatedConfigurationPropertyAnnotation, String endpointAnnotation,
			String readOperationAnnotation) {
		this.typeExcludes = determineTypeExcludes();
		this.typeUtils = new TypeUtils(environment);
		this.elements = environment.getElementUtils();
		this.fieldValuesParser = resolveFieldValuesParser(environment);
		this.configurationPropertiesAnnotation = configurationPropertiesAnnotation;
		this.nestedConfigurationPropertyAnnotation = nestedConfigurationPropertyAnnotation;
		this.deprecatedConfigurationPropertyAnnotation = deprecatedConfigurationPropertyAnnotation;
		this.endpointAnnotation = endpointAnnotation;
		this.readOperationAnnotation = readOperationAnnotation;
	}

	private static Set<String> determineTypeExcludes() {
		Set<String> excludes = new HashSet<>();
		excludes.add("com.zaxxer.hikari.IConnectionCustomizer");
		excludes.add("groovy.text.markup.MarkupTemplateEngine");
		excludes.add("java.io.Writer");
		excludes.add("java.io.PrintWriter");
		excludes.add("java.lang.ClassLoader");
		excludes.add("java.util.concurrent.ThreadFactory");
		excludes.add("javax.jms.XAConnectionFactory");
		excludes.add("javax.sql.DataSource");
		excludes.add("javax.sql.XADataSource");
		excludes.add("org.apache.tomcat.jdbc.pool.PoolConfiguration");
		excludes.add("org.apache.tomcat.jdbc.pool.Validator");
		excludes.add("org.flywaydb.core.api.callback.FlywayCallback");
		excludes.add("org.flywaydb.core.api.resolver.MigrationResolver");
		return excludes;
	}

	private static FieldValuesParser resolveFieldValuesParser(ProcessingEnvironment env) {
		try {
			return new JavaCompilerFieldValuesParser(env);
		}
		catch (Throwable ex) {
			return FieldValuesParser.NONE;
		}
	}

	public TypeUtils getTypeUtils() {
		return this.typeUtils;
	}

	public Object getDefaultValue(TypeElement type, String name) {
		return this.defaultValues.computeIfAbsent(type, this::resolveFieldValues)
				.get(name);
	}

	public boolean isExcluded(TypeMirror type) {
		if (type == null) {
			return false;
		}
		String typeName = type.toString();
		if (typeName.endsWith("[]")) {
			typeName = typeName.substring(0, typeName.length() - 2);
		}
		return this.typeExcludes.contains(typeName);
	}

	public boolean isDeprecated(Element element) {
		if (isElementDeprecated(element)) {
			return true;
		}
		if (element instanceof VariableElement || element instanceof ExecutableElement) {
			return isElementDeprecated(element.getEnclosingElement());
		}
		return false;
	}

	public ItemDeprecation resolveItemDeprecation(Element element) {
		AnnotationMirror annotation = getAnnotation(element,
				this.deprecatedConfigurationPropertyAnnotation);
		String reason = null;
		String replacement = null;
		if (annotation != null) {
			Map<String, Object> elementValues = getAnnotationElementValues(annotation);
			reason = (String) elementValues.get("reason");
			replacement = (String) elementValues.get("replacement");
		}
		reason = "".equals(reason) ? null : reason;
		replacement = "".equals(replacement) ? null : replacement;
		return new ItemDeprecation(reason, replacement);
	}

	public boolean hasAnnotation(Element element, String type) {
		return getAnnotation(element, type) != null;
	}

	public AnnotationMirror getAnnotation(Element element, String type) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return annotation;
				}
			}
		}
		return null;
	}

	public Map<String, Object> getAnnotationElementValues(AnnotationMirror annotation) {
		Map<String, Object> values = new LinkedHashMap<>();
		annotation.getElementValues().forEach((name, value) -> values
				.put(name.getSimpleName().toString(), value.getValue()));
		return values;
	}

	public TypeElement getConfigurationPropertiesAnnotationElement() {
		return this.elements.getTypeElement(this.configurationPropertiesAnnotation);
	}

	public AnnotationMirror getConfigurationPropertiesAnnotation(Element element) {
		return getAnnotation(element, this.configurationPropertiesAnnotation);
	}

	public AnnotationMirror getNestedConfigurationPropertyAnnotation(Element element) {
		return getAnnotation(element, this.nestedConfigurationPropertyAnnotation);
	}

	public TypeElement getEndpointAnnotationElement() {
		return this.elements.getTypeElement(this.endpointAnnotation);
	}

	public AnnotationMirror getReadOperationAnnotation(Element element) {
		return getAnnotation(element, this.readOperationAnnotation);
	}

	public boolean hasNullableAnnotation(Element element) {
		return getAnnotation(element, NULLABLE_ANNOTATION) != null;
	}

	private boolean isElementDeprecated(Element element) {
		return hasAnnotation(element, "java.lang.Deprecated")
				|| hasAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
	}

	private Map<String, Object> resolveFieldValues(TypeElement element) {
		Map<String, Object> values = new LinkedHashMap<>();
		resolveFieldValuesFor(values, element);
		return values;
	}

	private void resolveFieldValuesFor(Map<String, Object> values, TypeElement element) {
		try {
			this.fieldValuesParser.getFieldValues(element).forEach((name, value) -> {
				if (!values.containsKey(name)) {
					values.put(name, value);
				}
			});
		}
		catch (Exception ex) {
			// continue
		}
		Element superType = this.typeUtils.asElement(element.getSuperclass());
		if (superType instanceof TypeElement
				&& superType.asType().getKind() != TypeKind.NONE) {
			resolveFieldValuesFor(values, (TypeElement) superType);
		}
	}

}
