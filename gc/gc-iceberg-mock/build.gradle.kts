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

plugins { id("nessie-conventions-iceberg") }

extra["maven.name"] = "Nessie - GC - Mocked Iceberg data for tests"

dependencies {
  compileOnly(libs.iceberg.core)

  compileOnly(libs.errorprone.annotations)
  compileOnly(libs.immutables.value.annotations)
  annotationProcessor(libs.immutables.value.processor)

  implementation(libs.guava)
  implementation(platform(libs.jackson.bom))
  implementation("com.fasterxml.jackson.core:jackson-annotations")
  implementation("com.fasterxml.jackson.core:jackson-databind")
  compileOnly(libs.avro)

  implementation(libs.slf4j.api)

  compileOnly(libs.microprofile.openapi)

  compileOnly(libs.jakarta.validation.api)
  compileOnly(libs.jakarta.annotation.api)

  testRuntimeOnly(libs.logback.classic)

  testImplementation(libs.iceberg.core)

  testCompileOnly(libs.immutables.value.annotations)
  testAnnotationProcessor(libs.immutables.value.processor)

  testCompileOnly(libs.microprofile.openapi)

  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.bundles.junit.testing)
}
