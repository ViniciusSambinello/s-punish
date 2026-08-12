## Purpose

Define o catálogo configurável de categorias de punição e seus motivos, permitindo que a administração ajuste quais punições existem, como elas aparecem para os jogadores e quanto tempo duram, sem alterar código.

## ADDED Requirements

### Requirement: Categorias fixas de punição

O sistema SHALL suportar exatamente duas categorias de punição: `BAN` e `MUTE`. As categorias SHALL ser identificadas de forma case-insensitive na entrada do usuário e persistidas em forma canônica maiúscula.

#### Scenario: Categoria reconhecida em qualquer caixa

- **WHEN** um usuário informa `ban`, `Ban` ou `BAN` como categoria em um comando
- **THEN** o sistema resolve a categoria `BAN` e prossegue normalmente

#### Scenario: Categoria desconhecida

- **WHEN** um usuário informa uma categoria que não é `BAN` nem `MUTE`
- **THEN** o sistema rejeita o comando e responde com a mensagem configurável de categoria inválida, listando as categorias válidas

### Requirement: Catálogo de motivos por categoria

O sistema SHALL carregar, a partir da configuração, um catálogo de motivos independente para cada categoria. Cada motivo SHALL declarar:

- um **id** estável, único dentro da sua categoria, usado em comandos e como chave interna;
- um **nome de exibição**, apresentado ao jogador punido e à staff e persistido junto da punição;
- uma **duração**, expressa como quantidade com unidade (`s`, `m`, `h`, `d`, `w`, `mo`, `y`) ou como o valor literal `n`, que significa que a punição nunca expira;
- opcionalmente, um **ícone** e uma **descrição** para exibição em GUI;
- opcionalmente, uma **permissão** adicional exigida para usar aquele motivo.

#### Scenario: Motivo com duração determinada

- **GIVEN** um motivo configurado com duração `7d`
- **WHEN** esse motivo é usado para punir um jogador
- **THEN** a punição registrada expira exatamente 7 dias após o instante de aplicação

#### Scenario: Motivo permanente

- **GIVEN** um motivo configurado com duração `n`
- **WHEN** esse motivo é usado para punir um jogador
- **THEN** a punição registrada não possui instante de expiração e permanece ativa até ser revogada manualmente

#### Scenario: Nome de exibição persistido junto da punição

- **GIVEN** um motivo cujo nome de exibição é `Uso de cliente ilegal`
- **WHEN** uma punição é aplicada com esse motivo
- **THEN** o registro persistido contém tanto o id do motivo quanto o texto `Uso de cliente ilegal`, de modo que o histórico permanece legível mesmo se o motivo for renomeado ou removido da configuração depois

### Requirement: Validação do catálogo na carga

O sistema SHALL validar o catálogo ao carregá-lo e SHALL recusar-se a iniciar quando a configuração for inválida, registrando no log cada problema encontrado com o caminho da entrada afetada.

São condições inválidas: id duplicado dentro da mesma categoria, id ausente ou vazio, nome de exibição ausente ou vazio, duração ausente, duração com unidade desconhecida, duração numérica menor ou igual a zero, e categoria desconhecida no arquivo.

#### Scenario: Ids duplicados na mesma categoria

- **GIVEN** dois motivos de `BAN` declarados com o id `hacking`
- **WHEN** o sistema carrega o catálogo
- **THEN** o carregamento falha, o log descreve o id duplicado e sua categoria, e o plugin não é habilitado

#### Scenario: Duração com unidade inválida

- **GIVEN** um motivo com duração `7x`
- **WHEN** o sistema carrega o catálogo
- **THEN** o carregamento falha e o log indica o motivo afetado e as unidades aceitas

#### Scenario: Ids iguais em categorias diferentes são permitidos

- **GIVEN** um motivo `spam` em `MUTE` e outro motivo `spam` em `BAN`
- **WHEN** o sistema carrega o catálogo
- **THEN** o carregamento é bem-sucedido e cada motivo é resolvido dentro do escopo da sua própria categoria

### Requirement: Motivo restrito por permissão

Quando um motivo declara uma permissão adicional, o sistema SHALL exigir que o autor da punição possua essa permissão, tanto na forma por comando quanto na seleção via GUI, e SHALL ocultar da GUI os motivos para os quais o autor não tem permissão.

#### Scenario: Motivo oculto na GUI

- **GIVEN** um motivo de `BAN` que exige a permissão `spunish.reason.ban.hacking`
- **AND** um membro da staff que não possui essa permissão
- **WHEN** esse membro abre a GUI de motivos de `BAN`
- **THEN** o motivo não é exibido entre as opções disponíveis

#### Scenario: Motivo restrito recusado por comando

- **GIVEN** o mesmo motivo restrito e o mesmo membro da staff sem a permissão
- **WHEN** ele executa o comando de punição informando esse motivo diretamente
- **THEN** o sistema recusa a punição e responde com a mensagem configurável de permissão insuficiente para o motivo

### Requirement: Recarga do catálogo em runtime

O sistema SHALL oferecer um comando administrativo de recarga que reaplica a configuração de catálogo e mensagens sem reiniciar o servidor. A recarga SHALL ser atômica: se a nova configuração for inválida, o catálogo anterior permanece em vigor e o autor recebe a lista de erros.

#### Scenario: Recarga bem-sucedida

- **WHEN** um administrador executa o comando de recarga após editar o catálogo
- **THEN** os novos motivos passam a valer para as próximas GUIs e comandos, e o autor recebe confirmação

#### Scenario: Recarga com configuração inválida

- **GIVEN** uma edição que introduziu um id duplicado
- **WHEN** um administrador executa o comando de recarga
- **THEN** o catálogo em vigor permanece inalterado e o autor recebe a descrição dos erros encontrados

### Requirement: Punições existentes não são afetadas por mudanças no catálogo

Alterar ou remover um motivo do catálogo SHALL NOT alterar, invalidar ou reinterpretar punições já registradas.

#### Scenario: Motivo removido após punições emitidas

- **GIVEN** punições registradas com o motivo `griefing`
- **WHEN** o motivo `griefing` é removido da configuração e o catálogo é recarregado
- **THEN** as punições existentes continuam ativas, mantêm sua duração original e continuam exibindo o nome de exibição registrado no momento da aplicação
