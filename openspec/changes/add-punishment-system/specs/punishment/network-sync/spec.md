## Purpose

Garante que uma punição aplicada em qualquer ponto da rede passe a valer em todos os servidores e no proxy dentro de uma janela previsível, e que o bloqueio de acesso aconteça na borda antes de o jogador alcançar um backend.

## ADDED Requirements

### Requirement: Bloqueio de ban na borda do proxy

O módulo de proxy SHALL verificar o estado de ban durante o handshake de login e SHALL recusar a conexão antes que o jogador seja encaminhado a qualquer servidor backend. A tela de rejeição no proxy SHALL usar o mesmo texto configurável utilizado pelos backends.

#### Scenario: Recusa antes do backend

- **WHEN** um jogador banido tenta conectar à rede
- **THEN** o proxy recusa a conexão e nenhum servidor backend recebe a tentativa de login

#### Scenario: Texto de rejeição consistente

- **WHEN** o proxy recusa um jogador banido
- **THEN** a tela exibida é gerada a partir da mesma configuração de mensagens usada pelos backends

#### Scenario: Kick de rede em jogador online

- **GIVEN** um jogador online em um backend
- **WHEN** ele é banido a partir de outro servidor
- **THEN** o proxy o desconecta da rede inteira, e não apenas do backend atual

### Requirement: Backends permanecem seguros sem o proxy

O bloqueio de login nos servidores Paper SHALL ser mantido independentemente do módulo de proxy. Um backend SHALL recusar o login de um jogador banido mesmo que o módulo de proxy esteja ausente, desatualizado ou desabilitado.

#### Scenario: Proxy sem o módulo instalado

- **GIVEN** um proxy sem o módulo do sistema instalado
- **WHEN** um jogador banido tenta entrar em um backend
- **THEN** o backend recusa o login

#### Scenario: Acesso direto ao backend

- **GIVEN** um jogador banido que conecta diretamente à porta de um backend
- **WHEN** o login é processado
- **THEN** o backend recusa a conexão

### Requirement: Propagação de eventos de punição entre instâncias

Toda punição criada e toda revogação SHALL gerar um evento persistido que as demais instâncias — backends e proxy — consomem para invalidar o estado em cache e aplicar o efeito localmente.

A janela máxima de propagação SHALL ser configurável e o valor padrão SHALL NOT exceder 5 segundos.

#### Scenario: Mute propagado

- **GIVEN** um jogador online no servidor B
- **WHEN** ele é mutado a partir do servidor A
- **THEN** o servidor B bloqueia o chat dele dentro da janela de propagação configurada

#### Scenario: Revogação propagada

- **GIVEN** um jogador mutado e online no servidor B
- **WHEN** o mute é revogado a partir do servidor A
- **THEN** o servidor B libera o chat dele dentro da janela de propagação configurada

#### Scenario: Instância que estava offline

- **GIVEN** um servidor que estava desligado enquanto punições e revogações ocorreram
- **WHEN** ele volta a ficar online
- **THEN** ele passa a refletir o estado atual, sem exigir intervenção manual

#### Scenario: Eventos não se acumulam indefinidamente

- **WHEN** o sistema opera por um período prolongado
- **THEN** os eventos de sincronização já consumidos por todas as instâncias são descartados automaticamente

### Requirement: Cache local de estado por jogador online

Cada instância SHALL manter em memória o estado de punição dos jogadores conectados a ela, carregado no login e mantido coerente por eventos de sincronização e por punições locais. O cache SHALL ser descartado quando o jogador se desconecta daquela instância.

#### Scenario: Estado carregado no login

- **WHEN** um jogador entra em um backend
- **THEN** seu estado de mute é carregado antes de ele poder enviar a primeira mensagem

#### Scenario: Cache liberado na saída

- **WHEN** um jogador se desconecta
- **THEN** o estado em cache dele naquela instância é descartado

#### Scenario: Punição local reflete de imediato

- **GIVEN** um jogador online no mesmo servidor onde a punição foi aplicada
- **WHEN** a punição é gravada com sucesso
- **THEN** o cache local é atualizado imediatamente, sem aguardar a janela de propagação

### Requirement: Identificação do servidor de origem

Cada instância SHALL ter um identificador configurável que é registrado em toda punição aplicada nela e usado nos eventos de sincronização para que a instância originadora ignore o próprio evento.

Quando o identificador não estiver configurado, o sistema SHALL derivar um valor estável e registrar um aviso no log recomendando a configuração explícita.

#### Scenario: Origem registrada

- **WHEN** uma punição é aplicada em um servidor identificado como `lobby-1`
- **THEN** o registro persistido e o histórico apresentam `lobby-1` como servidor de origem

#### Scenario: Instância originadora não reprocessa

- **WHEN** a instância que aplicou a punição consome os eventos de sincronização
- **THEN** ela ignora o evento que ela mesma gerou, evitando aplicar o efeito duas vezes

#### Scenario: Identificador ausente

- **GIVEN** um servidor sem identificador configurado
- **WHEN** o plugin inicia
- **THEN** um identificador estável é derivado e o log recomenda configurá-lo explicitamente

### Requirement: Falha de sincronização degrada sem derrubar o servidor

Quando o consumo de eventos de sincronização falhar, a instância SHALL continuar operando com o estado que possui, SHALL registrar a falha no log, e SHALL retomar o consumo automaticamente quando o armazenamento voltar a responder. A falha de sincronização SHALL NOT desabilitar o plugin nem interromper o enforcement local.

#### Scenario: Consumo falha temporariamente

- **GIVEN** um servidor cuja leitura de eventos falhou
- **WHEN** o armazenamento volta a responder
- **THEN** o consumo é retomado a partir do último evento processado, sem perder eventos intermediários

#### Scenario: Enforcement local preservado

- **GIVEN** um servidor com a sincronização falhando
- **WHEN** um jogador com mute já carregado tenta falar
- **THEN** o mute continua sendo aplicado normalmente
