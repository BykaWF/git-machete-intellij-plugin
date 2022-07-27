import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `java-library`
  `kotlin-dsl`
  alias(libs.plugins.jetbrains.kotlin)
}

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(gradleApi())
  implementation(libs.aspectj.postCompileWeaving)
  implementation(libs.jsoup)
  implementation(libs.jetbrains.intellij)
  implementation(libs.jetbrains.kotlin)
  implementation(libs.spotless)
  implementation(libs.checkerFramework)
}

val javaMajorVersion = JavaVersion.VERSION_11

project.tasks.withType<KotlinCompile> {
//  TODO (#1004): Fix warnings and uncomment the line below
//  kotlinOptions.allWarningsAsErrors = true
  kotlinOptions.jvmTarget = javaMajorVersion.majorVersion
}