#!/bin/bash

# URL do endpoint de erro
URL="http://localhost:8080/error"

# Intervalo entre as chamadas em segundos
INTERVAL=2

echo "Iniciando loop de chamadas para $URL a cada $INTERVAL segundos."
echo "Pressione [Ctrl+C] para parar."

# Loop infinito
while true
do
  # Usa curl para fazer a requisição
  # -s para modo silencioso (não mostra barra de progresso)
  # -o /dev/null para descartar o corpo da resposta
  # -w para escrever informações de status
  curl -s -o /dev/null -w "Status: %{http_code} | Hora: $(date +'%T')\n" "$URL"
  
  # Espera pelo intervalo definido
  sleep $INTERVAL
done
