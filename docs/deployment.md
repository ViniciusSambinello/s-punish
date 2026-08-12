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

O Velocity só faz leitura de ban no pré-login e consumo de eventos de sync — um
pool pequeno (2 a 4 conexões) é suficiente mesmo em redes grandes. Ver
`database.pool.velocity-*` em `config.yml` quando a seção 10 configurar isso.

## Modos de falha

Ver `docs/troubleshooting.md` (seção 12) para o comportamento de `DENY`/`ALLOW`
quando o armazenamento está indisponível.
