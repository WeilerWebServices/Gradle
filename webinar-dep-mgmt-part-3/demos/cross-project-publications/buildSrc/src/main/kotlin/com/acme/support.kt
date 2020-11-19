package com.acme

import org.gradle.api.Named
import org.gradle.api.Project

inline fun <reified T : Named> Project.namedAttribute(value: String) = objects.named(T::class.java, value)
