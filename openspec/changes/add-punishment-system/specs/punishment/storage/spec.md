## Purpose

Define o contrato de persistência das punições em MySQL: o que é armazenado, como o schema evolui, e as garantias de disponibilidade e desempenho que o resto do sistema pode assumir.

## ADDED Requirements

### Requirement: MySQL como fonte única de verdade

O sistema SHALL persistir todas as punições, revogações e perfis de jogador em um banco MySQL 8.0 ou superior, compartilhado por todos os servidores da rede e pelo proxy. Nenhum estado de punição SHALL ser mantido apenas em disco local de um servidor.

#### Scenario: Punição visível de outro servidor

- **GIVEN** uma punição aplicada em um servidor
- **WHEN** outro servidor da rede consulta o estado do jogador
- **THEN** a punição é encontrada, porque ambos leem o mesmo banco

#### Scenario: Estado sobrevive a reinício

- **GIVEN** uma punição ativa
- **WHEN** todos os servidores da rede são reiniciados
- **THEN** a punição continua ativa

### Requirement: Configuração de conexão

A conexão SHALL ser configurável quanto a host, porta, nome do banco, usuário, senha, prefixo de tabelas, uso de SSL e parâmetros do pool de conexões (tamanho máximo, mínimo ocioso, timeout de conexão, tempo de vida máximo).

Credenciais SHALL NOT ser gravadas em log em nenhuma circunstância, incluindo mensagens de erro e stack traces.

#### Scenario: Prefixo de tabelas aplicado

- **GIVEN** o prefixo configurado como `sp_`
- **WHEN** o schema é criado
- **THEN** todas as tabelas do sistema são criadas com esse prefixo

#### Scenario: Credenciais não vazam em erro

- **WHEN** a conexão falha por senha incorreta
- **THEN** a mensagem registrada no log descreve a falha sem incluir a senha nem a URL completa com credenciais

#### Scenario: Falha de conexão na inicialização

- **WHEN** o banco está inacessível durante a inicialização do plugin
- **THEN** o plugin registra o erro e não é habilitado, em vez de iniciar em estado degradado silencioso

### Requirement: Registro de punição persistido

Cada punição persistida SHALL conter, no mínimo:

- um identificador único e estável, exibível à staff e ao jogador punido;
- categoria;
- UUID do alvo e nome do alvo no momento da aplicação;
- UUID e nome do autor, ou marcador de console;
- id do motivo e nome de exibição do motivo no momento da aplicação;
- instante de aplicação;
- instante de expiração, ou marcador de permanente;
- identificador do servidor de origem;
- estado de revogação: se revogada, o UUID e nome de quem revogou (ou marcador de console ou de sistema), o instante e o motivo da revogação.

Instantes SHALL ser armazenados em UTC.

#### Scenario: Registro completo

- **WHEN** uma punição é aplicada
- **THEN** todos os campos acima são gravados na mesma operação

#### Scenario: Identificador exibível

- **WHEN** uma punição é aplicada
- **THEN** seu identificador pode ser mostrado na tela de rejeição e usado pela staff para localizar o registro exato

#### Scenario: Instantes em UTC

- **GIVEN** servidores em fusos horários diferentes
- **WHEN** cada um grava uma punição
- **THEN** os instantes gravados são comparáveis entre si, porque todos estão em UTC

### Requirement: Perfis de jogador para resolução offline

O sistema SHALL manter uma tabela de perfis associando UUID ao último nome conhecido e ao instante do último acesso, atualizada a cada login. Essa tabela SHALL ser a base da resolução de alvos e stafers offline por nome.

#### Scenario: Perfil atualizado no login

- **WHEN** um jogador entra na rede
- **THEN** seu UUID, nome atual e instante de acesso são gravados ou atualizados

#### Scenario: Resolução por nome antigo

- **GIVEN** um jogador que trocou de nome
- **WHEN** a staff informa o nome atual dele
- **THEN** o UUID correto é resolvido a partir do perfil atualizado no último login

#### Scenario: Nome reutilizado por outra conta

- **GIVEN** dois perfis que já usaram o mesmo nome em momentos diferentes
- **WHEN** esse nome é resolvido
- **THEN** o perfil com o acesso mais recente é escolhido

### Requirement: Migrações versionadas e idempotentes

O sistema SHALL criar e evoluir o schema automaticamente através de migrações versionadas, registrando a versão aplicada no próprio banco. As migrações SHALL ser idempotentes e SHALL ser seguras quando vários servidores da rede iniciarem simultaneamente.

#### Scenario: Primeira inicialização

- **GIVEN** um banco vazio
- **WHEN** o plugin inicia
- **THEN** todas as tabelas são criadas e a versão do schema é registrada

#### Scenario: Inicialização com schema atual

- **GIVEN** um banco já na versão mais recente
- **WHEN** o plugin inicia
- **THEN** nenhuma alteração de schema é feita e a inicialização prossegue

#### Scenario: Inicialização simultânea de vários servidores

- **GIVEN** um banco vazio
- **WHEN** três servidores da rede iniciam ao mesmo tempo
- **THEN** as migrações são aplicadas exatamente uma vez, sem erro de objeto duplicado em nenhum dos servidores

#### Scenario: Schema mais novo que o plugin

- **GIVEN** um banco cuja versão de schema é maior do que a suportada pela versão do plugin em execução
- **WHEN** o plugin inicia
- **THEN** o plugin registra o erro e não é habilitado, para não corromper dados

### Requirement: Toda operação de armazenamento é assíncrona

Nenhuma operação de banco SHALL ser executada na thread principal do servidor. As operações SHALL ser executadas em um pool dedicado e SHALL retornar de forma assíncrona ao chamador.

#### Scenario: Nenhuma consulta na main thread

- **WHEN** qualquer comando, GUI ou listener do sistema precisa de dados persistidos
- **THEN** a consulta ocorre fora da thread principal

#### Scenario: Timeout de consulta

- **GIVEN** um timeout de consulta configurável
- **WHEN** uma consulta excede esse timeout
- **THEN** a operação é abortada, o chamador recebe uma falha, e o erro é registrado no log

### Requirement: Desempenho das consultas de leitura

O schema SHALL prover índices que sustentem, sem varredura completa de tabela, as consultas de: punição ativa por UUID e categoria, histórico por UUID ordenado por instante de aplicação, e agregação por autor, categoria e faixa de instante de aplicação.

#### Scenario: Verificação de login indexada

- **GIVEN** uma tabela com um grande volume de punições
- **WHEN** o estado de ban de um jogador é verificado no login
- **THEN** a consulta usa índice e não faz varredura completa da tabela

#### Scenario: Agregação de relatório indexada

- **GIVEN** a mesma tabela
- **WHEN** um relatório mensal por autor é agregado
- **THEN** a consulta usa índice sobre autor, categoria e instante de aplicação

### Requirement: Resiliência a perda de conexão

O sistema SHALL detectar perda de conexão com o banco, tentar reconectar automaticamente com espera crescente entre tentativas, e expor o estado de saúde da conexão. Operações que falharem por indisponibilidade SHALL retornar erro ao chamador em vez de bloquear indefinidamente.

#### Scenario: Reconexão automática

- **GIVEN** um banco que ficou indisponível e voltou
- **WHEN** a conexão é restabelecida
- **THEN** as operações voltam a funcionar sem reiniciar o servidor, e a recuperação é registrada no log

#### Scenario: Falha reportada ao chamador

- **GIVEN** o banco indisponível
- **WHEN** uma punição é aplicada
- **THEN** o chamador recebe uma falha dentro do timeout configurado, em vez de aguardar indefinidamente

### Requirement: Integridade da escrita de punição

A criação de uma punição e a atualização do perfil do alvo SHALL ser aplicadas de forma que o sistema nunca observe uma punição sem os seus campos obrigatórios. Uma escrita parcial SHALL NOT ser visível a nenhuma leitura.

#### Scenario: Falha no meio da escrita

- **WHEN** a conexão cai durante a gravação de uma punição
- **THEN** nenhuma punição parcial fica visível para leituras posteriores

### Requirement: Retenção configurável de registros antigos

O sistema SHALL suportar uma política configurável de retenção que remove punições encerradas — expiradas ou revogadas — mais antigas que um período definido. A política SHALL vir desabilitada por padrão e SHALL NOT remover punições ativas em nenhuma circunstância.

#### Scenario: Retenção desabilitada por padrão

- **GIVEN** a configuração padrão
- **WHEN** o plugin executa por tempo indeterminado
- **THEN** nenhum registro é removido automaticamente

#### Scenario: Retenção habilitada

- **GIVEN** a retenção configurada para 365 dias
- **WHEN** a rotina de retenção executa
- **THEN** punições encerradas há mais de 365 dias são removidas e punições ativas são preservadas, independentemente da idade
