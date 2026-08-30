# Mine Drama - Aplicativo de Mini Dramas & Streaming Vertical

Aplicativo Android nativo em **Kotlin** e **Jetpack Compose** para streaming de micro-dramas em formato vertical (estilo ReelShort / DramaBox / ShortMax).

---

## 🚀 Compilação Automática no GitHub (CI/CD)

Este repositório possui uma action configurada em `.github/workflows/build-apk.yml` pronta para compilar o APK automaticamente:

1. **Acesse a aba Actions** no seu repositório do GitHub (`https://github.com/SEU_USUARIO/SEU_REPOSITORIO/actions`).
2. Selecione o workflow **"Build Android APK"**.
3. Clique em **"Run workflow"** (ou faça um `git push` na branch `main`).
4. Ao finalizar a execução com sucesso, role até a seção **Artifacts** na página da execução e baixe o arquivo `mine-drama-debug-apk.zip` contendo o APK instalável (`.apk`).

---

## 🛠️ Tecnologias Utilizadas

- **Kotlin** & **Jetpack Compose** (Material 3)
- **Firebase Firestore** para catálogo e metadados em tempo real
- **Media3 / ExoPlayer** para reprodução vertical de vídeos fluida
- **Room Database** para histórico de reprodução e favoritos locais
- **Moshi** & **Kotlin Coroutines / Flow**
