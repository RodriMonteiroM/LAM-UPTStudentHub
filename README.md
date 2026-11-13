# UPT Student Hub

Projeto Android em Java e XML para Laboratório de Aplicações Móveis.

Aluno: Rodrigo Miguel, n.º 51856.

## Módulos

- Horário com sincronização assíncrona.
- Meteorologia do campus.
- Explorer: previsão marítima, detalhe, favoritos e comparação de três praias.

## Abrir o projeto

Abrir a pasta que contém `settings.gradle.kts` no Android Studio, usar o JDK integrado e sincronizar o Gradle. Executar a configuração `app` num emulador API 34 ou dispositivo compatível.

Configuração: minSdk 24, targetSdk 34, compileSdk 37 e Java 11. O Android Studio configura o caminho local do SDK em `local.properties`.

## Estrutura

As classes em `app/src/main/java/pt/upt/studenthub` estão organizadas por responsabilidade: activities, viewmodels, models, network, parsers, adapters e utils.

Os testes instrumentados estão em `app/src/androidTest`. Compilar com `gradlew assembleDebug` e executar a verificação estática com `gradlew lintDebug`.
