# --- Estágio 1: build ---
# Imagem com JDK + Maven, usada só pra compilar. Nunca vai pra produção.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copia só o pom.xml primeiro e baixa as dependências antes de copiar o
# código-fonte: o Docker cacheia essa camada, então trocar uma linha de
# código não força rebaixar todas as dependências de novo.
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests -o

# --- Estágio 2: imagem final ---
# Só JRE (não precisa de compilador pra RODAR um jar já pronto), base alpine
# pra ficar enxuta — mesma lógica de "imagem final mínima" do Postgres alpine
# do Módulo 2.
FROM eclipse-temurin:21-jre-alpine

# Não rodar como root é boa prática de produção: se a aplicação for
# comprometida, o processo dentro do container não tem privilégio de root.
RUN addgroup -S ledger && adduser -S ledger -G ledger
USER ledger

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
