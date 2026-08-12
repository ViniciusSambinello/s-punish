## Purpose

Define como uma punição ativa é encerrada manualmente antes do prazo, preservando o registro original para auditoria e refletindo o encerramento imediatamente no enforcement.

## ADDED Requirements

### Requirement: Comandos de revogação por categoria

O sistema SHALL expor `/unban <player>` e `/unmute <player>`, cada um revogando a punição ativa da categoria correspondente para o jogador informado. Ambos SHALL aceitar um motivo livre opcional ao final (`/unban <player> [motivo...]`).

#### Scenario: Revogação de ban

- **GIVEN** um jogador com ban ativo
- **WHEN** um membro da staff autorizado executa `/unban Steve`
- **THEN** o ban deixa de estar ativo, o autor recebe a mensagem configurável de confirmação e o jogador consegue entrar na próxima tentativa

#### Scenario: Revogação de mute

- **GIVEN** um jogador online com mute ativo
- **WHEN** um membro da staff autorizado executa `/unmute Steve`
- **THEN** o mute deixa de estar ativo e o jogador consegue falar na mensagem seguinte, sem relogin

#### Scenario: Motivo de revogação opcional

- **WHEN** um membro da staff executa `/unban Steve apelacao aceita`
- **THEN** o texto `apelacao aceita` é persistido como motivo da revogação e aparece no histórico

#### Scenario: Sem punição ativa

- **WHEN** um membro da staff executa `/unban Steve` para um jogador sem ban ativo
- **THEN** o comando é recusado com a mensagem configurável de ausência de punição ativa e nada é alterado

### Requirement: Permissão de revogação por categoria

Revogar SHALL exigir `spunish.unpunish.<category>` em minúsculas. A permissão de aplicar uma categoria SHALL NOT conceder implicitamente a permissão de revogá-la.

#### Scenario: Sem permissão de revogação

- **GIVEN** um membro da staff com `spunish.punish.ban` mas sem `spunish.unpunish.ban`
- **WHEN** ele executa `/unban Steve`
- **THEN** o comando é recusado com a mensagem configurável de permissão insuficiente e a punição permanece ativa

#### Scenario: Revogação de punição aplicada por outra pessoa

- **GIVEN** um membro da staff com `spunish.unpunish.ban`
- **WHEN** ele revoga um ban aplicado por outro membro da staff
- **THEN** a revogação é aceita

### Requirement: Registro de auditoria da revogação

A revogação SHALL preservar o registro original da punição e SHALL adicionar a ele: identidade de quem revogou (UUID e nome, ou marcador de console ou de sistema), instante da revogação e motivo da revogação quando informado.

A punição original SHALL NOT ser apagada nem ter seus campos originais alterados.

#### Scenario: Registro preservado

- **GIVEN** um ban aplicado em uma data específica com um motivo específico
- **WHEN** ele é revogado
- **THEN** o histórico continua exibindo a data, o autor e o motivo originais, acrescidos dos dados da revogação

#### Scenario: Revogação pelo console

- **WHEN** o console executa `/unban Steve`
- **THEN** a revogação é registrada com o marcador de console como autor

#### Scenario: Revogação automática por sobreposição

- **GIVEN** uma punição encerrada automaticamente porque outra da mesma categoria a sobrepôs
- **WHEN** o histórico do jogador é consultado
- **THEN** a punição encerrada aparece como revogada pelo sistema, distinguível de uma revogação feita por uma pessoa

### Requirement: Revogação reflete imediatamente no enforcement de toda a rede

Após uma revogação bem-sucedida, o efeito SHALL cessar em todos os servidores da rede e no proxy dentro da janela de propagação definida pela sincronização de rede, sem exigir reinício.

#### Scenario: Mute revogado em outro servidor

- **GIVEN** um jogador mutado e online em um servidor diferente daquele onde a revogação foi executada
- **WHEN** o mute é revogado
- **THEN** ele consegue falar dentro da janela de propagação

#### Scenario: Notificação de revogação à staff

- **WHEN** uma revogação é concluída
- **THEN** os jogadores online com `spunish.notify`, em qualquer servidor da rede, recebem o anúncio configurável de revogação

### Requirement: Falha de persistência não revoga

Quando a persistência da revogação falhar, a punição SHALL permanecer ativa e o autor SHALL receber a mensagem configurável de erro interno.

#### Scenario: Escrita falha

- **WHEN** a gravação da revogação falha
- **THEN** o jogador continua punido, nenhum anúncio é emitido, e a falha é registrada no log com o erro subjacente
