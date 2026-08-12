## Purpose

Define o efeito real das punições ativas sobre o jogador: como um ban impede o acesso, como um mute impede a comunicação, e como as punições deixam de valer quando expiram.

## ADDED Requirements

### Requirement: Definição de punição ativa

Uma punição SHALL ser considerada ativa quando, e somente quando, não estiver revogada e não estiver expirada. Uma punição está expirada quando possui instante de expiração e esse instante já passou em relação ao relógio de referência. Punições permanentes nunca expiram.

A expiração SHALL ser avaliada no momento da consulta, de modo que a correção do enforcement não dependa da execução de nenhuma tarefa periódica.

#### Scenario: Punição temporária vencida

- **GIVEN** um ban com expiração 10 minutos no passado e nenhuma tarefa de limpeza executada desde então
- **WHEN** o jogador tenta entrar
- **THEN** o login é permitido, porque a punição não é mais ativa

#### Scenario: Punição permanente

- **GIVEN** um ban sem instante de expiração
- **WHEN** o jogador tenta entrar em qualquer momento futuro
- **THEN** o login é recusado

#### Scenario: Punição revogada

- **GIVEN** um ban revogado, cuja expiração ainda estaria no futuro
- **WHEN** o jogador tenta entrar
- **THEN** o login é permitido

### Requirement: Ban bloqueia o login

Enquanto houver um `BAN` ativo para o UUID do jogador, toda tentativa de login SHALL ser recusada. A recusa SHALL ocorrer na fase de pré-login, antes de o jogador ser adicionado ao mundo.

#### Scenario: Login recusado por ban ativo

- **WHEN** um jogador com ban ativo tenta entrar
- **THEN** a conexão é encerrada com a tela de rejeição de ban e o jogador nunca aparece no mundo

#### Scenario: Login permitido sem ban

- **WHEN** um jogador sem ban ativo tenta entrar
- **THEN** o login prossegue normalmente

### Requirement: Tela de rejeição de ban

A tela de rejeição SHALL ser configurável e SHALL suportar, no mínimo, os placeholders de nome de exibição do motivo, autor da punição, duração restante formatada, instante de aplicação e identificador da punição.

Para punições permanentes, o texto de duração SHALL usar a mensagem configurável de permanente em vez de um tempo restante.

#### Scenario: Ban temporário mostra tempo restante

- **GIVEN** um ban ativo que expira em 2 dias e 3 horas
- **WHEN** o jogador tenta entrar
- **THEN** a tela de rejeição exibe o tempo restante formatado conforme a configuração de formatação de duração

#### Scenario: Ban permanente

- **GIVEN** um ban permanente ativo
- **WHEN** o jogador tenta entrar
- **THEN** a tela de rejeição usa o texto configurável de punição permanente no lugar do tempo restante

#### Scenario: Identificador exibido para apelação

- **WHEN** um jogador banido é rejeitado
- **THEN** a tela inclui o identificador da punição, permitindo que a staff localize o registro exato

### Requirement: Ban desconecta o jogador online

Quando um `BAN` é aplicado a um jogador que está online em qualquer servidor da rede, esse jogador SHALL ser desconectado com a mesma tela de rejeição de ban, sem depender de uma nova tentativa de login.

#### Scenario: Kick imediato no servidor local

- **GIVEN** um jogador online no mesmo servidor onde o comando foi executado
- **WHEN** ele é banido
- **THEN** ele é desconectado imediatamente com a tela de rejeição de ban

#### Scenario: Kick em outro servidor da rede

- **GIVEN** um jogador online em um servidor diferente daquele onde o comando foi executado
- **WHEN** ele é banido
- **THEN** ele é desconectado dentro da janela de propagação definida pela sincronização de rede

### Requirement: Mute bloqueia a comunicação

Enquanto houver um `MUTE` ativo para o jogador, o sistema SHALL bloquear as mensagens de chat que ele enviar e SHALL bloquear a execução dos comandos listados na lista configurável de comandos de comunicação. A mensagem bloqueada SHALL NOT ser entregue a nenhum outro jogador nem registrada no log de chat.

#### Scenario: Mensagem de chat bloqueada

- **WHEN** um jogador mutado envia uma mensagem no chat
- **THEN** a mensagem não é entregue a ninguém e o jogador recebe o aviso configurável de mute

#### Scenario: Comando de comunicação bloqueado

- **GIVEN** que `/msg` consta na lista configurável de comandos bloqueados
- **WHEN** um jogador mutado executa `/msg Alex oi`
- **THEN** o comando não é executado e o jogador recebe o aviso configurável de mute

#### Scenario: Comando não listado permanece disponível

- **GIVEN** que `/spawn` não consta na lista configurável
- **WHEN** um jogador mutado executa `/spawn`
- **THEN** o comando é executado normalmente

#### Scenario: Alias de comando bloqueado

- **GIVEN** que `/msg` consta na lista configurável e `/tell` é um alias registrado de `/msg`
- **WHEN** um jogador mutado executa `/tell Alex oi`
- **THEN** o comando é bloqueado

### Requirement: Aviso de mute informa o tempo restante

O aviso enviado ao jogador mutado SHALL ser configurável e SHALL suportar os placeholders de nome de exibição do motivo, autor, duração restante formatada e identificador da punição. Para mutes permanentes, SHALL usar o texto configurável de permanente.

#### Scenario: Aviso com tempo restante

- **GIVEN** um mute ativo que expira em 45 minutos
- **WHEN** o jogador tenta falar
- **THEN** o aviso exibe o tempo restante formatado

#### Scenario: Aviso limitado por cooldown

- **GIVEN** um cooldown configurável de aviso de mute
- **WHEN** o jogador tenta falar várias vezes dentro do cooldown
- **THEN** o aviso é enviado apenas na primeira tentativa da janela

### Requirement: Mute aplicado a jogador online tem efeito imediato

Quando um `MUTE` é aplicado a um jogador online, o bloqueio de chat SHALL valer a partir da próxima mensagem que ele enviar, sem exigir relogin.

#### Scenario: Efeito sem relogin

- **GIVEN** um jogador online que não estava mutado
- **WHEN** ele é mutado e em seguida tenta falar
- **THEN** a mensagem é bloqueada

### Requirement: Expiração natural restaura o jogador

Quando a expiração de uma punição é atingida, o jogador SHALL voltar ao estado normal sem intervenção da staff. Um jogador com mute expirado SHALL poder falar novamente na próxima mensagem, e um jogador com ban expirado SHALL poder entrar na próxima tentativa.

#### Scenario: Mute expira durante a sessão

- **GIVEN** um jogador online cujo mute expira em 30 segundos
- **WHEN** ele tenta falar após a expiração
- **THEN** a mensagem é entregue normalmente e nenhum aviso de mute é enviado

#### Scenario: Ban expira com o jogador offline

- **GIVEN** um jogador banido cuja punição expirou
- **WHEN** ele tenta entrar
- **THEN** o login é permitido

### Requirement: Comportamento em indisponibilidade do banco

O sistema SHALL usar um modo de falha configurável para quando o estado de punição não puder ser determinado por indisponibilidade do armazenamento, com as opções `ALLOW` (permitir a ação) e `DENY` (recusar a ação). O padrão SHALL ser `DENY` para login e `ALLOW` para chat.

Toda ocorrência SHALL ser registrada no log com o erro subjacente.

#### Scenario: Login com banco indisponível e padrão DENY

- **GIVEN** o armazenamento indisponível e o modo de falha de login em `DENY`
- **WHEN** um jogador tenta entrar
- **THEN** o login é recusado com a mensagem configurável de indisponibilidade temporária, e a falha é registrada no log

#### Scenario: Chat com banco indisponível e padrão ALLOW

- **GIVEN** o armazenamento indisponível e o modo de falha de chat em `ALLOW`
- **WHEN** um jogador envia uma mensagem
- **THEN** a mensagem é entregue e a falha é registrada no log

### Requirement: Verificação de punição não bloqueia a thread principal

A verificação de mute no envio de mensagem SHALL ser resolvida a partir de um estado já carregado em memória para o jogador, sem consulta síncrona ao armazenamento na thread principal. O estado SHALL ser carregado no login e atualizado por eventos de punição e de sincronização.

#### Scenario: Chat não consulta o banco

- **GIVEN** um jogador online cujo estado de mute já foi carregado no login
- **WHEN** ele envia uma mensagem
- **THEN** a decisão de bloqueio é tomada sem nenhuma consulta ao armazenamento durante o processamento do evento
