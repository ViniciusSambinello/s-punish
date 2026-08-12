## Context

Repositório greenfield: só existe `README.md` vazio e um commit inicial. Não há código, build, CI ou specs para preservar. Motivação em `proposal.md — Why`; requisitos em `specs/punishment/*/spec.md`.

Restrições que moldam o desenho:

- **Java 25** e **Gradle Kotlin DSL** são requisitos do usuário, não escolhas em aberto.
- **Paper 26.2** e **Velocity** são runtimes distintos com classloaders e APIs diferentes, mas precisam compartilhar domínio, persistência, configuração e mensagens.
- **MySQL é a única fonte de verdade** e o único canal de comunicação entre instâncias — a decisão de topologia excluiu Redis e canais de plugin messaging como dependência obrigatória.
- A thread principal do Paper não pode bloquear em I/O; toda leitura de punição no caminho de login e de chat precisa ser resolvida sem consulta síncrona.

**Premissa a confirmar no início da implementação:** "Paper 26.2" é interpretado como a API Paper para Minecraft 1.26.2. A coordenada exata do artefato, a versão da Velocity API compatível e o suporte oficial a Java 25 nesses runtimes precisam ser verificados na primeira task antes de fixar o version catalog. Se o runtime alvo não suportar Java 25, o toolchain é o único ponto do desenho que muda.

## Goals / Non-Goals

**Goals:**

- Um único núcleo de domínio sem dependência de plataforma, reutilizado por Paper e Velocity sem duplicação de regra de negócio.
- Enforcement correto mesmo com o proxy ausente, com o banco lento, ou com o relógio de um servidor dessincronizado.
- Caminho de chat e de login sem I/O síncrono.
- Configuração como contrato: motivos, mensagens e layout de GUI editáveis sem recompilar, com falha de validação que não derruba o estado em vigor.
- Build reprodutível offline: nenhum download de dependência no start do servidor.
- Código legível por nomeação, sem comentários explicativos — comentário apenas onde houver regra de negócio não óbvia.

**Non-Goals:**

- API pública para outros plugins consumirem (pode ser extraída depois; não é requisito agora).
- Abstração de múltiplos bancos (Postgres, SQLite). O contrato de storage é uma interface, mas só haverá uma implementação.
- Suporte a BungeeCord. O módulo de proxy é Velocity.
- Painel web, apelações, importação de bans legados, punições de IP — já excluídos no `proposal.md — Impact`.

## Decisions

### 1. Build multi-módulo com núcleo agnóstico de plataforma

```
s-punish/
├── settings.gradle.kts
├── build.gradle.kts                 # convenções: toolchain 25, repos, test
├── gradle/libs.versions.toml        # version catalog
├── spunish-common/                  # domínio, serviços, storage, config, mensagens
├── spunish-paper/                   # comandos, GUIs, listeners, bootstrap Paper
└── spunish-velocity/                # bloqueio de login na borda, bootstrap Velocity
```

`spunish-common` não referencia nenhuma classe de Bukkit/Paper/Velocity. Ele expõe portas que cada plataforma implementa:

| Porta (common) | Paper | Velocity |
| --- | --- | --- |
| `PermissionChecker` | `Permissible` | `CommandSource` |
| `AudienceResolver` | `Bukkit.getPlayer` | `ProxyServer.getPlayer` |
| `PlayerKicker` | `Player#kick` | `Player#disconnect` |
| `MainThreadDispatcher` | scheduler do Paper | executor imediato |
| `ServerIdentity` | config do backend | config do proxy |

**Por quê:** as regras de precedência, parsing de duração, validação de catálogo e agregação de relatório são idênticas nos dois runtimes. Duplicá-las garantiria divergência. **Alternativa rejeitada:** módulo único Paper com o proxy consultando o banco por conta própria — reintroduz a lógica de "punição ativa" em dois lugares, que é exatamente o ponto onde um bug deixa alguém banido entrar.

### 2. Shadow com relocation nos dois módulos de plataforma

Ambos os jars finais embutem HikariCP, driver MySQL e Configurate, relocados sob `com.spunish.libs.*`. Adventure/MiniMessage ficam `compileOnly` — Paper e Velocity já os fornecem em runtime.

Cuidados obrigatórios: `mergeServiceFiles()` no Shadow, e `driverClassName` da Hikari apontando explicitamente para o FQCN **relocado** do driver, já que a descoberta por `ServiceLoader` não sobrevive de forma confiável à relocação.

**Por quê:** Velocity compartilha classloader entre plugins, então relocation é obrigatório lá de qualquer forma. Usar o mesmo mecanismo nos dois módulos dá um caminho de build só, e o jar funciona em servidor sem saída para a internet.

**Alternativa rejeitada:** `PluginLoader` + `MavenLibraryResolver` do Paper, que baixaria as libs no start. Mais enxuto no jar do Paper, mas cria dois mecanismos de entrega de dependência no mesmo projeto e quebra em host sem acesso ao Maven Central no boot.

**Risco assumido:** relocar `mysql-connector-j` pode quebrar caminhos que resolvem classes por string. Coberto por uma task explícita de teste de fumaça que sobe o **jar shadeado** contra Testcontainers, não apenas as classes de teste.

### 3. Java 25 usado onde muda o desenho, não como vitrine

- **Virtual threads** para o executor de I/O de banco. Desde que o pinning por `synchronized` deixou de existir no JDK 24, JDBC bloqueante sobre virtual threads é seguro, e o paralelismo passa a ser limitado pelo pool da Hikari em vez de por um pool de plataforma dimensionado à mão.
- **Records** para todo o domínio (`Punishment`, `Reason`, `PunishmentTarget`, `ReportWindow`, `ReportSummary`).
- **Sealed interface + pattern matching** para `Actor` (`PlayerActor`, `ConsoleActor`, `SystemActor`) e para `PunishmentState` (`Active`, `Expired`, `Revoked`). O compilador passa a exigir tratamento exaustivo dos três estados em todo ponto de exibição — que é exatamente onde o histórico erraria em silêncio.
- **Text blocks** para o SQL, mantendo as queries legíveis sem concatenação.

### 4. JDBC direto com HikariCP, sem ORM

DAOs escritos à mão com `PreparedStatement` e text blocks. **Por quê:** as consultas que importam são três agregações e duas leituras indexadas; um ORM adicionaria reflexão (hostil a relocation), peso ao jar e uma camada entre o autor e o plano de execução, justamente onde o desempenho é sensível. **Alternativa rejeitada:** jOOQ/Hibernate — desproporcional para 4 tabelas.

### 5. Modelo de dados

Quatro tabelas, com prefixo configurável. UUIDs como `BINARY(16)` (metade do espaço de índice de um `CHAR(36)`), instantes como `DATETIME(3)` em UTC.

```sql
CREATE TABLE `{p}profiles` (
  `uuid`         BINARY(16)  NOT NULL,
  `name`         VARCHAR(16) NOT NULL,
  `last_seen_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`uuid`),
  KEY `idx_profiles_name` (`name`, `last_seen_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `{p}punishments` (
  `id`             BIGINT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `public_id`      CHAR(8)            NOT NULL,
  `category`       ENUM('BAN','MUTE') NOT NULL,
  `target_uuid`    BINARY(16)         NOT NULL,
  `target_name`    VARCHAR(16)        NOT NULL,
  `actor_type`     ENUM('PLAYER','CONSOLE')            NOT NULL,
  `actor_uuid`     BINARY(16)         NULL,
  `actor_name`     VARCHAR(32)        NOT NULL,
  `reason_id`      VARCHAR(64)        NOT NULL,
  `reason_display` VARCHAR(128)       NOT NULL,
  `created_at`     DATETIME(3)        NOT NULL,
  `expires_at`     DATETIME(3)        NULL,
  `origin_server`  VARCHAR(64)        NOT NULL,
  `revoker_type`   ENUM('PLAYER','CONSOLE','SYSTEM')   NULL,
  `revoker_uuid`   BINARY(16)         NULL,
  `revoker_name`   VARCHAR(32)        NULL,
  `revoked_at`     DATETIME(3)        NULL,
  `revoke_reason`  VARCHAR(255)       NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_punishments_public_id` (`public_id`),
  KEY `idx_punishments_active` (`target_uuid`, `category`, `revoked_at`, `expires_at`),
  KEY `idx_punishments_history` (`target_uuid`, `created_at`),
  KEY `idx_punishments_report` (`category`, `created_at`, `actor_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `{p}sync_events` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `type`          ENUM('PUNISHMENT_CREATED','PUNISHMENT_REVOKED') NOT NULL,
  `punishment_id` BIGINT UNSIGNED NOT NULL,
  `target_uuid`   BINARY(16)      NOT NULL,
  `origin_server` VARCHAR(64)     NOT NULL,
  `created_at`    DATETIME(3)     NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sync_events_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `{p}schema_version` (
  `version`    INT         NOT NULL,
  `applied_at` DATETIME(3) NOT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

`expires_at NULL` = permanente. `revoked_at NULL` = não revogada. `actor_type`/`revoker_type` existem porque o spec exige distinguir revogação por pessoa, por console e automática pelo sistema — o que `NULL` sozinho não expressa.

`reason_display` é desnormalizado de propósito: o catálogo pode renomear ou remover um motivo, e o histórico precisa continuar mostrando o texto vigente na aplicação (`catalog` — *Punições existentes não são afetadas*).

`public_id` de 8 caracteres é o identificador mostrado na tela de rejeição, para a staff localizar o registro sem expor a chave sequencial.

### 6. Relógio autoritativo é o do banco

Toda comparação de expiração usa `UTC_TIMESTAMP(3)` no próprio predicado SQL, não `Instant.now()` do servidor de jogo.

**Por quê:** backends de uma rede raramente têm relógios sincronizados. Se cada um decidisse a expiração pelo relógio local, o mesmo jogador entraria em um servidor e não em outro. Com o predicado no banco, existe uma única resposta. O relógio local só é usado para formatação de exibição e para o cálculo dos limites de janela de relatório, que são então enviados como parâmetros.

### 7. Migrações versionadas com lock de aplicação

Cada migração é um script numerado aplicado dentro de um `GET_LOCK` nomeado, com a versão registrada em `schema_version`. Isso satisfaz o cenário de três servidores iniciando ao mesmo tempo sem erro de objeto duplicado. Se `schema_version` contiver versão maior que a suportada pelo binário, o plugin registra o erro e não habilita.

**Alternativa rejeitada:** Flyway/Liquibase — peso e reflexão desproporcionais para um schema de 4 tabelas que sabemos que só rodará em MySQL.

### 8. Sincronização por polling de tabela de eventos, com janela de sobreposição

Cada instância consome `sync_events` a cada intervalo configurável (padrão 2s, teto de propagação de 5s exigido pelo spec). O consumidor **não** rastreia apenas o último `id`: ele consulta por `created_at > lastPollInstant - overlap` e deduplica por `id` contra um conjunto limitado de ids recentes.

**Por quê a sobreposição:** `AUTO_INCREMENT` é atribuído no insert, mas a visibilidade acontece no commit. Uma transação que pegou o id 100 pode commitar depois de outra que pegou o 101 — um consumidor que filtra por `id > 101` perde o 100 para sempre, e alguém fica sem ser mutado. Filtrar por tempo com sobreposição e deduplicar por id fecha essa janela.

Eventos mais velhos que uma retenção curta e configurável são apagados. Instância que ficou fora mais tempo que isso recarrega o estado no start, então não depende da tabela de eventos para convergir.

**Alternativa rejeitada:** Redis pub/sub — mais barato e imediato, mas o usuário optou por uma topologia sem infraestrutura adicional além do MySQL.

### 9. Cache de estado só para jogadores conectados

Cada instância mantém em memória o estado de mute dos jogadores conectados **a ela**, carregado no `AsyncPlayerPreLoginEvent` e descartado no quit. O evento de chat lê só desse mapa.

Ban **não** é cacheado: a verificação acontece no pré-login, que já é assíncrono, e ler direto do banco ali elimina qualquer risco de cache velho deixar alguém entrar.

### 10. GUIs próprias, sem biblioteca externa

Uma abstração mínima de menu (`InventoryHolder` próprio + roteamento de clique + paginação) dentro de `spunish-paper`. **Por quê:** as três GUIs são menus estáticos paginados; uma lib traria mais uma dependência para relocar e um modelo de layout que brigaria com o requisito de itens totalmente configuráveis por YAML. **Alternativa rejeitada:** triumph-gui / InvUI.

Toda GUI é *snapshot*: os dados são carregados antes de abrir e não se atualizam sozinhos. Trocar janela de relatório, página ou filtro dispara nova carga assíncrona e reabre o conteúdo.

### 11. Quatro arquivos de configuração com papéis separados

| Arquivo | Conteúdo |
| --- | --- |
| `config.yml` | banco, pool, id do servidor, intervalos de sync, modo de falha, limites de duração por permissão, comandos bloqueados no mute, retenção |
| `reasons.yml` | catálogo de motivos por categoria |
| `messages.yml` | todo texto de chat, telas de rejeição, anúncios, formatação de duração e data |
| `gui.yml` | títulos, tamanhos, slots, ícones e textos de item das GUIs |

Configurate 4 (YAML) nos dois módulos, mapeando para records via `@ConfigSerializable`.

**Recarga atômica:** a nova configuração é carregada e validada em objetos novos; só se tudo passar é que a referência em vigor é trocada. Uma configuração inválida nunca substitui parcialmente a anterior — exigência dos specs de `catalog` e `messaging`.

### 12. Permission nodes

| Node | Concede |
| --- | --- |
| `spunish.punish` | usar `/punish` |
| `spunish.punish.<ban\|mute>` | aplicar aquela categoria |
| `spunish.punish.self` | punir a si mesmo |
| `spunish.punish.override` | ignorar isenção e sobrepor punição ativa |
| `spunish.reason.<category>.<reason>` | usar motivo marcado como restrito |
| `spunish.exempt.<ban\|mute>` | ser protegido daquela categoria |
| `spunish.unpunish.<ban\|mute>` | revogar aquela categoria |
| `spunish.record` | abrir relatório do próprio usuário |
| `spunish.record.others` | abrir relatório geral e de terceiros |
| `spunish.history` | abrir histórico de qualquer jogador |
| `spunish.notify` | receber anúncios de staff |
| `spunish.admin.reload` | recarregar configuração |

Limites de duração por permissão usam nodes numerados declarados em `config.yml` (ex.: `spunish.limit.trial` → `7d`), não uma convenção de node com o tempo embutido — assim o operador muda o teto sem mexer no gerenciador de permissões.

### 13. Estratégia de teste

| Camada | Como |
| --- | --- |
| Domínio de `common` | JUnit 5 puro: parser de duração, validação de catálogo, regras de precedência, cálculo de janela de relatório, formatação de mensagem, resolução de estado |
| Storage, reporting, sync | Testcontainers com MySQL 8: migrações, migração concorrente, uso de índice, agregações, janela de sobreposição do consumidor de eventos |
| Jar shadeado | Teste de fumaça que carrega o jar do Shadow e abre conexão real, provando que a relocation do driver sobreviveu |
| Camada de plataforma | Matriz de teste manual documentada em `docs/testing.md` |

O `SystemClock` é uma porta injetável para que os testes de expiração e de janela sejam determinísticos.

### 14. Versionamento e documentação do repositório

- **SemVer** em `gradle.properties`, com a tag de release derivada dele.
- **Conventional Commits** validados no CI por commitlint, com escopos `common`, `paper`, `velocity`, `docs`, `build`.
- **Branches:** `main` sempre liberável; `develop` de integração; `feat/*`, `fix/*`, `chore/*` por trabalho; merge em `develop` por PR com squash.
- **CHANGELOG.md** no formato Keep a Changelog, gerado a partir dos commits na abertura da release.
- **CI** (GitHub Actions): em push e PR — `./gradlew build` em JDK 25, testes com Testcontainers, commitlint; em tag `v*` — build dos dois jars e criação da release com os artefatos.
- **Docs:** `README.md` (o que é, instalação, comandos, permissões, screenshots), e `docs/` com `configuration.md`, `permissions.md`, `database.md`, `deployment.md`, `testing.md`, `troubleshooting.md`. Mais `CONTRIBUTING.md`, `LICENSE`, templates de issue e PR.

O histórico do git acompanha a ordem das tasks: um commit por task concluída, com escopo, de modo que o repositório conte a construção do sistema em vez de aparecer em um despejo único.

### 15. Comentários

Sem comentários de "o que o código faz". Comentário só onde existir regra de negócio ou motivo não dedutível do código — por exemplo, por que `reason_display` é desnormalizado, por que o consumidor de eventos usa janela de sobreposição, e por que a expiração usa o relógio do banco. Nomes de método e de variável carregam o resto.

## Risks / Trade-offs

**Paper 26.2 ou Velocity podem não rodar em Java 25** → A primeira task verifica o runtime alvo antes de fixar o version catalog. O toolchain está isolado no build de convenções; rebaixá-lo não afeta nenhuma outra decisão. Virtual threads são o único ponto do desenho que dependeria de rebaixamento, e degradam para um pool fixo sem mudança de contrato.

**Relocation do driver MySQL pode quebrar resolução de classe por string** → `mergeServiceFiles()`, `driverClassName` explícito com o FQCN relocado, e teste de fumaça sobre o jar shadeado, não sobre as classes soltas.

**Polling adiciona latência e carga constante ao banco** → Uma consulta indexada leve por instância a cada 2s; retenção curta mantém a tabela pequena. Intervalo configurável para redes maiores trocarem latência por carga. O caminho de latência zero está preservado: a instância que aplicou a punição atualiza o próprio cache na hora.

**Perda de evento por ordenação de commit do AUTO_INCREMENT** → Janela de sobreposição por `created_at` com dedupe por id, coberta por teste de integração com commits concorrentes fora de ordem.

**Velocity precisa do próprio pool de conexões** → Aumenta a contagem total de conexões contra o MySQL. Documentar o dimensionamento em `docs/deployment.md`; o proxy só faz leitura de ban no pré-login e consumo de eventos, então um pool pequeno basta.

**Modo de falha `DENY` no login transforma queda do banco em queda da rede** → É o padrão porque `ALLOW` deixaria todo banido entrar durante o incidente. A escolha é configurável e está documentada junto com a recomendação de monitorar a saúde da conexão.

**Comandos vanilla continuam como fonte de verdade concorrente** → Fora do escopo remover. `docs/deployment.md` instrui negar `minecraft.command.ban`, `minecraft.command.pardon` e correlatos à staff.

**GUIs próprias significam mais código para manter** → Aceito em troca de configurabilidade total e de uma dependência a menos. O escopo é pequeno: um menu paginado genérico serve às três telas.

**Testes da camada de plataforma são manuais** → Toda regra testável foi empurrada para `common`, que tem cobertura automatizada. A camada de plataforma fica reduzida a adaptação e apresentação, e a matriz manual é versionada em `docs/testing.md`.

## Migration Plan

Não há migração de dados: projeto novo, e a importação de bans legados está fora de escopo.

**Ordem de implantação:**

1. Provisionar o MySQL e o usuário da aplicação; conferir alcance a partir do proxy e de todos os backends.
2. Subir **um** backend com o plugin. As migrações criam o schema na primeira inicialização.
3. Validar aplicação, expiração, histórico e relatório nesse backend isolado.
4. Distribuir aos demais backends com o mesmo `config.yml`, alterando apenas o identificador de servidor.
5. Instalar o módulo Velocity por último — os backends já bloqueiam login sozinhos, então a borda é reforço, não pré-requisito.
6. Negar os comandos vanilla de punição à staff.

**Rollback:** remover os jars e reverter as permissões dos comandos vanilla. O schema permanece intacto; nenhuma punição é perdida e reinstalar retoma do mesmo estado. Não há passo de rollback de schema porque nenhuma migração destrói dado existente.

## Open Questions

- Coordenada exata e repositório do artefato da API Paper 26.2, e a versão da Velocity API pareada com ela. Resolvido na primeira task; não altera specs nem quebra o desenho.
- Extrair uma API pública para outros plugins consultarem punições. Adiável sem custo — a fronteira de serviço em `common` já é o ponto natural de extração quando houver demanda.
