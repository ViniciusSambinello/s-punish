## Purpose

Permite que a staff consulte todas as punições que um jogador já recebeu, com estado atual e detalhes de cada registro, para embasar decisões de reincidência e responder apelações.

## ADDED Requirements

### Requirement: Comando de histórico

O sistema SHALL expor `/history <player>`, que abre uma GUI com o histórico de punições **recebidas** pelo jogador informado, de todas as categorias, ordenadas da mais recente para a mais antiga pelo instante de aplicação.

O comando SHALL exigir a permissão `spunish.history`. O tab complete da posição 1 SHALL sugerir nomes de jogadores online.

#### Scenario: Histórico aberto

- **WHEN** um membro da staff com `spunish.history` executa `/history Steve`
- **THEN** uma GUI é aberta listando as punições recebidas por Steve, da mais recente para a mais antiga

#### Scenario: Sem permissão

- **WHEN** um jogador sem `spunish.history` executa `/history Steve`
- **THEN** o comando é recusado com a mensagem configurável de permissão insuficiente

#### Scenario: Jogador desconhecido

- **WHEN** um membro da staff executa `/history` para um nome que nunca entrou na rede
- **THEN** o comando é recusado com a mensagem configurável de jogador não encontrado

#### Scenario: Jogador sem punições

- **GIVEN** um jogador conhecido que nunca foi punido
- **WHEN** seu histórico é consultado
- **THEN** a GUI é aberta exibindo o texto configurável de histórico vazio

#### Scenario: Console

- **WHEN** o console executa `/history Steve`
- **THEN** o histórico é apresentado como texto no console, já que o console não pode abrir GUIs

### Requirement: Conteúdo de cada entrada do histórico

Cada entrada exibida SHALL apresentar, no mínimo: categoria, nome de exibição do motivo registrado no momento da aplicação, nome do autor da punição, data e hora de aplicação, duração original, e estado atual.

O estado atual SHALL ser exatamente um entre **ativa**, **expirada** e **revogada**, e SHALL ser visualmente distinguível por ícone ou cor configurável.

#### Scenario: Entrada ativa mostra tempo restante

- **GIVEN** uma punição ativa que expira em 3 dias
- **WHEN** o histórico é exibido
- **THEN** a entrada aparece como ativa e informa o tempo restante formatado

#### Scenario: Entrada permanente ativa

- **GIVEN** uma punição permanente não revogada
- **WHEN** o histórico é exibido
- **THEN** a entrada aparece como ativa e usa o texto configurável de permanente no lugar do tempo restante

#### Scenario: Entrada revogada mostra dados da revogação

- **GIVEN** uma punição revogada
- **WHEN** o histórico é exibido
- **THEN** a entrada aparece como revogada e informa quem revogou, quando, e o motivo da revogação quando houver

#### Scenario: Entrada expirada

- **GIVEN** uma punição temporária cuja expiração já passou e que não foi revogada
- **WHEN** o histórico é exibido
- **THEN** a entrada aparece como expirada

#### Scenario: Nome de exibição histórico preservado

- **GIVEN** uma punição aplicada com um motivo que depois foi renomeado na configuração
- **WHEN** o histórico é exibido
- **THEN** a entrada mostra o nome de exibição vigente no momento da aplicação, não o nome atual do catálogo

### Requirement: Paginação do histórico

A GUI SHALL paginar o histórico com um tamanho de página configurável e SHALL exibir controles de página anterior e próxima, além do indicador de página atual e total. As páginas SHALL ser carregadas sob demanda, sem trazer todo o histórico de uma vez.

#### Scenario: Navegação entre páginas

- **GIVEN** um jogador com mais punições do que cabem em uma página
- **WHEN** a staff aciona o controle de próxima página
- **THEN** a página seguinte de punições é exibida, mantendo a mesma ordenação

#### Scenario: Controles nos limites

- **WHEN** a primeira página é exibida
- **THEN** o controle de página anterior não é exibido ou está desabilitado, e o mesmo vale para o controle de próxima página na última página

#### Scenario: Carga sob demanda

- **GIVEN** um jogador com um histórico muito extenso
- **WHEN** o histórico é aberto
- **THEN** apenas os registros da página exibida são recuperados do armazenamento

### Requirement: Filtro por categoria no histórico

A GUI SHALL oferecer um controle para alternar entre todas as categorias, somente `BAN` e somente `MUTE`. Alternar o filtro SHALL reiniciar a paginação na primeira página.

#### Scenario: Filtrar por categoria

- **GIVEN** um jogador com bans e mutes no histórico
- **WHEN** a staff aplica o filtro de `MUTE`
- **THEN** apenas mutes são exibidos e a visualização volta para a primeira página

### Requirement: Detalhe de uma punição

Clicar em uma entrada do histórico SHALL exibir seus detalhes completos, incluindo o identificador da punição e o servidor de origem, em formato copiável ou legível para registro em apelações.

#### Scenario: Abertura do detalhe

- **WHEN** a staff clica em uma entrada do histórico
- **THEN** os detalhes completos daquela punição são apresentados, incluindo identificador e servidor de origem

### Requirement: Consulta de histórico é assíncrona

A recuperação do histórico SHALL ocorrer fora da thread principal, e a GUI SHALL ser aberta ou atualizada na thread principal somente após os dados estarem disponíveis.

#### Scenario: Banco lento não trava o servidor

- **GIVEN** um armazenamento que responde em 2 segundos
- **WHEN** a staff abre o histórico de um jogador
- **THEN** o servidor continua processando ticks normalmente e a GUI é preenchida quando a consulta conclui
