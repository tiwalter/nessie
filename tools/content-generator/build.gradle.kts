/*
 * Copyright (C) 2022 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  alias(libs.plugins.nessie.run)
  id("nessie-conventions-server")
  id("nessie-jacoco")
  id("nessie-shadow-jar")
}

dependencies {
  implementation(project(":nessie-client"))

  implementation(libs.picocli)
  // TODO help picocli to make their annotation-processor incremental
  annotationProcessor(libs.picocli.codegen)
  implementation(libs.guava)

  compileOnly(libs.jakarta.validation.api)
  compileOnly(libs.jakarta.annotation.api)

  compileOnly(libs.microprofile.openapi)
  runtimeOnly(libs.slf4j.api)

  implementation(platform(libs.jackson.bom))
  implementation("com.fasterxml.jackson.core:jackson-annotations")
  implementation("com.fasterxml.jackson.core:jackson-databind")

  compileOnly(libs.immutables.builder)
  compileOnly(libs.immutables.value.annotations)
  annotationProcessor(libs.immutables.value.processor)

  testFixturesImplementation(project(":nessie-client"))

  testFixturesApi(platform(libs.junit.bom))
  testFixturesApi(libs.bundles.junit.testing)

  testFixturesApi(libs.microprofile.openapi)
  testFixturesCompileOnly(libs.picocli)
  testFixturesCompileOnly(platform(libs.jackson.bom))
  testFixturesCompileOnly("com.fasterxml.jackson.core:jackson-annotations")

  testImplementation(project(":nessie-jaxrs-testextension"))

  testImplementation(project(":nessie-versioned-storage-inmemory"))

  nessieQuarkusServer(project(":nessie-quarkus", "quarkusRunner"))
}

nessieQuarkusApp {
  includeTask(tasks.named<Test>("intTest"))
  environmentNonInput.put("HTTP_ACCESS_LOG_LEVEL", testLogLevel())
  jvmArgumentsNonInput.add("-XX:SelfDestructTimer=30")
  systemProperties.put("nessie.server.send-stacktrace-to-client", "true")
}

tasks.named<ShadowJar>("shadowJar").configure {
  manifest {
    attributes["Main-Class"] = "org.projectnessie.tools.contentgenerator.cli.NessieContentGenerator"
  }
}

tasks.named<Test>("intTest").configure { systemProperty("expectedNessieVersion", project.version) }
