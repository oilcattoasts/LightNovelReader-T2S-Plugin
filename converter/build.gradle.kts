plugins { `java-library` }
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
val verification = sourceSets.create("verification") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
tasks.register<JavaExec>("verifyConverter") {
    dependsOn(tasks.named(verification.classesTaskName))
    classpath = verification.runtimeClasspath
    mainClass.set("org.lnr.t2s.converter.TraditionalToSimplifiedTest")
    args(rootProject.file("plugin/src/main/assets/opencc").absolutePath)
}
tasks.check { dependsOn("verifyConverter") }
