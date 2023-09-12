plugins {
    id("mache")
}

mache {
    minecraftVersion.set("1.20.1")
    minecraftJarType.set("client")
}

dependencies {
    codebook("1.0.6")
    remapper(art("1.0.5"))
    decompiler("org.vineflower:vineflower:+")
    parchment("1.20.1", "2023.07.30")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
}

// pass flags to javac
gradle.projectsEvaluated {
    tasks.withType(JavaCompile::class) {
        options.compilerArgs.addAll(arrayOf("-Xmaxerrs", "1000"))
    }
}
