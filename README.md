# 🌊 Litoral Novelas - Histórias que Emocionam

Aplicativo Android nativo desenvolvido em **Kotlin** e **Jetpack Compose** (Material Design 3) para streaming de novelas e minisséries em formato vertical, com persistência em nuvem via **Firebase (Auth, Firestore e Cloud Storage)**.

---

## 📱 Como Gerar e Baixar o APK no GitHub Actions

O repositório está 100% configurado com um fluxo de CI/CD automatizado em `.github/workflows/build-apk.yml` para compilar e gerar o APK instalável diretamente no GitHub:

### Opção 1: Gerar Manualmente pelo GitHub (Sem precisar de commit)
1. Acesse o seu repositório no GitHub.
2. Clique na aba **Actions** no topo.
3. No menu lateral esquerdo, selecione o workflow **"Build & Release Litoral Novelas APK"**.
4. Clique no botão **"Run workflow"** no canto direito.
5. (Opcional) Marque a caixa se desejar que o APK seja publicado diretamente como uma **GitHub Release**.
6. Clique em **"Run workflow"**.

### Opção 2: Gerar Automaticamente com Push
Qualquer `git push` feito na branch `main` ou `master` (ou ao enviar uma tag como `v1.0.0`) disparará a compilação automática do APK.

### 📥 Onde Baixar o APK Gerado:
- **Nos Artifacts da Execução**: Clique na execução concluída na aba **Actions**, role até a seção **Artifacts** no final da página e faça o download do pacote **`LitoralNovelas-APK`**.
- **Nas Releases**: Se você enviou uma tag (ex: `v1.0.0`) ou marcou a opção de release, o arquivo **`LitoralNovelas-app-debug.apk`** estará disponível diretamente na aba **Releases** do repositório para download e instalação direta no smartphone Android.

---

## ✨ Principais Funcionalidades

- **Streaming Vertical Fluido**: Player com ExoPlayer/Media3, suporte a rolagem vertical rápida, toque duplo para curtir, controle de volume, tela cheia e barra de reprodução interativa.
- **Nuvem Firebase Completa**:
  - **Firebase Cloud Storage**: Upload de vídeos MP4 e capas de episódios com barra de progresso em tempo real (0% a 100%).
  - **Cloud Firestore**: Banco de dados online em tempo real para catálogo global, sincronização de episódios, curtidas e visualizações.
  - **Firebase Auth & Credential Manager**: Login com Google e E-mail/Senha, recuperação de senha e gestão de perfis.
- **Modo Offline & Cache**: Persistência local com Room Database para acesso contínuo.
- **Diálogo de Saída Seguro**: Confirmação ao pressionar o botão voltar do Android para evitar saídas acidentais.

