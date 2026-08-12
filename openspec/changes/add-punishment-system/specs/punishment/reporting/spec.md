## Purpose

Fornece à coordenação relatórios de auditoria sobre as punições **aplicadas** pela staff, agregadas por janela de tempo e por membro da equipe, com acesso restrito por permissão.

## ADDED Requirements

### Requirement: Comando de relatório por categoria

O sistema SHALL expor `/record <category>`, que abre uma GUI de relatório das punições aplicadas na categoria informada, considerando toda a staff. O comando SHALL exigir a permissão `spunish.record`.

O tab complete da posição 1 SHALL sugerir `ban` e `mute`.

#### Scenario: Relatório geral aberto

- **WHEN** um membro da coordenação com `spunish.record` executa `/record ban`
- **THEN** a GUI de relatório de `BAN` é aberta com as punições de toda a staff

#### Scenario: Sem permissão

- **WHEN** um jogador sem `spunish.record` executa `/record ban`
- **THEN** o comando é recusado com a mensagem configurável de permissão insuficiente

#### Scenario: Categoria inválida

- **WHEN** um usuário executa `/record kick`
- **THEN** o comando é recusado com a mensagem configurável de categoria inválida

### Requirement: Janelas de tempo do relatório

A GUI de relatório SHALL oferecer exatamente quatro janelas selecionáveis: **diária**, **semanal**, **mensal** e **desde o início**. A janela selecionada SHALL ficar visualmente destacada e SHALL poder ser trocada sem fechar a GUI.

As janelas SHALL ser calculadas no fuso horário configurável do relatório, com estas definições:

- diária: do início do dia corrente até agora;
- semanal: dos últimos 7 dias completos até agora;
- mensal: dos últimos 30 dias completos até agora;
- desde o início: sem limite inferior.

#### Scenario: Troca de janela

- **GIVEN** a GUI de relatório aberta na janela diária
- **WHEN** a coordenação seleciona a janela mensal
- **THEN** os números são recalculados para os últimos 30 dias e a janela mensal fica destacada, sem fechar a GUI

#### Scenario: Fuso horário configurado

- **GIVEN** o fuso horário do relatório configurado como `America/Sao_Paulo`
- **WHEN** a janela diária é consultada
- **THEN** o corte de início do dia usa a meia-noite naquele fuso, não a meia-noite UTC

#### Scenario: Janela sem registros

- **GIVEN** uma categoria sem punições na janela selecionada
- **WHEN** o relatório é exibido
- **THEN** o total exibido é zero e a GUI mostra o texto configurável de ausência de dados

### Requirement: Conteúdo do relatório geral

Para a janela selecionada, o relatório geral SHALL exibir, no mínimo:

- o total de punições aplicadas na categoria;
- a divisão desse total por estado atual (ativas, expiradas, revogadas);
- o ranking dos membros da staff por quantidade de punições aplicadas, em ordem decrescente;
- a distribuição por motivo, em ordem decrescente de quantidade.

#### Scenario: Totais e ranking

- **GIVEN** 40 bans aplicados na janela, sendo 25 por um membro e 15 por outro
- **WHEN** o relatório geral é exibido
- **THEN** o total exibido é 40 e o ranking apresenta os dois membros em ordem decrescente com suas quantidades

#### Scenario: Distribuição por motivo

- **GIVEN** punições na janela distribuídas entre três motivos
- **WHEN** o relatório geral é exibido
- **THEN** os três motivos aparecem com suas quantidades, em ordem decrescente

#### Scenario: Ranking limitado e navegável

- **GIVEN** mais membros da staff do que cabem na área de ranking
- **WHEN** o relatório é exibido
- **THEN** o ranking é paginado ou truncado de forma configurável, indicando que há mais entradas

### Requirement: Relatório por membro da staff

O sistema SHALL expor `/record <category> <staffer>`, que abre a GUI de relatório restrita às punições daquela categoria aplicadas pelo membro da staff informado, com as mesmas quatro janelas de tempo.

O tab complete da posição 2 SHALL sugerir nomes de jogadores online que possuam permissão de punir a categoria informada.

#### Scenario: Relatório individual

- **WHEN** a coordenação executa `/record ban Alex`
- **THEN** a GUI exibe somente as punições de `BAN` aplicadas por Alex, com as quatro janelas disponíveis

#### Scenario: Conteúdo do relatório individual

- **WHEN** um relatório individual é exibido
- **THEN** ele apresenta o total do membro na janela, a divisão por estado atual, a distribuição por motivo, e a lista das punições mais recentes aplicadas por ele

#### Scenario: Staffer desconhecido

- **WHEN** a coordenação executa `/record ban` para um nome que nunca entrou na rede
- **THEN** o comando é recusado com a mensagem configurável de jogador não encontrado

#### Scenario: Staffer sem punições na janela

- **GIVEN** um membro da staff sem punições na janela selecionada
- **WHEN** seu relatório é exibido
- **THEN** o total é zero e a GUI mostra o texto configurável de ausência de dados

### Requirement: Separação entre relatório próprio e de terceiros

Consultar o relatório de **outro** membro da staff SHALL exigir a permissão `spunish.record.others`, além de `spunish.record`. Um membro com `spunish.record` mas sem `spunish.record.others` SHALL poder consultar apenas o seu próprio relatório individual, e SHALL NOT poder abrir o relatório geral da categoria.

#### Scenario: Consulta do próprio relatório permitida

- **GIVEN** um membro da staff com `spunish.record` e sem `spunish.record.others`
- **WHEN** ele executa `/record ban` com o próprio nome como staffer
- **THEN** o relatório dele é exibido

#### Scenario: Consulta de terceiro recusada

- **GIVEN** o mesmo membro sem `spunish.record.others`
- **WHEN** ele executa `/record ban` com o nome de outro membro
- **THEN** o comando é recusado com a mensagem configurável de permissão insuficiente

#### Scenario: Relatório geral exige permissão de terceiros

- **GIVEN** o mesmo membro sem `spunish.record.others`
- **WHEN** ele executa `/record ban` sem informar staffer
- **THEN** o comando é recusado com a mensagem configurável de permissão insuficiente

#### Scenario: Coordenação vê qualquer relatório

- **GIVEN** um membro com `spunish.record` e `spunish.record.others`
- **WHEN** ele consulta o relatório geral ou o de qualquer membro
- **THEN** o relatório é exibido

### Requirement: Punições revogadas permanecem contabilizadas

Punições revogadas SHALL continuar sendo contadas no total da janela em que foram aplicadas, e SHALL ser adicionalmente reportadas como revogadas. O relatório SHALL exibir a taxa de revogação do escopo consultado.

#### Scenario: Revogação não apaga a punição do total

- **GIVEN** 10 bans aplicados por um membro na janela, dos quais 3 foram revogados
- **WHEN** o relatório individual dele é exibido
- **THEN** o total apresentado é 10, sendo 3 marcadas como revogadas, e a taxa de revogação exibida é 30%

### Requirement: Relatório reflete a rede inteira

Os relatórios SHALL agregar as punições de todos os servidores da rede que compartilham o mesmo armazenamento, independentemente de onde o comando foi executado. O relatório SHALL indicar o servidor de origem nas listagens de punições individuais.

#### Scenario: Agregação entre servidores

- **GIVEN** punições aplicadas em três servidores diferentes da rede
- **WHEN** o relatório geral é consultado a partir de qualquer um deles
- **THEN** as punições dos três servidores estão incluídas nos totais

### Requirement: Consultas agregadas são assíncronas e limitadas

Toda agregação SHALL ser executada fora da thread principal. O sistema SHALL aplicar um cooldown configurável por usuário entre consultas de relatório e SHALL cachear o resultado por uma duração configurável, para evitar sobrecarga do armazenamento.

#### Scenario: Agregação não trava o servidor

- **GIVEN** um armazenamento com grande volume de registros
- **WHEN** a coordenação abre o relatório desde o início
- **THEN** o servidor continua processando ticks normalmente e a GUI é preenchida quando a agregação conclui

#### Scenario: Cooldown de consulta

- **GIVEN** um cooldown configurável de relatório
- **WHEN** o mesmo usuário solicita um novo relatório dentro do cooldown
- **THEN** o sistema responde com a mensagem configurável de cooldown ou serve o resultado cacheado, sem executar nova agregação

#### Scenario: Cache expirado

- **GIVEN** um resultado cacheado cuja duração expirou
- **WHEN** o relatório é solicitado novamente
- **THEN** uma nova agregação é executada
