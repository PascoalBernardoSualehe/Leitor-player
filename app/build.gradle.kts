plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.leitor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.leitor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // É necessário para que o Gradle encontre as bibliotecas nativas (.so) do ACRCloud
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("androidx.media:media:1.6.0")

    // Dependências do Room para SQLite
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-rxjava3:2.6.1")

    // Dependências do Glide para capas de álbuns
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Dependências do RxJava
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")

    // Biblioteca para Imagem Circular
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Dependências de Teste
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // ================== INÍCIO DA CORREÇÃO ==================
    // Adiciona o SDK local do ACRCloud que está na pasta 'libs'
    implementation(files("libs/acrcloud-universal-sdk-1.3.30.jar"))
    // =================== FIM DA CORREÇÃO ====================

    // ================== ADIÇÕES NECESSÁRIAS ==================
    // Para ActivityResultLauncher
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")

    // Para LocalBroadcastManager (se ainda estiver usando)
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // Para SearchView
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Para MediaSessionCompat
    implementation("androidx.media:media:1.6.0")

    // Para NotificationCompat
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.media:media:1.6.0")

    // Para RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // Para Lifecycle (se necessário)
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("com.google.code.gson:gson:2.10.1")
}