# Makefile para o projeto RestauranteSDI

# --- Variáveis de Configuração ---

# Compilador Java
# JCC = /usr/local/jdk1.8.0_131/bin/javac
# JVM = /usr/local/jdk1.8.0_131/bin/java

JCC = javac
JVM = java

# Diretório de código-fonte
SRC_DIR = src

# Diretório de saída para os arquivos .class
BIN_DIR = bin

# Flags do compilador (especifica o diretório de saída)
JFLAGS = -d $(BIN_DIR)

# Classpath para execução (aponta para o diretório bin)
CP = -cp $(BIN_DIR)

# Encontra todos os arquivos .java no diretório src e suas subpastas
SOURCES = $(shell find $(SRC_DIR) -name '*.java')

# --- Alvos Principais ---

# Alvo padrão: executado quando você digita apenas "make"
all: compile

# Alvo para compilar todo o código-fonte
compile:
	@echo "-------------------------------------"
	@echo "Compilando o projeto..."
	@mkdir -p $(BIN_DIR)
	@$(JCC) $(JFLAGS) $(SOURCES)
	@echo "Compilação concluída com sucesso!"
	@echo "-------------------------------------"

# Alvo para limpar os arquivos compilados
clean:
	@echo "-------------------------------------"
	@echo "Limpando arquivos compilados..."
	@rm -rf $(BIN_DIR)/*
	@echo "Limpeza concluída."
	@echo "-------------------------------------"


# --- Alvos para Iniciar os RMI Registries ---

# Alvo para iniciar o RMI Registry da Cozinha na porta 1099
# Requer um terminal separado
run_rmi_cozinha:
	@echo "Iniciando RMI Registry para a Cozinha na porta 1099..."
	@echo "ATENÇÃO: Este processo ocupará este terminal."
	@rmiregistry -J-cp -J$(BIN_DIR) 1096

# Alvo para iniciar o RMI Registry do Restaurante na porta 1100
# Requer um terminal separado
run_rmi_restaurante:
	@echo "Iniciando RMI Registry para o Restaurante na porta 1100..."
	@echo "ATENÇÃO: Este processo ocupará este terminal."
	@rmiregistry -J-cp -J$(BIN_DIR) 1110


# --- Alvos para Executar os Servidores e o Cliente ---

# Alvo para iniciar o Servidor da Cozinha
# Requer um terminal separado
run_cozinha: compile
	@echo "Iniciando o Servidor da Cozinha..."
	@echo "URL: rmi://localhost:1099/CozinhaService"
	@$(JVM) $(CP) restauranteSDI.server.CozinhaServer

# Alvo para iniciar o Servidor do Restaurante
# Requer um terminal separado
run_restaurante: compile
	@echo "Iniciando o Servidor do Restaurante..."
	@echo "URL: rmi://localhost:1100/RestauranteService"
	@$(JVM) $(CP) -Djava.security.policy=config/all.policy restauranteSDI.server.RestauranteServer

# Alvo para iniciar o Servidor/Publisher do Mercado (SOAP)
# Requer um terminal separado
run_mercado: compile
	@echo "Iniciando o Web Service do Mercado..."
	@echo "URL: http://localhost:9999/ws/mercado"
	@$(JVM) $(CP) restauranteSDI.server.MercadoPublisher

# Alvo para executar o Cliente da Mesa
# Requer um terminal separado
run_cliente: compile
	@echo "Iniciando o Cliente da Mesa..."
	@$(JVM) $(CP) restauranteSDI.client.MesaCliente


# --- MERCADO DISTRIBUÍDO (RAFT) ---
# Todas as filiais são configuradas com a porta SOAP 9000.
# Como apenas o LÍDER publica o serviço, não haverá conflito de porta.

run_filial1: compile
	@echo "Filial 1..."
	@$(JVM) $(CP) restauranteSDI.server.MercadoPublisher 1 5001 9000

run_filial2: compile
	@echo "Filial 2..."
	@$(JVM) $(CP) restauranteSDI.server.MercadoPublisher 2 5002 9000

run_filial3: compile
	@echo "Filial 3..."
	@$(JVM) $(CP) restauranteSDI.server.MercadoPublisher 3 5003 9000

run_filial4: compile
	@echo "Filial 4..."
	@$(JVM) $(CP) restauranteSDI.server.MercadoPublisher 4 5004 9000

run_filial5: compile
	@echo "Filial 5..."
	@$(JVM) $(CP) restauranteSDI.server.MercadoPublisher 5 5005 9000

# --- Alvo de Ajuda ---

help:
	@echo "------------------------------------------------------------"
	@echo "Comandos disponíveis para o projeto RestauranteSDI:"
	@echo "  make all              - Compila o projeto (padrão)."
	@echo "  make compile          - Força a recompilação de todos os arquivos."
	@echo "  make clean            - Remove todos os arquivos compilados (.class)."
	@echo ""
	@echo "  --- INFRAESTRUTURA RMI (Terminais 1 e 2) ---"
	@echo "  1. make run_rmi_cozinha"
	@echo "  2. make run_rmi_restaurante"
	@echo ""
	@echo "  --- SERVIDORES (Terminais 3 e 4) ---"
	@echo "  3. make run_cozinha"
	@echo "  4. make run_restaurante"
	@echo ""
	@echo "  --- MERCADO DISTRIBUÍDO (Terminais 5 a 9) ---"
	@echo "  5. make run_filial1    (Porta P2P: 5001)"
	@echo "  6. make run_filial2    (Porta P2P: 5002)"
	@echo "  7. make run_filial3    (Porta P2P: 5003)"
	@echo "  8. make run_filial4    (Porta P2P: 5004)"
	@echo "  9. make run_filial5    (Porta P2P: 5005 - WebService: 9000)"
	@echo ""
	@echo "  --- CLIENTE (Terminal 10) ---"
	@echo "  10. make run_cliente"
	@echo "------------------------------------------------------------"