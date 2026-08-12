## Purpose

Garante que todo texto que o sistema mostra a jogadores e à staff venha de arquivos de configuração, com placeholders e formatação previsíveis, para que a operação ajuste a comunicação sem tocar em código.

## ADDED Requirements

### Requirement: Todas as mensagens voltadas ao usuário são configuráveis

Todo texto exibido a um jogador ou a um membro da staff SHALL ter origem em um arquivo de mensagens editável. Isso inclui: confirmações, erros de validação, avisos de permissão, telas de rejeição de login, avisos de mute, anúncios de punição e revogação, títulos de GUI, nomes e descrições de itens de GUI, rótulos de estado e rótulos de janela de relatório.

Mensagens de log destinadas ao operador do servidor SHALL NOT ser configuráveis.

#### Scenario: Texto alterado sem recompilar

- **WHEN** o operador edita o texto de confirmação de punição e recarrega
- **THEN** a nova redação passa a ser usada nas punições seguintes

#### Scenario: Título de GUI configurável

- **WHEN** o operador altera o título da GUI de motivos
- **THEN** a GUI passa a abrir com o novo título após a recarga

### Requirement: Placeholders documentados por mensagem

Cada mensagem SHALL declarar quais placeholders aceita, e o sistema SHALL substituí-los pelos valores correspondentes. Um placeholder desconhecido SHALL ser deixado literal no texto e registrado no log uma única vez por chave, sem interromper o envio da mensagem.

Os placeholders comuns SHALL incluir, no mínimo: alvo, autor, categoria, id do motivo, nome de exibição do motivo, duração original, duração restante, instante de aplicação, instante de expiração, identificador da punição e servidor de origem.

#### Scenario: Substituição

- **GIVEN** uma mensagem contendo os placeholders de alvo e de nome de exibição do motivo
- **WHEN** a mensagem é enviada após uma punição
- **THEN** os placeholders são substituídos pelos valores reais daquela punição

#### Scenario: Placeholder desconhecido

- **GIVEN** uma mensagem editada com um placeholder que o sistema não fornece
- **WHEN** a mensagem é enviada
- **THEN** o texto é entregue com o placeholder literal, e o log registra o aviso uma única vez para aquela chave

#### Scenario: Placeholder de tempo restante em punição permanente

- **GIVEN** uma mensagem que usa o placeholder de duração restante
- **WHEN** ela é enviada para uma punição permanente
- **THEN** o placeholder é substituído pelo texto configurável de permanente

### Requirement: Formatação rica com MiniMessage

As mensagens SHALL ser interpretadas como MiniMessage, suportando cores, gradientes, hover e click events. Uma tag malformada SHALL NOT derrubar o envio: o sistema SHALL entregar o texto sem formatação e registrar o erro no log com a chave da mensagem.

#### Scenario: Formatação aplicada

- **GIVEN** uma mensagem com tags de cor MiniMessage
- **WHEN** ela é enviada a um jogador
- **THEN** o texto aparece com a formatação declarada

#### Scenario: Tag malformada

- **GIVEN** uma mensagem com uma tag MiniMessage não fechada
- **WHEN** ela é enviada
- **THEN** o jogador recebe o texto sem formatação e o log identifica a chave da mensagem defeituosa

### Requirement: Mensagens multilinha e desligáveis

Uma mensagem SHALL poder ser declarada como lista de linhas, sendo enviada como várias linhas na ordem declarada. Uma mensagem definida como texto vazio ou lista vazia SHALL NOT ser enviada.

#### Scenario: Mensagem de múltiplas linhas

- **GIVEN** uma tela de rejeição declarada como lista de quatro linhas
- **WHEN** o jogador é rejeitado
- **THEN** as quatro linhas aparecem na ordem declarada

#### Scenario: Mensagem desligada

- **GIVEN** o anúncio público definido como lista vazia
- **WHEN** uma punição é aplicada
- **THEN** nenhum anúncio público é enviado

### Requirement: Prefixo global reutilizável

O sistema SHALL suportar um prefixo global configurável, aplicável por meio de um placeholder disponível em qualquer mensagem, para que a identidade visual seja alterada em um único lugar.

#### Scenario: Prefixo alterado uma vez

- **GIVEN** várias mensagens que usam o placeholder de prefixo
- **WHEN** o operador altera o prefixo global e recarrega
- **THEN** todas essas mensagens passam a exibir o novo prefixo

### Requirement: Formatação configurável de duração e de data

O sistema SHALL usar um formato configurável para durações, com rótulos de unidade traduzíveis, número máximo de unidades exibidas e o texto usado para punições permanentes. Datas e horas SHALL usar um padrão de formatação e um fuso horário configuráveis.

#### Scenario: Duração abreviada

- **GIVEN** o formato configurado para no máximo duas unidades
- **WHEN** um tempo restante de 2 dias, 3 horas e 15 minutos é exibido
- **THEN** o texto mostra apenas as duas unidades mais significativas

#### Scenario: Rótulos traduzidos

- **GIVEN** rótulos de unidade configurados em português
- **WHEN** uma duração é exibida
- **THEN** as unidades aparecem com os rótulos configurados

#### Scenario: Data no fuso configurado

- **GIVEN** o fuso configurado como `America/Sao_Paulo`
- **WHEN** o instante de aplicação de uma punição é exibido
- **THEN** ele aparece convertido para esse fuso, mesmo tendo sido armazenado em UTC

### Requirement: Chaves ausentes não quebram o sistema

Quando uma chave de mensagem estiver ausente do arquivo — por exemplo, após uma atualização que introduziu novas chaves — o sistema SHALL usar o valor padrão embutido, registrar o aviso no log uma única vez por chave, e continuar operando normalmente.

#### Scenario: Chave nova ausente após atualização

- **GIVEN** um arquivo de mensagens de uma versão anterior
- **WHEN** o plugin de uma versão mais nova é iniciado
- **THEN** as chaves ausentes usam os padrões embutidos e o log lista quais chaves faltaram

#### Scenario: Arquivo de mensagens ausente

- **WHEN** o arquivo de mensagens não existe
- **THEN** o sistema o cria com os valores padrão e prossegue

### Requirement: Recarga de mensagens em runtime

O comando administrativo de recarga SHALL reaplicar o arquivo de mensagens junto com o catálogo. Se o arquivo estiver sintaticamente inválido, as mensagens em vigor SHALL permanecer inalteradas e o autor SHALL receber o erro.

#### Scenario: Recarga com YAML inválido

- **GIVEN** uma edição que quebrou a sintaxe do arquivo
- **WHEN** o administrador executa a recarga
- **THEN** as mensagens anteriores continuam em uso e o autor recebe a descrição do erro de sintaxe
