plugins {
	java
	war
	id("org.springframework.boot") version "2.7.9"
	id("io.spring.dependency-management") version "1.0.15.RELEASE"
	kotlin("jvm")
}

group = "com.airasia"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11


repositories {
	mavenCentral()
	mavenLocal()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
	maven { url = uri("https://s01.oss.sonatype.org/content/groups/staging") }
	maven {
		url = uri("https://maven.pkg.github.com/edna-aa/sqldelight")
		credentials {
			username = "edna-aa"
			password = ""
		}
		// Restrict this repository to specific versions containing "-wasm"
		content {
			includeGroup("app.cash.sqldelight") // Restrict to the group
			includeVersionByRegex("app.cash.sqldelight", ".*", ".*-wasm.*") // Match any artifact in the group with versions containing "-wasm"
		}
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")
	testImplementation("org.springframework.boot:spring-boot-starter-test")


//	implementation("io.telereso.kmp:core-jvm:0.0.2-local")
	implementation(project(":core"))

}

tasks.withType<Test> {
	useJUnitPlatform()
}
