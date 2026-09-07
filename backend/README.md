# QRVet backend

Back-end das telas de autenticação, recuperação de senha, equipe e perfil.

## Execução pelo Docker

Na raiz do projeto:

```bash
docker compose up --build
```

- Aplicação: `http://localhost:3000`
- API: `http://localhost:3000/api`
- Mailpit: `http://localhost:8025`
- MySQL: `localhost:3306`

Com `APP_SEED_DEMO=true`, o login administrativo de demonstração é:

```text
E-mail: sarah@qrvet.clinic
Senha: Qrvet@123
```

## Autenticação

O login devolve um access token JWT com 15 minutos de validade e cria o cookie
HttpOnly `qrvet_refresh`. Envie o access token nas rotas protegidas:

```http
Authorization: Bearer <accessToken>
```

O refresh token é rotacionado em `POST /api/auth/refresh`. O front deve fazer as
requisições de login, refresh e logout com credenciais habilitadas.

## Rotas implementadas

```text
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/logout
GET    /api/auth/me
POST   /api/auth/forgot-password
POST   /api/auth/reset-password
POST   /api/auth/accept-invitation

GET    /api/team/members
POST   /api/team/invitations
POST   /api/team/invitations/{id}/resend
DELETE /api/team/invitations/{id}
PATCH  /api/team/members/{id}/role
DELETE /api/team/members/{id}

GET    /api/users/me
PATCH  /api/users/me
PATCH  /api/users/me/password
POST   /api/users/me/avatar
DELETE /api/users/me/avatar
POST   /api/users/me/heartbeat
GET    /api/users/{id}/avatar
```

Para testar e-mail, faça uma recuperação de senha ou crie um convite e abra o
Mailpit. Nenhum e-mail é enviado para a internet.

## Presença

O front envia um heartbeat a cada dois minutos enquanto a aba está visível e em
foco. O membro aparece como online se o último heartbeat ocorreu nos últimos
cinco minutos (`PRESENCE_ONLINE_SECONDS`). Contas removidas são desativadas e
deixam de aparecer na equipe; portanto, “Inativo” representa presença, não
bloqueio da conta.
