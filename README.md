# 🛡️ Quiet

**Versão 1.0 - Jaguatirica**

App Android que **bloqueia 100% das chamadas** que não estão na sua **lista de confiança** (whitelist). Feito para o povo brasileiro cansado de golpes de ligação, ligações mudas que gravam sua voz, e spam telefônico.

> **Proteção vitalícia contra golpes.** Quem não está na sua lista é **desligado na cara** — sem toque, sem chamada perdida, sem notificação, sem susto.

**Application ID:** `org.floatingskies.Quiet`
**Desenvolvedor:** Ariel Closs
**Contato:** arielcloss@gmail.com

---

## ✨ O que o app faz

- ✅ **Whitelist de confiança**: só quem você autorizar pode te ligar
- ✅ **Bloqueio silencioso**: a chamada é recusada e desligada sem você saber
- ✅ **Sem chamada perdida**: não aparece no registro de chamadas
- ✅ **Sem notificação**: nada de "fulano tentou te ligar"
- ✅ **Bloqueio de números ocultos/privados** (configurável)
- ✅ **Modo paranóia**: se a lista estiver vazia, **TODAS** as chamadas são bloqueadas
- ✅ **Log de chamadas bloqueadas**: veja quem tentou e quantas vezes
- ✅ **Exportar CSV**: para denúncia na Anatel/Polícia Civil
- ✅ **Não substitui o Google Telefone**: você só autoriza o Quiet a filtrar chamadas (via `ROLE_CALL_SCREENING`)
- ✅ **Ativação vitalícia**: 1 doação, código de 9 linhas, funciona para sempre
- ✅ **Compartilhável**: o código pode ser enviado a familiares
- ✅ **Funciona offline**: a validação do código é 100% local (SHA-256)

---

## 🆓 Versão gratuita vs 💛 Versão vitalícia

| Recurso | Gratuita | Vitalícia (doador) |
|---------|----------|---------------------|
| **Contatos na whitelist** | até **11** | **∞ ILIMITADO** |
| Bloqueio silencioso | ✅ | ✅ |
| Bloqueio de ocultos | ✅ | ✅ |
| Log de bloqueadas | ✅ | ✅ |
| Exportar CSV | ✅ | ✅ |
| Atualizações futuras | ✅ | ✅ |
| **Preço** | R$ 0,00 | **R$ 4,99 (doação única vitalícia)** |

---

## 💛 Doação vitalícia — R$ 4,99

Esta é uma **doação**, não uma compra. Ela ajuda o desenvolvedor a manter o projeto vivo, pagar servidores e melhorar o app para todos os brasileiros.

### Chaves PIX (NUBANK — Ariel Closs)

| Tipo | Chave |
|------|-------|
| 📞 **Telefone** | `+55 69 9342-7132` |
| 📧 **Email** | `arielcloss@gmail.com` |

### Fluxo de ativação

1. Usuário paga **R$ 4,99** via PIX (escaneia o QR Code no app ou usa uma das chaves acima)
2. Informa o email dele e o ID do comprovante dentro do app
3. O desenvolvedor (Ariel) recebe a notificação do PIX no NUBANK
4. Gera um código de 9 linhas com `ferramentas/gerar_codigo.py`
5. Envia o código por email **do `arielcloss@gmail.com`** para o cliente
6. Cliente cola o código na tela de ativação do app → pronto!

> ⚠️ O app em si não envia email automaticamente. O desenvolvedor precisa gerar o código e enviar manualmente por email. Em uma versão futura, pode-se adicionar um backend (ex.: Google Apps Script + Gmail API) para automatizar o fluxo.

---

## 🌍 Compatibilidade

| Android | Versão | Status | Método de bloqueio |
|---------|--------|--------|---------------------|
| 5.0-5.1 | Lollipop | ✅ Funciona com limitações | `ITelephony.endCall()` via reflexão |
| 6.0 | Marshmallow | ✅ Funciona | `ITelephony.endCall()` via reflexão |
| 7.0-9.0 | Nougat-Pie | ✅ **100% silencioso** | `CallScreeningService` (direto) |
| 10-14 | Q-UpsideDownCake | ✅ **100% silencioso** | `CallScreeningService` (via `ROLE_CALL_SCREENING`) |

### 🎯 Não substitui o discador padrão (Android 10+)

No Android 10+, o Quiet usa o **`ROLE_CALL_SCREENING`** (via `RoleManager`) para ser autorizado como "app de filtragem de chamadas". Isso significa:

- ✅ **O Google Telefone (ou Samsung Phone, etc.) continua sendo o discador padrão**
- ✅ Você faz e recebe ligações normalmente pelo discador nativo
- ✅ O Quiet só decide se bloqueia ou não cada chamada recebida (callback do `CallScreeningService`)
- ✅ Mesma abordagem usada por Truecaller, Should I Answer?, etc.

### ⚠️ Limitações conhecidas (Android 5.0-6.0)

Nestas versões antigas, o Android não tem API oficial para bloqueio silencioso. O app usa reflexão para chamar `endCall()` do `ITelephony`. Pode ocorrer:

- 🟡 A chamada tocar 1 ring antes de ser desligada (depende do fabricante)
- 🟡 Aparecer como "chamada perdida" em alguns aparelhos (LG, Xiaomi antigos)
- 🔴 Em alguns aparelhos com root/ROM custom, pode não funcionar

**Para Android 7+ o bloqueio é 100% silencioso e oficial via `CallScreeningService`.**

### 📱 Suporte a fabricantes

Testado e compatível com:
- ✅ **Android puro** (Pixel, Nexus, Motorola, Android One)
- ✅ **MIUI** (Xiaomi, Redmi, Poco) — exige "Auto-iniciar" ligado
- ✅ **OneUI** (Samsung Galaxy) — funciona perfeitamente
- ✅ **LG UI** (LG K-series, G-series) — funciona
- ✅ **EMUI** (Huawei) — exige "Proteção de bateria" desligada para o app
- ✅ **ColorOS** (Oppo, Realme) — exige "Iniciar em segundo plano" permitido

---

## 📲 Como compilar o APK

### Pré-requisitos

1. **Android Studio Hedgehog ou superior** (download: https://developer.android.com/studio)
2. **JDK 17** (recomendado: jbr-17 embutido ou OpenJDK 17)
3. **Android SDK Platform 34** (Android 14) — Android Studio instala automaticamente
4. **Build Tools 34.0.0**
5. Internet para baixar as dependências do Gradle pela primeira vez

### Passo a passo

```bash
# 1. Copie a pasta BloqueadorChamadasBR para seu computador
# 2. Abra o Android Studio → "Open" → selecione a pasta BloqueadorChamadasBR

# 3. Escolha o Gradle JDK: jbr-17 (preferencial) ou Embedded JDK
# 4. Aguarde o Gradle sincronizar (5-15 min na primeira vez)

# 5. Para gerar o APK de debug (para testar):
#    Menu Build → Build Bundle(s)/APK(s) → Build APK(s)

# 6. Para gerar o APK de release (para distribuir):
#    Menu Build → Generate Signed Bundle / APK → APK
#    Crie uma keystore (primeira vez) e preencha os dados
```

O APK será gerado em:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

### Compilar via linha de comando (alternativa)

```bash
# Linux/Mac
./gradlew assembleRelease

# Windows
gradlew.bat assembleRelease
```

---

## 🔑 Sistema de ativação (códigos de 9 linhas)

### Como funciona

1. O app gera um código de 9 linhas no formato `XXXX-XXXX-XXXX` por linha
2. As 8 primeiras linhas são aleatórias (corpo)
3. A 9ª linha é a **assinatura** = SHA-256(corpo + segredo) → 12 chars → formatados
4. O app valida offline: recalcula a assinatura e compara com a 9ª linha
5. **Sem internet necessária** para validar

### Gerar códigos para clientes doadores

Use o script Python na pasta `ferramentas/`:

```bash
cd ferramentas

# Gera 1 código
python3 gerar_codigo.py

# Gera 5 códigos
python3 gerar_codigo.py -n 5

# Gera 10 códigos e salva em arquivo
python3 gerar_codigo.py -n 10 -o codigos_clientes.txt

# Formato compacto (com pipes | para enviar por WhatsApp)
python3 gerar_codigo.py --compacto
```

**Exemplo de código gerado:**
```
0Z1R-7SR1-Y0U0
CUQF-L80H-QUSI
2EWN-BF7C-P21Z
96UU-X0ZV-MZZE
4O8R-3ERD-OYOT
TXDY-E03L-K3RV
6D55-BPVC-DRFQ
AY70-Y8QI-DL4L
6435-4860-50F0
```

### Fluxo recomendado para enviar o código

1. Cliente paga **R$ 4,99** via PIX (chaves: `+55 69 9342-7132` ou `arielcloss@gmail.com`)
2. Cliente informa o email dele dentro do app
3. Você (Ariel) recebe a notificação do PIX no NUBANK
4. Gera o código: `python3 ferramentas/gerar_codigo.py -n 1`
5. Copia o código
6. Envia um email do `arielcloss@gmail.com` para o cliente, com:
   - Assunto: "Seu código vitalício Quiet 🛡️"
   - Corpo: o código de 9 linhas + instruções para colar no app
7. Cliente cola o código na tela de ativação → vitalício liberado!

---

## 🏗️ Estrutura do projeto

```
BloqueadorChamadasBR/
├── app/
│   ├── build.gradle                          # Configurações do módulo + dependências
│   ├── proguard-rules.pro                    # Regras de ofuscação
│   └── src/main/
│       ├── AndroidManifest.xml               # Permissões + declaração de serviços
│       ├── java/org/floatingskies/quiet/
│       │   ├── App.kt                        # Application class (init)
│       │   ├── MainActivity.kt               # Dashboard principal
│       │   ├── data/                         # Camada Room (Whitelist + BlockedCalls)
│       │   ├── service/
│       │   │   └── CallBlockerService.kt     # ⭐ CallScreeningService (bloqueio silencioso)
│       │   ├── receiver/
│       │   │   ├── CallReceiver.kt           # Receiver legado (Android 5-6)
│       │   │   └── BootReceiver.kt           # Reativa no boot
│       │   ├── ui/
│       │   │   ├── dialer/
│       │   │   │   └── ProxyDialerActivity.kt # Fallback para discador padrão
│       │   │   ├── onboarding/               # Tela inicial + permissões
│       │   │   ├── whitelist/                # Lista de confiança
│       │   │   ├── payment/                  # Pagamento PIX
│       │   │   ├── activation/               # Ativação por código
│       │   │   ├── blocked/                  # Log de bloqueadas
│       │   │   └── settings/                # Configurações
│       │   └── util/
│       │       ├── PhoneUtils.kt             # Normalização de números BR
│       │       ├── ActivationValidator.kt    # Validação SHA-256
│       │       ├── PermissionHelper.kt       # Permissões + ROLE_CALL_SCREENING
│       │       └── PrefsManager.kt           # SharedPreferences
│       └── res/
│           ├── layout/                       # Telas XML
│           ├── values/                       # Cores, strings (PT-BR), estilos
│           ├── drawable/                     # Ícones vetoriais + logo PNG
│           └── mipmap-*/                     # Ícone do launcher em 5 densidades
├── ferramentas/
│   └── gerar_codigo.py                       # Gerador de códigos de ativação
├── build.gradle                              # Top-level
├── settings.gradle
└── gradle.properties
```

---

## 🔧 Tecnologias

- **Linguagem**: Kotlin 1.9.20
- **minSdk**: 21 (Android 5.0 Lollipop)
- **targetSdk**: 34 (Android 14)
- **UI**: Material Design 3 (escuro, foco em acessibilidade)
- **DB**: Room 2.6.1
- **QR Code**: ZXing + zxing-android-embedded
- **Build**: Gradle 8.2 + Android Gradle Plugin 8.1.4
- **Logo**: `org.floatingskies.Quiet.png` (PNG 512x512 RGBA, em todas as densidades)

---

## 📜 Versionamento

| Versão | Codinome | Descrição |
|--------|----------|-----------|
| 1.0 | **Jaguatirica** | Versão inicial pública. Bloqueio silencioso via CallScreeningService, whitelist, doação vitalícia R$ 4,99, role-based call screening. |

---

## ⚖️ Aviso legal

Este app é uma **ferramenta de proteção pessoal**. Não substitui:

- 🚨 **Denúncia formal** na Anatel (https://www.anatel.gov.br/consumidor)
- 🚨 **Boletim de ocorrência** na Polícia Civil em caso de golpe consumado
- 🚨 **Notificação** ao Procon em caso de cobrança indevida

O app bloqueia chamadas baseado **apenas no número de telefone**. Não identifica a intenção do chamador. Cabe ao usuário manter sua whitelist atualizada com números legítimos (banco, médico, familiares, etc.).

> **Dica**: Ao adicionar o número do seu banco à whitelist, confirme o número oficial no site ou app do banco. Nunca adicione números recebidos por SMS ou WhatsApp suspeitos.

---

## 🆘 Solução de problemas

| Problema | Solução |
|----------|---------|
| "O app não bloqueia nada" | Verifique se concedeu o **ROLE_CALL_SCREENING** (Android 10+) na tela inicial |
| "Não aparece o diálogo de filtragem de chamadas" | Toque em "App de filtragem de chamadas" na tela inicial do app |
| "Aparece chamada perdida mesmo assim" | Você está em Android 5.0-6.0 (limitação do sistema). Em 7+ funciona 100% silencioso |
| "O app para de funcionar depois de um tempo" | Ative "Ignorar otimização de bateria" nas permissões |
| "MIUI desliga o app em segundo plano" | Ative "Auto-iniciar" nas configurações do MIUI → Apps → Quiet |
| "Não consigo receber ligação do banco" | Adicione o número do banco na whitelist (use o número oficial, não o que aparece no SMS) |
| "Doei mas não recebi o código" | Verifique o spam do email. O remetente é `arielcloss@gmail.com`. Se não chegou em 30min, contate o desenvolvedor |

---

## 📞 Suporte

- **Email do desenvolvedor:** arielcloss@gmail.com

Para reportar bugs ou sugerir melhorias, envie um email para o desenvolvedor.

---

**Made com ❤️ para o Brasil.**
*Versão 1.0 - Jaguatirica*
