# qrvet
Projeto de Trabalho de Graduação da Fatec Ipiranga.

Membros do grupo:
- Mateus Balduino da Silva
- Anthony Akio Neves
- Vitor Vieira da Hora
- Kauan Torres
- Washington Luis

## Execução com Docker

Os Dockerfiles estão em `backend/Dockerfile` e `frontend/Dockerfile`. O
`docker-compose.yml` contém toda a configuração necessária, inclusive as
variáveis de execução, portanto não é necessário criar um arquivo `.env`.

Para publicar as imagens no Docker Hub, após instalar e iniciar o Docker
Desktop, execute na raiz do projeto:

```powershell
docker login
docker compose build
docker compose push
```

Para executar em outra máquina (por exemplo, a do professor), copie apenas o
arquivo `docker-compose.yml` e execute:

```powershell
docker compose pull
docker compose up -d
```

A aplicação fica disponível em `http://localhost:3000`. Para acompanhar a
inicialização, use `docker compose logs -f`.

> Atenção: o Compose inclui credenciais por solicitação do projeto. Não o
> publique em repositório público nem reutilize essas credenciais em produção.
