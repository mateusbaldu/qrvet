# qrvet
Projeto de Trabalho de Graduação da Fatec Ipiranga.

## Executar a aplicação

Copie `.env.example` para `.env`, ajuste as senhas do banco e execute:

```bash
docker compose up --build
```

- QRVet: `http://localhost:3000`
- Mailpit: `http://localhost:8025`
- API: `http://localhost:3000/api`

Com os dados demonstrativos habilitados, use `sarah@qrvet.clinic` e a senha
`Qrvet@123`. Convites e recuperações de senha aparecem no Mailpit e não são
enviados para endereços reais.

Os fluxos integrados atualmente são login e sessão, recuperação de senha,
aceite de convite, equipe e perfil. Consulte também o `backend/README.md`.

Membros do grupo:

- Mateus Balduino da Silva
- Anthony Akio Neves
- Vitor Vieira da Hora
- Kauan Torres
- Washington Luis
