## 1. Fundação do repositório e build

- [x] 1.1 Confirmar a coordenada do artefato da API Paper 26.2, a versão pareada da Velocity API e o suporte a Java 25 nos dois runtimes; registrar o resultado em `docs/deployment.md` e rebaixar o toolchain apenas se necessário
- [x] 1.2 Criar `settings.gradle.kts` declarando os módulos `spunish-common`, `spunish-paper` e `spunish-velocity`
- [x] 1.3 Criar `gradle/libs.versions.toml` com Paper API, Velocity API, HikariCP, mysql-connector-j, Configurate YAML, Adventure/MiniMessage, JUnit 5, AssertJ, Testcontainers e o plugin Shadow
- [x] 1.4 Criar o `build.gradle.kts` raiz com as convenções compartilhadas: toolchain Java 25, repositórios, `UTF-8`, `-Xlint`, e `useJUnitPlatform()`
- [x] 1.5 Adicionar o Gradle Wrapper e `gradle.properties` com a versão SemVer inicial `0.1.0-SNAPSHOT`
- [x] 1.6 Criar `.gitignore`, `.gitattributes` e `.editorconfig`
- [x] 1.7 Criar o esqueleto dos três módulos com seus `build.gradle.kts` e confirmar que `./gradlew build` passa em um projeto vazio
- [x] 1.8 Inicializar o fluxo de git: branch `develop` a partir de `main`, e commit inicial no padrão Conventional Commits

## 2. Domínio em `spunish-common`

- [x] 2.1 Criar o record `PunishmentCategory` como enum `BAN`/`MUTE` com resolução case-insensitive a partir de texto
- [x] 2.2 Criar a sealed interface `Actor` com `PlayerActor`, `ConsoleActor` e `SystemActor`
- [x] 2.3 Criar os records `PunishmentTarget` (uuid, nome) e `PlayerProfile` (uuid, nome, último acesso)
- [x] 2.4 Criar o record `Punishment` com todos os campos persistidos, incluindo dados de revogação
- [x] 2.5 Criar a sealed interface `PunishmentState` com `Active`, `Expired` e `Revoked`, e a resolução de estado a partir de um `Punishment` e de um instante de referência
- [x] 2.6 Criar a porta `SystemClock` e sua implementação padrão, injetável para testes determinísticos
- [x] 2.7 Implementar `DurationParser`: aceita `n` (permanente) e sequências quantidade+unidade `s|m|h|d|w|mo|y`, rejeitando malformado, zero e negativo
- [x] 2.8 Escrever testes unitários de `DurationParser` cobrindo `n`, unidade simples, composto (`1h30m`, `1y6mo`), unidade desconhecida, zero e vazio
- [x] 2.9 Implementar `DurationFormatter` com rótulos de unidade, número máximo de unidades e texto de permanente, todos vindos de configuração
- [x] 2.10 Escrever testes unitários de `DurationFormatter` cobrindo truncamento por número máximo de unidades e o caso permanente

## 3. Configuração, catálogo e mensagens

- [x] 3.1 Definir os records `@ConfigSerializable` de `config.yml`: banco, pool, identidade do servidor, sync, modo de falha, retenção, comandos bloqueados no mute e limites de duração por permissão
- [x] 3.2 Definir os records de `reasons.yml`: motivo com id, nome de exibição, duração, ícone, descrição e permissão opcional, agrupados por categoria
- [x] 3.3 Implementar o carregamento Configurate dos arquivos, criando-os a partir dos recursos padrão quando ausentes
- [x] 3.4 Implementar `ReasonCatalogValidator` cobrindo id duplicado na categoria, id vazio, nome de exibição vazio, duração ausente, unidade inválida, duração não positiva e categoria desconhecida
- [x] 3.5 Escrever testes unitários do validador, incluindo o caso de ids iguais em categorias diferentes ser válido
- [x] 3.6 Implementar `ReasonCatalog` com busca por categoria e por id, e filtragem por permissão do autor
- [x] 3.7 Implementar a troca atômica de configuração: carrega e valida em objetos novos, e só então substitui a referência em vigor
- [x] 3.8 Escrever teste unitário provando que uma recarga inválida preserva o catálogo anterior intacto
- [x] 3.9 Definir o `messages.yml` padrão com todas as chaves: confirmações, erros, permissões, telas de rejeição, avisos de mute, anúncios, rótulos de estado e de janela de relatório
- [x] 3.10 Implementar `MessageService` com resolução de chave, fallback para o padrão embutido, aviso de chave ausente registrado uma única vez, e suporte a mensagem em lista e a mensagem vazia como desligada
- [x] 3.11 Implementar a substituição de placeholders com prefixo global, deixando placeholder desconhecido literal e registrando o aviso uma única vez por chave
- [x] 3.12 Implementar a renderização MiniMessage com degradação para texto sem formatação quando a tag for malformada, registrando a chave defeituosa
- [x] 3.13 Implementar a formatação de data e hora com padrão e fuso configuráveis, convertendo a partir de UTC
- [x] 3.14 Escrever testes unitários de `MessageService`: substituição, placeholder desconhecido, chave ausente, tag malformada, lista de linhas e mensagem desligada
- [x] 3.15 Definir o `gui.yml` padrão com títulos, tamanhos, slots, ícones e textos de item das GUIs de categoria, motivo, histórico e relatório

## 4. Persistência MySQL

- [x] 4.1 Implementar `DatabaseConnectionProvider` sobre HikariCP, com prefixo de tabela, SSL e parâmetros de pool vindos da configuração, e `driverClassName` apontando para o FQCN relocado
- [x] 4.2 Garantir que credenciais nunca apareçam em log, inclusive em mensagens de erro e stack traces, e cobrir isso com teste
- [x] 4.3 Implementar o executor de I/O sobre virtual threads e a fronteira assíncrona baseada em `CompletableFuture`, com timeout de consulta configurável
- [x] 4.4 Escrever a migração `V1` criando `profiles`, `punishments`, `sync_events` e `schema_version` com os índices do design
- [x] 4.5 Implementar `SchemaMigrator` com `GET_LOCK` nomeado, aplicação idempotente e recusa de inicialização quando a versão do schema for maior que a suportada
- [x] 4.6 Implementar `ProfileRepository`: upsert no login e resolução por nome escolhendo o perfil de acesso mais recente
- [x] 4.7 Implementar `PunishmentRepository.insert`, gravando punição e perfil do alvo na mesma transação e gerando o `public_id`
- [x] 4.8 Implementar `PunishmentRepository.findActive` por uuid e categoria, com o predicado de expiração usando `UTC_TIMESTAMP(3)`
- [x] 4.9 Implementar `PunishmentRepository.revoke` registrando tipo de revogador, uuid, nome, instante e motivo, sem alterar campos originais
- [x] 4.10 Implementar `PunishmentRepository.findHistory` paginado por uuid, com filtro opcional de categoria e ordenação por instante de aplicação decrescente
- [x] 4.11 Implementar as consultas agregadas de relatório: total, divisão por estado, ranking por autor e distribuição por motivo, parametrizadas por categoria, faixa de instante e autor opcional
- [x] 4.12 Implementar a rotina de retenção, desabilitada por padrão, removendo apenas punições encerradas mais antigas que o período configurado
- [x] 4.13 Implementar a detecção de perda de conexão, a reconexão com espera crescente e o indicador de saúde consultável

## 5. Serviços de punição em `spunish-common`

- [x] 5.1 Implementar `PunishmentIssueService` orquestrando validação, aplicação e persistência, retornando um resultado tipado de sucesso ou de recusa
- [x] 5.2 Implementar as regras de recusa: autopunição sem `spunish.punish.self`, alvo com `spunish.exempt.<category>`, e punição ativa já existente na mesma categoria
- [x] 5.3 Implementar a sobreposição autorizada por `spunish.punish.override`, revogando a punição anterior com autoria de sistema dentro da mesma transação
- [x] 5.4 Implementar a checagem de limite de duração por permissão, recusando inclusive o caso permanente acima do teto
- [x] 5.5 Implementar a checagem de permissão de categoria e de motivo restrito
- [x] 5.6 Garantir que falha de persistência aborte a punição sem kickar nem mutar, retornando recusa por erro interno
- [x] 5.7 Escrever testes unitários das regras de precedência com repositório em memória: autopunição, isenção, duplicada, sobreposição, limite de duração e categorias diferentes coexistindo
- [x] 5.8 Implementar `PunishmentRevokeService` com validação de punição ativa, permissão por categoria e motivo opcional
- [x] 5.9 Escrever testes unitários de revogação: sem punição ativa, sem permissão, revogação por console e preservação do registro original
- [x] 5.10 Implementar `PunishmentHistoryService` retornando páginas já resolvidas em `PunishmentState`
- [x] 5.11 Implementar `ReportService` com o cálculo das janelas diária, semanal, mensal e desde o início no fuso configurado, convertendo os limites para instantes UTC
- [x] 5.12 Escrever testes unitários das janelas de relatório, incluindo o corte de início do dia no fuso configurado e não em UTC
- [x] 5.13 Implementar o cache de relatório com duração configurável e o cooldown por usuário
- [x] 5.14 Implementar o cálculo da taxa de revogação e garantir que punições revogadas continuem contadas no total da janela

## 6. Sincronização de rede

- [ ] 6.1 Implementar `SyncEventRepository` com escrita de evento na mesma transação da punição e da revogação
- [ ] 6.2 Implementar o consumidor com polling por `created_at` e janela de sobreposição, deduplicando por id contra um conjunto limitado de ids recentes
- [ ] 6.3 Implementar o descarte de eventos consumidos por retenção curta configurável
- [ ] 6.4 Implementar a identidade de servidor configurável, com derivação estável e aviso em log quando ausente, e o descarte do evento originado pela própria instância
- [ ] 6.5 Implementar `PunishmentStateCache` por jogador conectado, carregado no login, atualizado por punição local e por evento de sync, e descartado no quit
- [ ] 6.6 Garantir que falha no consumo registre no log, preserve o enforcement local, não desabilite o plugin e retome do último evento processado
- [ ] 6.7 Escrever teste de integração do consumidor com commits concorrentes fora de ordem de `AUTO_INCREMENT`, provando que nenhum evento é perdido

## 7. Módulo Paper — bootstrap e enforcement

- [ ] 7.1 Criar o `paper-plugin.yml` e a classe principal, com a inicialização das configurações, do banco, das migrações e dos serviços, e recusa de habilitação quando o banco estiver inacessível
- [ ] 7.2 Implementar as portas de plataforma do Paper: `PermissionChecker`, `AudienceResolver`, `PlayerKicker`, `MainThreadDispatcher` e `ServerIdentity`
- [ ] 7.3 Implementar o desligamento limpo: encerrar o consumidor de sync, o executor de I/O e o pool de conexões
- [ ] 7.4 Implementar o listener de pré-login: atualizar o perfil, verificar ban ativo, recusar com a tela configurável e carregar o estado de mute no cache
- [ ] 7.5 Implementar o modo de falha configurável no pré-login, com padrão `DENY` e registro em log
- [ ] 7.6 Implementar o listener de chat lendo apenas do cache, sem consulta ao armazenamento durante o evento
- [ ] 7.7 Implementar o bloqueio dos comandos de comunicação configurados, resolvendo aliases registrados
- [ ] 7.8 Implementar o modo de falha configurável no chat, com padrão `ALLOW`
- [ ] 7.9 Implementar o cooldown configurável do aviso de mute
- [ ] 7.10 Implementar o kick imediato do jogador online ao receber ban, tanto local quanto por evento de sync
- [ ] 7.11 Implementar o listener de quit descartando o estado em cache
- [ ] 7.12 Implementar a notificação de staff para `spunish.notify` e o anúncio público desligável, em punição e em revogação

## 8. Módulo Paper — comandos e tab complete

- [ ] 8.1 Implementar `/punish` com as três aridades válidas e recusa das demais com a mensagem de uso
- [ ] 8.2 Implementar a restrição do console às formas que não abrem GUI, com mensagem orientando a forma completa
- [ ] 8.3 Implementar o tab complete de `/punish`: jogadores, categorias filtradas por permissão, motivos da categoria digitada filtrados por permissão, e sugestões de tempo
- [ ] 8.4 Implementar a filtragem por prefixo case-insensitive e o retorno vazio quando a categoria da posição anterior for inválida
- [ ] 8.5 Implementar `/unban` e `/unmute` com motivo livre opcional e permissão `spunish.unpunish.<category>`
- [ ] 8.6 Implementar `/record <category>` e `/record <category> <staffer>` com o gating de `spunish.record` e `spunish.record.others`
- [ ] 8.7 Implementar o tab complete de `/record`: categorias na posição 1, e na posição 2 os jogadores online com permissão de punir aquela categoria
- [ ] 8.8 Implementar `/history <player>` com permissão `spunish.history`, tab complete de jogadores e saída em texto quando executado pelo console
- [ ] 8.9 Implementar o comando administrativo de recarga com `spunish.admin.reload`, devolvendo ao autor a lista de erros quando a configuração for inválida
- [ ] 8.10 Garantir que nenhum comando execute resolução de jogador ou acesso ao armazenamento na thread principal

## 9. Módulo Paper — GUIs

- [ ] 9.1 Implementar a abstração mínima de menu: holder próprio, roteamento de clique, cancelamento do arraste e do clique em item, e paginação
- [ ] 9.2 Implementar a construção de itens a partir de `gui.yml`, com título, ícone, nome e descrição resolvidos pelo `MessageService`
- [ ] 9.3 Implementar a GUI de categorias, exibindo apenas as categorias que o autor pode aplicar e preservando o alvo ao encaminhar
- [ ] 9.4 Implementar a GUI de motivos, exibindo nome e duração, ocultando motivos sem permissão, e aplicando a punição com a duração padrão ao clicar
- [ ] 9.5 Implementar a paginação da GUI de motivos e o controle de retorno, presente somente quando aberta a partir da GUI de categorias
- [ ] 9.6 Garantir que fechar qualquer GUI sem selecionar não crie nenhum registro
- [ ] 9.7 Implementar a GUI de histórico com carga por página sob demanda, ordenação decrescente e rótulo de estado distinguível por ícone ou cor
- [ ] 9.8 Implementar no histórico a exibição de tempo restante, texto de permanente, e dados de revogação quando houver
- [ ] 9.9 Implementar o filtro de categoria do histórico, reiniciando a paginação na primeira página
- [ ] 9.10 Implementar o detalhe de uma punição com identificador e servidor de origem
- [ ] 9.11 Implementar a GUI de relatório com as quatro janelas selecionáveis, destaque da janela ativa e recarga assíncrona sem fechar a GUI
- [ ] 9.12 Implementar no relatório geral o total, a divisão por estado, o ranking por autor e a distribuição por motivo
- [ ] 9.13 Implementar o ranking paginado ou truncado de forma configurável, indicando a existência de mais entradas
- [ ] 9.14 Implementar a GUI de relatório individual do staffer, com a lista das punições mais recentes aplicadas por ele
- [ ] 9.15 Implementar o texto configurável de ausência de dados nas GUIs de histórico e de relatório

## 10. Módulo Velocity

- [ ] 10.1 Criar a classe de plugin Velocity com anotação de plugin, carregando as mesmas configurações e serviços de `spunish-common`
- [ ] 10.2 Implementar as portas de plataforma do Velocity: permissão, audiência, desconexão, dispatcher e identidade
- [ ] 10.3 Implementar a verificação de ban no evento de login do proxy, recusando antes de encaminhar a qualquer backend, com o mesmo texto configurável dos backends
- [ ] 10.4 Implementar o consumo de eventos de sync no proxy, desconectando da rede o jogador banido em outro servidor
- [ ] 10.5 Configurar o pool de conexões do proxy com dimensionamento reduzido e documentar a recomendação
- [ ] 10.6 Verificar manualmente que um backend recusa o login de jogador banido com o módulo de proxy ausente e em conexão direta à porta do backend

## 11. Empacotamento e testes de integração

- [ ] 11.1 Configurar o Shadow nos dois módulos de plataforma, relocando HikariCP, driver MySQL e Configurate sob `com.spunish.libs`, com `mergeServiceFiles()` e Adventure mantido como `compileOnly`
- [ ] 11.2 Escrever o teste de fumaça que carrega o jar shadeado e abre conexão real, provando que a relocation do driver sobreviveu
- [ ] 11.3 Escrever os testes Testcontainers de migração: banco vazio, banco já atualizado, três inicializações simultâneas e schema mais novo que o binário
- [ ] 11.4 Escrever os testes Testcontainers de `PunishmentRepository`: inserção, punição ativa, expiração pelo relógio do banco, revogação e histórico paginado
- [ ] 11.5 Escrever os testes Testcontainers das agregações de relatório, incluindo revogadas contadas no total e a taxa de revogação
- [ ] 11.6 Escrever o teste que confirma, por plano de execução, o uso de índice na verificação de login e na agregação por autor
- [ ] 11.7 Escrever os testes Testcontainers de resolução de perfil: nome trocado e nome reutilizado por outra conta
- [ ] 11.8 Escrever o teste da rotina de retenção, provando que punições ativas nunca são removidas
- [ ] 11.9 Executar a matriz de teste manual da camada de plataforma em um Paper e um Velocity reais e registrar o resultado

## 12. Documentação para o GitHub

- [ ] 12.1 Escrever o `README.md`: descrição, requisitos, instalação, tabela de comandos, tabela de permissões, exemplo de configuração e badges de CI
- [ ] 12.2 Escrever `docs/configuration.md` cobrindo os quatro arquivos, cada chave e o comportamento da recarga atômica
- [ ] 12.3 Escrever `docs/permissions.md` com a tabela completa de nodes e exemplos de grupos de staff
- [ ] 12.4 Escrever `docs/database.md` com o schema, os índices, o significado de `expires_at` nulo e a política de retenção
- [ ] 12.5 Escrever `docs/deployment.md` com a ordem de implantação, o dimensionamento de pool no proxy, os modos de falha e a instrução de negar os comandos vanilla de punição
- [ ] 12.6 Escrever `docs/testing.md` com a matriz de teste manual da camada de plataforma
- [ ] 12.7 Escrever `docs/troubleshooting.md` cobrindo falha de conexão, schema à frente do binário, latência de propagação e mensagem sem formatação por tag malformada
- [ ] 12.8 Escrever `CONTRIBUTING.md` com o modelo de branches, Conventional Commits, escopos aceitos e como rodar os testes
- [ ] 12.9 Adicionar `LICENSE`, `CHANGELOG.md` no formato Keep a Changelog, e os templates de issue e de pull request em `.github/`

## 13. CI e release

- [ ] 13.1 Criar o workflow de CI executando `./gradlew build` em JDK 25, com os testes Testcontainers, em push e em pull request
- [ ] 13.2 Adicionar a validação de Conventional Commits no CI, com os escopos `common`, `paper`, `velocity`, `docs` e `build`
- [ ] 13.3 Adicionar o cache de dependências do Gradle e a publicação dos relatórios de teste como artefato do workflow
- [ ] 13.4 Criar o workflow de release disparado por tag `v*`, publicando os jars do Paper e do Velocity na GitHub Release
- [ ] 13.5 Proteger `main` exigindo CI verde e pull request, e documentar a regra em `CONTRIBUTING.md`
- [ ] 13.6 Fechar a versão `0.1.0`: atualizar `CHANGELOG.md`, fixar a versão em `gradle.properties`, abrir o PR de `develop` para `main` e criar a tag de release
