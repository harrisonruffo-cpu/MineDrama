# 🎬 Mine Drama - Aplicativo de Mini Dramas & Novelas Verticais

Aplicativo Android nativo desenvolvido em **Kotlin** e **Jetpack Compose** (Material Design 3) para streaming de micro-dramas em formato vertical (estilo ReelShort / DramaBox / ShortMax).

---

## 📱 Como Gerar e Baixar o APK no GitHub Actions

O repositório já está configurado com um fluxo de CI/CD automatizado em `.github/workflows/build-apk.yml` pronto para gerar o APK instalável diretamente no GitHub:

### Opção 1: Gerar Manualmente pelo GitHub (Sem precisar de commit)
1. Acesse o seu repositório no GitHub.
2. Clique na aba **Actions** no topo.
3. No menu lateral esquerdo, selecione o workflow **"Build & Release Android APK"**.
4. Clique no botão **"Run workflow"** no canto direito.
5. (Opcional) Marque a caixa se desejar que o APK seja publicado diretamente como uma **GitHub Release**.
6. Clique em **"Run workflow"**.

### Opção 2: Gerar Automaticamente com Push
Qualquer `git push` feito na branch `main` ou `master` (ou ao enviar uma tag como `v1.0.0`) disparará a compilação automática do APK.

### 📥 Onde Baixar o APK Gerado:
- **Nos Artifacts da Execução**: Clique na execução concluída na aba **Actions**, role até a seção **Artifacts** no final da página e faça o download do pacote **`MineDrama-APK`**.
- **Nas Releases**: Se você enviou uma tag (ex: `v1.0.0`) ou ativou a opção de release, o arquivo **`MineDrama-app-debug.apk`** estará disponível diretamente na aba **Releases** do repositório para download direto no celular.

---

## ✨ Principais Funcionalidades

- **Streaming Vertical Fluido**: Player com ExoPlayer/Media3, suporte a rolagem vertical, duplo toque para curtir e reprodução em tela cheia.
- **Login e Gerenciamento de Contas Google**: Suporte a login rápido com múltiplas contas Google e troca ágil de perfis.
- **Publicação e Gerenciamento de Novelas**: Faça upload de vídeos do dispositivo, selecione imagens de capa personalizadas e renomeie episódios ou novelas a qualquer momento.
- **Catálogo & Nuvem**: Integração com Firebase Firestore e armazenamento offline local com Room Database.
- **Histórico & Favoritos**: Sincronização de progresso e lista de favoritos.
