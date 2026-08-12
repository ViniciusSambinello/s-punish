## Why

O servidor não possui nenhum sistema próprio de punições: bans e mutes hoje dependem dos comandos vanilla do Paper, que gravam em arquivos locais (`banned-players.json`), não sincronizam entre servidores da rede, não guardam quem puniu nem por qual motivo padronizado, e não oferecem nenhuma forma de auditar a atuação da staff. Sem histórico consultável e sem relatórios por período, a coordenação não consegue verificar consistência de aplicação, detectar abuso de cargo, nem responder a apelações com base em registro confiável.

Este change entrega o **s-punish**: um sistema de punições em rede, com persistência em MySQL, catálogo de motivos configurável, aplicação via GUI, e relatórios de auditoria com controle de acesso por permissão.

## What Changes

**Projeto e build**
- Cria o projeto do zero: Gradle Kotlin DSL (`build.gradle.kts`), toolchain **Java 25**, build multi-módulo.
- Módulos: `spunish-common` (domínio, serviços, persistência, configuração — sem dependência de plataforma), `spunish-paper` (plugin Paper 26.2), `spunish-velocity` (plugin de proxy).
- Shadow/relocation das dependências runtime (HikariCP, driver MySQL, biblioteca de config) para evitar conflito de classpath.

**Punições**
- Duas categorias: `BAN` e `MUTE`.
- Catálogo de motivos por categoria definido em configuração; cada motivo tem um id, um nome de exibição (mostrado ao jogador e persistido), e uma duração. Duração `n` significa **permanente** (nunca expira).
- Punição aplicada a jogadores online **e offline** (resolução de UUID por cache local de perfis).
- Expiração automática por tempo, avaliada na leitura (sem job de varredura obrigatório para correção).

**Comandos** (tab complete em `<category>`, `<reason>`, `<player>` e `<staffer>`)
- `/punish <player>` — GUI de categorias → GUI de motivos → aplica.
- `/punish <player> <category>` — pula direto para a GUI de motivos da categoria.
- `/punish <player> <category> <reason> <time>` — aplica direto, sem GUI, com tempo explícito.
- `/record <category>` — GUI de relatório geral da categoria: diário, semanal, mensal e desde o início.
- `/record <category> <staffer>` — mesmo relatório, restrito às punições aplicadas por aquele membro da staff.
- `/history <player>` — GUI paginada com o histórico de punições recebidas pelo jogador.
- `/unban <player>` e `/unmute <player>` — revogação manual, registrando autor e momento da revogação.

**Aplicação (enforcement)**
- `BAN`: kick imediato se online + bloqueio de login no Paper e, prioritariamente, na borda (Velocity), com mensagem de tela contendo motivo, duração restante e autor.
- `MUTE`: bloqueio de chat e de comandos de chat configuráveis, com aviso ao jogador a cada tentativa.

**Rede**
- MySQL como fonte única de verdade, compartilhado por todos os servidores da rede.
- Módulo Velocity bloqueia o login antes de o jogador alcançar qualquer backend.
- Propagação de mudanças entre instâncias por invalidação de cache baseada em tabela de eventos + polling curto (sem exigir Redis).

**Controle de acesso**
- Permission nodes puros (`spunish.*`), compatíveis com qualquer gerenciador de permissões. Sem dependência de LuckPerms.
- Nodes separados para punir por categoria, ver relatório próprio, ver relatório de terceiros, ver histórico e revogar.

**Mensagens e documentação**
- Todas as mensagens voltadas ao jogador e à staff são configuráveis, incluindo títulos e itens das GUIs.
- Documentação de repositório para GitHub: `README.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `docs/` (configuração, permissões, schema do banco, deploy), templates de issue/PR e workflow de CI.
- Versionamento: SemVer, Conventional Commits, branches `main`/`develop`/`feat|fix/*`, tags de release assinadas por CI.

## Capabilities

### New Capabilities

- `punishment/catalog`: Definição configurável de categorias e motivos — id, nome de exibição, duração (`n` = permanente), ícone de GUI e permissão opcional por motivo; carregamento, validação e recarga.
- `punishment/issuance`: Fluxo de aplicação de punição — comando `/punish` em suas três formas, GUIs de categoria e de motivo, tab complete, parsing de duração, resolução de alvo online/offline e regras de precedência (hierarquia, autopunição, punição duplicada).
- `punishment/enforcement`: Efeito real das punições ativas — bloqueio de login para `BAN`, bloqueio de chat para `MUTE`, kick imediato, cálculo de expiração e telas/mensagens de rejeição.
- `punishment/revocation`: Revogação manual via `/unban` e `/unmute`, com registro de autor e momento, e reflexo imediato no enforcement e no histórico.
- `punishment/history`: Consulta do histórico de punições recebidas por um jogador via `/history`, com GUI paginada, estado (ativa/expirada/revogada) e detalhes por punição.
- `punishment/reporting`: Relatórios de auditoria via `/record`, com janelas diária, semanal, mensal e total, escopo geral ou por membro da staff, e gating por permissão.
- `punishment/storage`: Persistência em MySQL — schema, migrações versionadas, pool de conexões, execução assíncrona fora da main thread e contrato de consultas agregadas.
- `punishment/messaging`: Externalização e formatação de todas as mensagens e textos de GUI, com placeholders, MiniMessage e recarga em runtime.
- `punishment/network-sync`: Coerência entre instâncias da rede — módulo Velocity para bloqueio na borda e propagação de invalidação de cache entre backends.

### Modified Capabilities

Nenhuma. O repositório não possui specs existentes; todas as capabilities acima são novas.

## Impact

**Código** — repositório novo, sem código legado a migrar. Estrutura criada:
- `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml` (version catalog)
- `spunish-common/`, `spunish-paper/`, `spunish-velocity/`
- `docs/`, `.github/workflows/`, `.github/ISSUE_TEMPLATE/`

**Dependências** — Paper API 1.26.2, Velocity API 3.x, HikariCP, driver MySQL (mysql-connector-j), Adventure/MiniMessage (fornecido pela plataforma no Paper), biblioteca de configuração YAML, JUnit 5 + Testcontainers (MySQL) para testes de integração.

**Infraestrutura** — exige uma instância MySQL 8.0+ acessível por todos os servidores da rede e pelo proxy. Requer JDK 25 nos runtimes do Paper e do Velocity.

**Operacional** — os comandos vanilla `/ban`, `/pardon`, `/ban-ip` continuam existindo e passam a ser uma fonte de verdade concorrente; a documentação recomendará desabilitá-los por permissão. Bans pré-existentes em `banned-players.json` **não** são importados automaticamente neste change.

**Fora de escopo** — punições de IP, warns/kicks como categorias, sistema de apelação, painel web, importação de bans legados, integração com Discord.
