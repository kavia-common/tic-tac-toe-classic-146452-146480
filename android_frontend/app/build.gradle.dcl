androidApplication {
    namespace = "org.example.app"

    dependencies {
        implementation("org.apache.commons:commons-text:1.11.0")
        implementation(project(":utilities"))
        implementation("androidx.cardview:cardview:1.0.0")
        implementation("com.google.android.material:material:1.11.0")
    }
}
