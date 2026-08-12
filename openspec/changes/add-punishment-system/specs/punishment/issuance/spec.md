## Purpose

Define como um membro da staff aplica uma punição a um jogador, cobrindo as três formas do comando `/punish`, as GUIs de categoria e de motivo, o tab complete, a interpretação de tempo e as regras que impedem punições inválidas.

## ADDED Requirements

### Requirement: Comando de punição com três formas de invocação

O sistema SHALL expor o comando `/punish` aceitando exatamente três formas:

1. `/punish <player>` — abre a GUI de categorias;
2. `/punish <player> <category>` — abre diretamente a GUI de motivos da categoria informada;
3. `/punish <player> <category> <reason> <time>` — aplica a punição imediatamente, sem GUI.

Qualquer outra aridade SHALL ser rejeitada com a mensagem configurável de uso do comando.

#### Scenario: Forma sem categoria abre a GUI de categorias

- **WHEN** um membro da staff com permissão executa `/punish Steve`
- **THEN** a GUI de categorias é aberta para ele, contendo `BAN` e `MUTE`

#### Scenario: Forma com categoria abre a GUI de motivos

- **WHEN** um membro da staff executa `/punish Steve ban`
- **THEN** a GUI de motivos de `BAN` é aberta, sem passar pela GUI de categorias

#### Scenario: Forma completa aplica direto

- **WHEN** um membro da staff executa `/punish Steve ban hacking 30d`
- **THEN** o jogador é punido imediatamente com o motivo `hacking` e duração de 30 dias, sem nenhuma GUI ser aberta

#### Scenario: Aridade inválida

- **WHEN** um membro da staff executa `/punish Steve ban hacking`
- **THEN** o sistema rejeita o comando e responde com a mensagem configurável de uso

### Requirement: Comando restrito ao console e à staff autorizada

Executar `/punish` SHALL exigir a permissão `spunish.punish`, e aplicar uma punição de uma categoria específica SHALL exigir adicionalmente `spunish.punish.<category>` em minúsculas. O console SHALL poder usar somente a forma completa, já que não pode abrir GUIs.

#### Scenario: Sem permissão base

- **WHEN** um jogador sem `spunish.punish` executa qualquer forma de `/punish`
- **THEN** o comando é recusado com a mensagem configurável de permissão insuficiente e nenhuma GUI é aberta

#### Scenario: Permissão de categoria ausente

- **GIVEN** um membro da staff com `spunish.punish` e `spunish.punish.mute`, mas sem `spunish.punish.ban`
- **WHEN** ele executa `/punish Steve ban hacking 30d`
- **THEN** a punição é recusada com a mensagem configurável de permissão insuficiente

#### Scenario: Categoria sem permissão não aparece na GUI

- **GIVEN** o mesmo membro da staff sem `spunish.punish.ban`
- **WHEN** ele executa `/punish Steve`
- **THEN** a GUI de categorias exibe apenas `MUTE`

#### Scenario: Console usando forma de GUI

- **WHEN** o console executa `/punish Steve` ou `/punish Steve ban`
- **THEN** o sistema responde que essa forma exige um jogador e instrui a usar a forma completa

### Requirement: Tab complete contextual

O sistema SHALL fornecer sugestões de tab complete dependentes da posição do argumento:

- posição 1 (`<player>`): nomes de jogadores online visíveis ao autor;
- posição 2 (`<category>`): `ban` e `mute`, filtradas pelas permissões de categoria do autor;
- posição 3 (`<reason>`): ids de motivo da categoria já informada na posição 2, filtrados pelas permissões de motivo do autor;
- posição 4 (`<time>`): a duração padrão do motivo informado, mais um conjunto configurável de durações sugeridas.

As sugestões SHALL ser filtradas pelo prefixo já digitado, de forma case-insensitive.

#### Scenario: Sugestão de categorias

- **WHEN** o autor digita `/punish Steve ` e aciona o tab complete
- **THEN** as sugestões são `ban` e `mute`, restritas às categorias que ele tem permissão de aplicar

#### Scenario: Sugestão de motivos depende da categoria digitada

- **WHEN** o autor digita `/punish Steve mute ` e aciona o tab complete
- **THEN** as sugestões são os ids de motivo configurados para `MUTE`, e nenhum motivo de `BAN` aparece

#### Scenario: Filtro por prefixo

- **GIVEN** motivos de `BAN` com ids `hacking`, `hitbox` e `griefing`
- **WHEN** o autor digita `/punish Steve ban h` e aciona o tab complete
- **THEN** as sugestões são `hacking` e `hitbox`

#### Scenario: Categoria inválida na posição anterior

- **WHEN** o autor digita `/punish Steve xyz ` e aciona o tab complete
- **THEN** nenhuma sugestão de motivo é retornada

### Requirement: GUI de seleção de categoria

A GUI de categorias SHALL exibir um item por categoria que o autor pode aplicar. Selecionar um item SHALL abrir a GUI de motivos correspondente, preservando o jogador alvo. Título, itens, ícones e textos SHALL ser configuráveis.

#### Scenario: Seleção encaminha para motivos

- **WHEN** o autor clica no item de `BAN` na GUI de categorias
- **THEN** a GUI de motivos de `BAN` é aberta para o mesmo jogador alvo

#### Scenario: Fechar sem selecionar

- **WHEN** o autor fecha a GUI de categorias sem clicar em nenhum item
- **THEN** nenhuma punição é aplicada e nenhum registro é criado

### Requirement: GUI de seleção de motivo

A GUI de motivos SHALL exibir um item por motivo configurado da categoria, visível ao autor, mostrando o nome de exibição e a duração do motivo. Clicar em um motivo SHALL aplicar a punição usando a duração padrão daquele motivo e fechar a GUI.

A GUI SHALL paginar quando os motivos visíveis excederem a capacidade de uma página, e SHALL oferecer um controle de retorno à GUI de categorias quando tiver sido aberta a partir dela.

#### Scenario: Clique aplica com a duração padrão

- **GIVEN** um motivo `spam` de `MUTE` com duração `1h`
- **WHEN** o autor clica nesse motivo na GUI
- **THEN** o jogador alvo é mutado por 1 hora, a GUI é fechada e o autor recebe a mensagem configurável de confirmação

#### Scenario: Paginação

- **GIVEN** uma categoria com mais motivos visíveis do que cabem em uma página
- **WHEN** o autor abre a GUI de motivos
- **THEN** controles de página anterior e próxima são exibidos e navegam entre os motivos sem perder o jogador alvo

#### Scenario: Retorno à GUI de categorias

- **GIVEN** que a GUI de motivos foi aberta a partir da GUI de categorias
- **WHEN** o autor aciona o controle de retorno
- **THEN** a GUI de categorias é reaberta para o mesmo jogador alvo

#### Scenario: GUI aberta diretamente não oferece retorno

- **GIVEN** que a GUI de motivos foi aberta por `/punish Steve ban`
- **WHEN** a GUI é exibida
- **THEN** nenhum controle de retorno à GUI de categorias é exibido

### Requirement: Interpretação do argumento de tempo

Na forma completa do comando, o argumento `<time>` SHALL aceitar:

- o valor literal `n`, que significa permanente;
- uma sequência de um ou mais pares quantidade+unidade sem espaços, com as unidades `s`, `m`, `h`, `d`, `w`, `mo`, `y` (por exemplo `30d`, `1h30m`, `1y6mo`).

Um valor de tempo malformado, negativo ou zero SHALL ser rejeitado com a mensagem configurável de tempo inválido, e nenhuma punição SHALL ser criada.

#### Scenario: Tempo permanente explícito

- **WHEN** o autor executa `/punish Steve ban hacking n`
- **THEN** a punição é registrada como permanente, sem instante de expiração

#### Scenario: Tempo composto

- **WHEN** o autor executa `/punish Steve mute spam 1h30m`
- **THEN** a punição expira 90 minutos após o instante de aplicação

#### Scenario: Tempo malformado

- **WHEN** o autor executa `/punish Steve ban hacking 30x`
- **THEN** o comando é recusado com a mensagem configurável de tempo inválido e nenhuma punição é criada

#### Scenario: Tempo zero

- **WHEN** o autor executa `/punish Steve mute spam 0s`
- **THEN** o comando é recusado com a mensagem configurável de tempo inválido

### Requirement: Duração máxima por permissão

O sistema SHALL suportar um limite configurável de duração por permission node. Quando o autor solicitar, por comando, uma duração maior do que o seu limite — incluindo permanente — a punição SHALL ser recusada.

#### Scenario: Duração acima do limite

- **GIVEN** um autor cujo limite configurado é `7d`
- **WHEN** ele executa `/punish Steve ban hacking 30d`
- **THEN** a punição é recusada com a mensagem configurável de duração acima do limite, informando o limite aplicável

#### Scenario: Permanente acima do limite

- **GIVEN** o mesmo autor com limite `7d`
- **WHEN** ele executa `/punish Steve ban hacking n`
- **THEN** a punição é recusada com a mensagem configurável de duração acima do limite

#### Scenario: Autor sem limite configurado

- **GIVEN** um autor que não corresponde a nenhum limite configurado
- **WHEN** ele aplica qualquer duração
- **THEN** nenhuma restrição de duração é aplicada

### Requirement: Resolução do jogador alvo

O sistema SHALL aceitar como alvo tanto jogadores online quanto jogadores offline previamente conhecidos pela rede. A resolução SHALL usar o nome informado e SHALL retornar o UUID e o último nome conhecido do jogador.

Quando o nome não corresponder a nenhum jogador conhecido, o comando SHALL ser recusado com a mensagem configurável de jogador não encontrado.

#### Scenario: Alvo offline conhecido

- **GIVEN** um jogador que já entrou na rede anteriormente e está offline
- **WHEN** um membro da staff o pune pelo nome
- **THEN** a punição é registrada contra o UUID correto e passa a valer no próximo login dele

#### Scenario: Alvo desconhecido

- **WHEN** um membro da staff pune um nome que nunca entrou na rede
- **THEN** o comando é recusado com a mensagem configurável de jogador não encontrado e nenhum registro é criado

#### Scenario: Punição registra o nome no momento da aplicação

- **WHEN** uma punição é aplicada
- **THEN** o registro guarda o UUID do alvo e o nome que ele usava naquele instante

### Requirement: Regras de precedência e recusa

O sistema SHALL recusar a punição, sem criar registro, quando:

- o autor for o próprio alvo, salvo se possuir `spunish.punish.self`;
- o alvo possuir `spunish.exempt.<category>` e o autor não possuir `spunish.punish.override`;
- o alvo já possuir uma punição ativa da mesma categoria e o autor não possuir `spunish.punish.override`.

Quando o autor possuir `spunish.punish.override` e o alvo já tiver punição ativa da mesma categoria, a punição anterior SHALL ser revogada automaticamente com autoria do sistema e a nova SHALL ser aplicada, ambas permanecendo visíveis no histórico.

#### Scenario: Autopunição

- **WHEN** um membro da staff sem `spunish.punish.self` tenta punir a si mesmo
- **THEN** o comando é recusado com a mensagem configurável correspondente

#### Scenario: Alvo isento

- **GIVEN** um alvo com `spunish.exempt.ban`
- **WHEN** um autor sem `spunish.punish.override` tenta bani-lo
- **THEN** o comando é recusado com a mensagem configurável de alvo protegido

#### Scenario: Punição duplicada recusada

- **GIVEN** um alvo com um mute ativo
- **WHEN** um autor sem `spunish.punish.override` tenta mutá-lo novamente
- **THEN** o comando é recusado com a mensagem configurável de punição já ativa, informando o tempo restante

#### Scenario: Sobreposição autorizada

- **GIVEN** um alvo com um mute ativo de 1 hora
- **WHEN** um autor com `spunish.punish.override` o muta por 7 dias
- **THEN** o mute anterior é marcado como revogado pelo sistema, o novo mute de 7 dias fica ativo, e o histórico do jogador exibe os dois registros

#### Scenario: Categorias diferentes coexistem

- **GIVEN** um alvo com um mute ativo
- **WHEN** um autor sem override o bane
- **THEN** o ban é aplicado normalmente e ambas as punições ficam ativas simultaneamente

### Requirement: Registro e notificação da punição aplicada

Ao aplicar uma punição, o sistema SHALL persistir um registro contendo, no mínimo: categoria, UUID e nome do alvo, identidade do autor (UUID e nome, ou marcador de console), id do motivo, nome de exibição do motivo, instante de aplicação, instante de expiração ou marcador de permanente, e o servidor de origem.

O sistema SHALL notificar o autor com uma mensagem de confirmação e SHALL emitir um anúncio para os detentores da permissão `spunish.notify`. Anúncio público ao servidor SHALL ser configurável e desligável.

#### Scenario: Confirmação ao autor

- **WHEN** uma punição é aplicada com sucesso
- **THEN** o autor recebe a mensagem configurável de confirmação contendo alvo, categoria, motivo e duração

#### Scenario: Notificação à staff

- **WHEN** uma punição é aplicada com sucesso
- **THEN** todos os jogadores online com `spunish.notify`, em qualquer servidor da rede, recebem o anúncio configurável de staff

#### Scenario: Anúncio público desligado

- **GIVEN** que o anúncio público está desabilitado na configuração
- **WHEN** uma punição é aplicada
- **THEN** nenhuma mensagem é enviada aos jogadores sem `spunish.notify`

#### Scenario: Falha de persistência não aplica a punição

- **WHEN** a persistência do registro falha
- **THEN** a punição não é aplicada, o alvo não é kickado nem mutado, o autor recebe a mensagem configurável de erro interno, e a falha é registrada no log com o erro subjacente

### Requirement: Comando nunca bloqueia a thread principal

Nenhuma operação de resolução de jogador, leitura ou escrita de punição SHALL ser executada na thread principal do servidor. A abertura de GUIs e o envio de mensagens SHALL ocorrer na thread principal após a conclusão do trabalho assíncrono.

#### Scenario: Banco lento não trava o servidor

- **GIVEN** um banco de dados que responde em 2 segundos
- **WHEN** um membro da staff aplica uma punição
- **THEN** o servidor continua processando ticks normalmente durante a operação e a confirmação chega ao autor quando a escrita conclui
