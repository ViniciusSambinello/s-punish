# Deployment

## Coordenadas de runtime (verificadas em 2026-08-11)

A premissa de `design.md` — que "Paper 26.2" é a API Paper para a versão 26.2 do
Minecraft — foi confirmada. Nenhum rebaixamento de toolchain foi necessário.

| Item | Valor |
| --- | --- |
| Paper API | `io.papermc.paper:paper-api:26.2.build.112-stable` |
| Velocity API | `com.velocitypowered:velocity-api:4.0.0` |
| Repositório dos dois artefatos | `https://repo.papermc.io/repository/maven-public/` |
| Java exigido pelo Paper 26.2 | 25 |
| Java exigido pelo Velocity (setup atual) | 25 |
| Build tool | Gradle 9.7.0 (suporta daemon em Java 25 a partir da linha 9.1, mas builds anteriores a 9.7 reportaram falso-negativo de compatibilidade com Java 25 sob o plugin Kotlin JVM — fora do caminho deste projeto, que não compila Kotlin) |

Toolchain do projeto fixado em Java 25 em `build.gradle.kts` (convenção raiz), sem
plano de rebaixamento: ambos os runtimes o suportam nativamente.

`velocity-api` está pareado na versão estável `4.0.0` em vez do snapshot
`4.1.0-SNAPSHOT` em desenvolvimento — a documentação oficial de compatibilidade
do Velocity (`docs.papermc.io/velocity/server-compatibility`) confirma suporte
até a versão 26.2 do Minecraft já na linha estável, e uma dependência de build
não deve apontar para um SNAPSHOT.

## Ordem de implantação

1. Provisionar o MySQL 8.0+ e o usuário da aplicação; conferir alcance a partir
   do proxy e de todos os backends.
2. Subir **um** backend com o plugin. As migrações criam o schema na primeira
   inicialização.
3. Validar aplicação, expiração, histórico e relatório nesse backend isolado.
4. Distribuir aos demais backends com o mesmo `config.yml`, alterando apenas o
   identificador de servidor (`server.id`).
5. Instalar o módulo Velocity por último — os backends já bloqueiam login
   sozinhos, então a borda é reforço, não pré-requisito.
6. Negar os comandos vanilla de punição (`minecraft.command.ban`,
   `minecraft.command.pardon`, `minecraft.command.ban-ip`, etc.) à staff.

## Dimensionamento do pool no proxy

O Velocity só faz leitura de ban no login e consumo de eventos de sync — um
pool pequeno é suficiente mesmo em redes grandes. O `config.yml` que o módulo
Velocity cria no primeiro boot (`spunish-velocity`'s bundled default, distinto
do `config.yml` de cada backend) já vem com `maximum-pool-size: 4` e
`minimum-idle: 1`; não aumente esses valores a menos que o proxy sirva um
número muito grande de backends simultaneamente.

## Verificação manual da borda (proxy ausente)

Requer infraestrutura real (um Paper com o plugin, MySQL, e opcionalmente o
Velocity) e por isso não foi executada neste ambiente — apenas documentada
aqui para quem for validar um deployment real (ver também a seção 11.9 do
`tasks.md`, que cobre a mesma matriz de teste manual de forma mais ampla):

1. Aplique um ban de teste diretamente no backend (`/punish <player> ban <reason> <time>`).
2. Sem o módulo Velocity instalado em nenhum proxy, conecte-se diretamente à
   porta do backend (bypassando qualquer proxy) com o jogador banido.
3. Confirme que o login é recusado com a tela de ban configurável — ou seja,
   que o enforcement do backend por si só já é suficiente, e o módulo
   Velocity é reforço de borda, não pré-requisito (consistente com a ordem
   de implantação acima, que instala o Velocity por último).

## Modos de falha

Ver `docs/troubleshooting.md` (seção 12) para o comportamento de `DENY`/`ALLOW`
quando o armazenamento está indisponível.
