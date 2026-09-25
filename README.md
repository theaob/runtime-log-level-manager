# Runtime Log Level Manager

Kotlin/JavaFX uygulamalarında logger seviyelerini **çalışma anında** değiştirmek için drop-in
kütüphane. Spring Boot Admin'in *Loggers* ekranına benzer bir pencereyi uygulamanın içinden açar.

![Log Levels penceresi](docs/screenshot.png)

- Logback, Log4j2 ve java.util.logging desteği; hangisini kullandığınız otomatik algılanır
  (SLF4J bağlaması → Log4j2 core → JUL sırasıyla).
- Filtreleme, sadece yapılandırılmış / sadece bu oturumda değişen loggerları gösterme.
- Tek tıkla seviye değiştirme, `Reset` ile üst loggerdan miras almaya dönme.
- Henüz oluşturulmamış bir sınıf ya da paket için isimle seviye atama.
- `Revert changes` ile oturumdaki tüm değişiklikleri geri alma.
- Ek bağımlılık getirmez: JavaFX ve loglama kütüphanesi uygulamanızdan gelir.

## Kurulum

Maven Central üzerinden:

```kotlin
dependencies {
    implementation("tr.com.onurbaykal:log-level-manager:<sürüm>")
}
```

Yerel olarak denemek için: `./gradlew :log-level-manager:publishToMavenLocal` ve
`implementation("tr.com.onurbaykal:log-level-manager:0.1.0-SNAPSHOT")` (`mavenLocal()` reposu ile).

Gereksinimler: Java 11+, JavaFX 17+ (`javafx.controls`).

## Kullanım

```kotlin
override fun start(stage: Stage) {
    stage.scene = Scene(root)

    // Ctrl+Shift+L (macOS'ta Cmd+Shift+L) ile pencereyi açar
    LogLevelManager.install(stage.scene)

    // ya da bir menüye ekleyin
    menuBar.menus += Menu("Tools", null, LogLevelManager.createMenuItem())

    stage.show()
}
```

Diğer seçenekler:

```kotlin
LogLevelManager.show(ownerWindow)                  // pencereyi doğrudan aç (her thread'den çağrılabilir)
val view = LogLevelManager.createView()            // kendi Tab/Dialog'unuza gömün
LogLevelManager.setLevel(MyService::class, LogLevel.DEBUG)
LogLevelManager.setLevel("com.example.db", LogLevel.TRACE)
LogLevelManager.setLevel("com.example.db", null)    // miras almaya geri dön
LogLevelManager.revertAll()
scene.installLogLevelManager(KeyCombination.keyCombination("F12"))
```

Pencereyi yalnızca geliştirme ya da destek modunda açmak isterseniz `install` çağrısını bir
bayrağın (ör. `-Ddebug.tools=true`) arkasına koymanız yeterli.

### Arayüzdeki renkler

- **Dolu buton**: seviye bu loggerda açıkça ayarlanmış (`Reset` görünür).
- **Çerçeveli buton**: seviye bir üst loggerdan miras alınıyor.
- **Solda mavi çizgi**: logger bu oturumda değiştirildi.

## Notlar

- Değişiklikler kalıcı değildir; uygulama yeniden başladığında konfigürasyon dosyanız geçerli olur.
  Logback'in `scan="true"` ya da Log4j2'nin `monitorInterval` özelliği dosyayı yeniden yüklerse
  arayüzden yapılan değişiklikler de sıfırlanır.
- **java.util.logging**: handler'ların kendi seviyeleri vardır (varsayılan `ConsoleHandler` INFO).
  Bir loggeri DEBUG/TRACE'e çektiğinizde çıktı görmek için handler seviyesini de düşürmeniz gerekir.
- Farklı bir loglama altyapısı için `LoggingBackend` arayüzünü uygulayıp
  `LogLevelManager.backend = MyBackend()` ile ya da `META-INF/services/tr.com.onurbaykal.loglevel.LoggingBackend`
  dosyasıyla kaydedebilirsiniz.
- Modüler (JPMS) uygulamalarda modül adı `tr.com.onurbaykal.loglevel`'dir.

## Geliştirme

```bash
./gradlew :log-level-manager:test   # backend testleri
./gradlew :sample:run               # örnek uygulama (Logback)
```

## CI ve sürüm yayınlama

- `.github/workflows/ci.yml`: `main`'e her push'ta ve her PR'da Linux, Windows ve macOS üzerinde
  `./gradlew build` çalıştırır.
- `.github/workflows/release.yml`: `v` ile başlayan bir tag push'landığında (ör. `v0.1.0`) o sürümü
  imzalayıp Maven Central'a yayınlar ve bir GitHub Release oluşturur.

```bash
git tag v0.1.0
git push origin v0.1.0
```

### Tek seferlik kurulum

1. [central.sonatype.com](https://central.sonatype.com) üzerinde hesap açın ve `tr.com.onurbaykal`
   namespace'ini ekleyin. Doğrulama için `onurbaykal.com.tr` alan adına portalın verdiği DNS TXT
   kaydını ekleyin.
2. Portalda *View Account → Generate User Token* ile bir kullanıcı token'ı oluşturun.
3. Bir GPG anahtarı oluşturup açık anahtarı bir keyserver'a yükleyin:

   ```bash
   gpg --quick-gen-key "Onur Baykal <e-posta>" rsa4096 sign 2y
   gpg --list-keys --keyid-format short          # anahtar kimliği, ör. 1A2B3C4D
   gpg --keyserver keyserver.ubuntu.com --send-keys <ANAHTAR_KİMLİĞİ>
   gpg --armor --export-secret-keys <ANAHTAR_KİMLİĞİ>   # SIGNING_KEY secret'ının değeri
   ```

4. GitHub'da *Settings → Secrets and variables → Actions* altında şu repository secret'larını ekleyin:

   | Secret | Değer |
   |---|---|
   | `MAVEN_CENTRAL_USERNAME` | User token kullanıcı adı |
   | `MAVEN_CENTRAL_PASSWORD` | User token parolası |
   | `SIGNING_KEY` | ASCII-armored gizli GPG anahtarı |
   | `SIGNING_KEY_ID` | Anahtar kimliğinin son 8 karakteri |
   | `SIGNING_KEY_PASSWORD` | GPG anahtarının parolası |
